package com.app.pustakam.android.screen.notebookReader

import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.feature.notes.domain.usecase.ReadNoteUseCase
import com.app.pustakam.feature.notes.domain.usecase.UpdateReadingProgressUseCase
import com.app.pustakam.core.common.util.log_d
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.database.localdb.preferences.IAppPreferences
import com.app.pustakam.core.filesys.reader.DocumentExpansion
import com.app.pustakam.core.filesys.reader.EmbeddedDocumentSource
import com.app.pustakam.core.filesys.reader.ExpandedPages
import com.app.pustakam.core.filesys.reader.PageLayoutEngine
import com.app.pustakam.core.filesys.reader.PageLayoutPolicy
import com.app.pustakam.core.filesys.reader.ReaderBlock
import com.app.pustakam.core.filesys.reader.ReaderDocumentExpander
import com.app.pustakam.core.filesys.reader.ReaderPage

// 🔧 18-Jul-2026: one leaf of the book — every content type maps to at least one page
sealed class BookPage {
    abstract val sourceContentId: String?

    data class Cover(val title: String, val subtitle: String, override val sourceContentId: String? = null) : BookPage()
    data class TextPage(val text: String, val chunkIndex: Int, val chunkCount: Int, override val sourceContentId: String?) : BookPage()
    data class ImagePage(val path: String, val title: String, override val sourceContentId: String?) : BookPage()
    data class PdfSheet(val path: String, val pageIndex: Int, val pageCount: Int, val title: String, override val sourceContentId: String?) : BookPage()
    data class MediaPage(val media: NoteContentModel.MediaContent, override val sourceContentId: String?) : BookPage()
    data class DocFilePage(val media: NoteContentModel.MediaContent, override val sourceContentId: String?) : BookPage()
    data class LinkPage(val url: String, override val sourceContentId: String?) : BookPage()
    data class LocationPage(val latitude: Double, val longitude: Double, val address: String?, override val sourceContentId: String?) : BookPage()
}

data class BookUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val note: Note? = null,
    val pages: List<ReaderPage> = emptyList(),
    // 📖 15-Aug-2026: DISPLAY index the reader should sit at — moves only when a document opens or closes
    val startPageIndex: Int = 0,
    val readingMode: ReadingMode = ReadingMode.PAGE,
    val expandedDocumentIds: Set<String> = emptySet(),
    val busyDocumentIds: Set<String> = emptySet(),
    val unreadableDocumentIds: Set<String> = emptySet(),
)

class NoteBookReaderViewModel : BaseViewModel() {
    private val readNoteUseCase by inject<ReadNoteUseCase>()
    private val updateReadingProgressUseCase by inject<UpdateReadingProgressUseCase>()
    private val userPrefs by inject<IAppPreferences>()

    private val _uiState = MutableStateFlow(BookUiState())
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()
    var layoutPolicy: PageLayoutPolicy = PageLayoutPolicy.standard()
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            userPrefs.readingModeFlow.collect { raw ->
                _uiState.update { it.copy(readingMode = ReadingMode.from(raw)) }
            }
        }
    }

    fun toggleReadingMode() {
        val next = _uiState.value.readingMode.toggled()
        _uiState.update { it.copy(readingMode = next) }
        viewModelScope.launch(Dispatchers.IO) { userPrefs.setReadingMode(next.key) }
    }

    private companion object {
        private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    private var startContentId: String? = null
    private var singleContentMode: Boolean = false
    private var progressContentId: String? = null
    private var lastKnownPage: Int = 0
    private var totalPages: Int = 0

    // 📖 15-Aug-2026: the engine list never changes; documents are spliced on top of it for display
    private var basePages: List<ReaderPage> = emptyList()
    private val sources = mutableMapOf<String, EmbeddedDocumentSource>()
    private val expansions = mutableMapOf<String, Int>()
    private var expandedPages: ExpandedPages = ExpandedPages(emptyList())
    private var lastDisplayIndex: Int = 0

    fun load(
        noteId: String,
        startContentId: String? = null,
        singleContent: Boolean = false,
        pageWidth: Float = PageLayoutPolicy.A4_WIDTH,
        pageHeight: Float = PageLayoutPolicy.A4_HEIGHT,
    ) {
        layoutPolicy = PageLayoutPolicy.forScreen(pageWidth, pageHeight)
        this.startContentId = startContentId
        this.singleContentMode = singleContent && startContentId != null
        makeAWish(NOTES_CODES.READ) { readNoteUseCase.invoke(noteId) }
    }

    fun onPageChanged(index: Int) {
        lastDisplayIndex = index
        // a document sheet reports its card's page, so progress stays inside the engine list
        lastKnownPage = expandedPages.baseIndexOf(index)
        scheduleProgressSave()
    }

    /** Open the first window of a document under its card, or close it again. */
    fun toggleDocument(media: NoteContentModel.MediaContent) {
        val contentId = media.id
        log_d("InlineDoc", "toggle $contentId type=${media.type} path=${media.localPath} open=${expansions[contentId]}")
        if (expansions.containsKey(contentId)) {
            expansions.remove(contentId)
            publish(anchorOn = contentId)
            return
        }
        val known = sources[contentId]
        if (known != null) {
            if (!known.isReadable) return
            expansions[contentId] = ReaderDocumentExpander.firstWindow(known.pageCount)
            publish(anchorOn = contentId)
            return
        }
        _uiState.update { it.copy(busyDocumentIds = it.busyDocumentIds + contentId) }
        viewModelScope.launch {
            // counting sheets touches the file, so it happens once per document and off the main thread
            val probed = withContext(Dispatchers.IO) { EmbeddedDocumentProbe.probe(media) }
            sources[contentId] = probed
            if (probed.isReadable) {
                expansions[contentId] = ReaderDocumentExpander.firstWindow(probed.pageCount)
            }
            publish(anchorOn = contentId, releaseBusy = contentId)
        }
    }

    fun loadMoreDocumentPages(contentId: String) {
        val source = sources[contentId] ?: return
        val loaded = expansions[contentId] ?: return
        val next = ReaderDocumentExpander.nextWindow(loaded, source.pageCount)
        if (next == loaded) return
        expansions[contentId] = next
        publish(anchorAt = lastDisplayIndex)
    }

    private fun publish(
        anchorOn: String? = null,
        anchorAt: Int? = null,
        releaseBusy: String? = null,
    ) {
        val rebuilt = ReaderDocumentExpander.expand(
            basePages,
            sources.values.toList(),
            expansions.map { DocumentExpansion(it.key, it.value) },
            layoutPolicy,
        )
        expandedPages = rebuilt
        log_d("InlineDoc", "publish base=${basePages.size} display=${rebuilt.size} expansions=$expansions")
        val anchor = when {
            anchorAt != null -> anchorAt.coerceIn(0, (rebuilt.size - 1).coerceAtLeast(0))
            // 📖 a page fills the screen, so opening must LAND on the first sheet — anchoring to the
            //   card leaves the document a whole screen below the fold and looks like nothing happened
            anchorOn != null -> firstSheetIndexOf(rebuilt.pages, anchorOn)
                .takeIf { it >= 0 } ?: cardIndexOf(rebuilt.pages, anchorOn)
            else -> rebuilt.displayIndexOf(lastKnownPage)
        }
        lastDisplayIndex = anchor
        _uiState.update {
            it.copy(
                pages = rebuilt.pages,
                startPageIndex = anchor,
                expandedDocumentIds = expansions.keys.toSet(),
                unreadableDocumentIds = sources.filterValues { source -> !source.isReadable }.keys.toSet(),
                busyDocumentIds = if (releaseBusy != null) it.busyDocumentIds - releaseBusy else it.busyDocumentIds,
            )
        }
    }

    /** Where this document's first open sheet sits, or -1 when it is closed. */
    private fun firstSheetIndexOf(pages: List<ReaderPage>, contentId: String): Int =
        pages.indexOfFirst { it.embedded?.contentId == contentId }

    private fun cardIndexOf(pages: List<ReaderPage>, contentId: String): Int = pages
        .indexOfFirst { page ->
            !page.isDocumentSheet && page.blocks.any { it is ReaderBlock.Document && it.item.id == contentId }
        }
        .coerceAtLeast(0)

    private var saveJob: Job? = null
    private fun scheduleProgressSave() {
        val contentId = progressContentId ?: return
        if (totalPages <= 0) return
        val page = lastKnownPage.coerceIn(0, totalPages - 1)
        saveJob?.cancel()
        // saveScope OUTLIVES this ViewModel so backing out doesn't drop the pending write
        saveJob = saveScope.launch {
            delay(350)
            updateReadingProgressUseCase(contentId, page, totalPages).collect { }
        }
    }

    override fun onCleared() {
        // final flush on teardown (back-out / process death) on the surviving scope
        saveJob?.cancel()
        val contentId = progressContentId
        if (contentId != null && totalPages > 0) {
            val page = lastKnownPage.coerceIn(0, totalPages - 1)
            saveScope.launch { updateReadingProgressUseCase(contentId, page, totalPages).collect { } }
        }
        super.onCleared()
    }

    override fun onLoading(taskCode: TaskCode) {
        _uiState.update { if (it.pages.isEmpty()) it.copy(isLoading = true) else it }
    }

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        val note = result.data.data as Note
        viewModelScope.launch(Dispatchers.IO) {
            val pages = PageLayoutEngine.buildPages(note, layoutPolicy)
            // an explicit startContentId (tapped card) is an intentional jump and wins over resume
            val jump = PageLayoutEngine.pageIndexOf(pages, startContentId)

            // progress is tracked against the first document on the page list, as before
            val progressContent = note.contents.filterIsInstance<NoteContentModel.MediaContent>()
                .firstOrNull { media -> pages.any { it.contains(media.id) } }
            progressContentId = progressContent?.id
            totalPages = pages.size

            val savedPage = progressContent
                ?.takeIf { it.hasReadingProgress() }
                ?.progressPage
                ?.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            val resolvedStart = if (jump >= 0) jump else savedPage ?: 0
            lastKnownPage = resolvedStart
            lastDisplayIndex = resolvedStart
            basePages = pages
            sources.clear()
            expansions.clear()
            expandedPages = ExpandedPages(pages)
            _uiState.update {
                it.copy(
                    isLoading = false, error = null, note = note, pages = pages,
                    startPageIndex = resolvedStart,
                    expandedDocumentIds = emptySet(), busyDocumentIds = emptySet(),
                    unreadableDocumentIds = emptySet(),
                )
            }
        }
    }

    override fun onFailure(taskCode: TaskCode, error: Error) {
        // 🔧 19-Jul-2026: FIX (first-load "No record found") — the read flow can emit a local-miss
        //   failure BEFORE the real data arrives. Never surface that flash: keep the loader when
        //   nothing is built yet, and ignore failures entirely once pages exist.
        log_d("BookReader", "read failed (suppressed for UX): $error")
    }


    override fun clearError() { _uiState.update { it.copy(error = null) } }
}
