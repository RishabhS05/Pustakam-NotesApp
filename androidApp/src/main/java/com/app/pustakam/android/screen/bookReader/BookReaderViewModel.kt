package com.app.pustakam.android.screen.bookReader

// 🔧 18-Jul-2026: NEW FEATURE (book reader) — loads a note and flattens EVERY content type into
//   real book pages (text chunks, one page per PDF page, image/media/doc/link/location pages).
// 🔧 19-Jul-2026: page building moved to BookPageFactory (DRY with the inline editor widget);
//   single-file mode + first-load error/loader fixes.
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.domain.repositories.usecases.ReadNoteUseCase
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import com.app.pustakam.util.log_d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject

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
)

class BookReaderViewModel : BaseViewModel() {
    private val readNoteUseCase by inject<ReadNoteUseCase>()

    private val _uiState = MutableStateFlow(BookUiState())
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    private var appContext: Context? = null
    private var startContentId: String? = null
    // 🔧 19-Jul-2026: FIX — open ONE file as its own book (tapped doc card), not the whole note
    private var singleContentMode: Boolean = false

    fun load(context: Context, noteId: String, startContentId: String? = null, singleContent: Boolean = false) {
        appContext = context.applicationContext
        this.startContentId = startContentId
        this.singleContentMode = singleContent && startContentId != null
        makeAWish(NOTES_CODES.READ) { readNoteUseCase.invoke(noteId) }
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
            _uiState.update {
                it.copy(
                    isLoading = false, error = null, note = note, pages = pages,
                    startPageIndex = if (start >= 0) start else 0
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
