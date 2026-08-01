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
import org.koin.core.component.inject
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.database.localdb.preferences.BasePreferences

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
    val pages: List<BookPage> = emptyList(),
    val startPageIndex: Int = 0,
    val readingMode: ReadingMode = ReadingMode.PAGE,
)

class NoteBookReaderViewModel : BaseViewModel() {
    private val readNoteUseCase by inject<ReadNoteUseCase>()
    // 📖 23-Jul-2026: progress persistence via its own use case (mirror of the delete-content flow)
    private val updateReadingProgressUseCase by inject<UpdateReadingProgressUseCase>()
    // 📖 25-Jul-2026: reading-mode preference — SAME BasePreferences the Settings screen writes, so
    //   the reader's toggle and Settings stay in sync. UI never sees the shared prefs type directly.
    private val userPrefs by inject<BasePreferences>()

    private val _uiState = MutableStateFlow(BookUiState())
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

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
    // 🔧 19-Jul-2026: FIX — open ONE file as its own book (tapped doc card), not the whole note
    private var singleContentMode: Boolean = false

    // 📖 23-Jul-2026: the document whose reading progress is being tracked, and the last shown page.
    private var progressContentId: String? = null
    private var lastKnownPage: Int = 0
    private var totalPages: Int = 0

    fun load(noteId: String, startContentId: String? = null, singleContent: Boolean = false) {
        this.startContentId = startContentId
        this.singleContentMode = singleContent && startContentId != null
        makeAWish(NOTES_CODES.READ) { readNoteUseCase.invoke(noteId) }
    }

    fun onPageChanged(index: Int) {
        lastKnownPage = index
        scheduleProgressSave()
    }

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
        // 🔧 19-Jul-2026: FIX (loader lifecycle) — the flow can emit Loading again after content
        //   is up; never bring the loader back over already-built pages.
        _uiState.update { if (it.pages.isEmpty()) it.copy(isLoading = true) else it }
    }

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        val note = result.data.data as Note
        // 🔧 18-Jul-2026: page building does file IO (pdf page counts, txt reads) → IO dispatcher
        viewModelScope.launch(Dispatchers.IO) {
            // 🔧 19-Jul-2026: single-file mode builds pages for ONLY the tapped content (DRY factory)
            val pages = if (singleContentMode) {
                note.contents.firstOrNull { it.id == startContentId }
                    ?.let { BookPageFactory.buildForContent(it) } ?: BookPageFactory.buildForNote(note)
            } else BookPageFactory.buildForNote(note)
            val start = if (singleContentMode) 0
            else startContentId?.let { id -> pages.indexOfFirst { it.sourceContentId == id } } ?: -1

            // 📖 23-Jul-2026: track the document this book represents so progress can be saved onto it.
            //   Single-file mode = the opened file; whole-note book = the first paged document (its
            //   page indices are what we count). Resume from its saved progressPage when present.
            val progressContent = if (singleContentMode) {
                note.contents.filterIsInstance<NoteContentModel.MediaContent>()
                    .firstOrNull { it.id == startContentId }
            } else {
                note.contents.filterIsInstance<NoteContentModel.MediaContent>()
                    .firstOrNull { it.id == pages.firstNotNullOfOrNull { p -> p.sourceContentId } }
            }
            progressContentId = progressContent?.id
            totalPages = pages.size

            val savedPage = progressContent
                ?.takeIf { it.hasReadingProgress() }
                ?.progressPage
                ?.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            val resolvedStart = when {
                singleContentMode -> savedPage ?: 0
                start >= 0 -> start
                else -> savedPage ?: 0
            }
            lastKnownPage = resolvedStart
            _uiState.update {
                it.copy(
                    isLoading = false, error = null, note = note, pages = pages,
                    startPageIndex = resolvedStart
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

    override suspend fun logoutUserForcefully() { readNoteUseCase.logoutUser() }

    override fun clearError() { _uiState.update { it.copy(error = null) } }
}
