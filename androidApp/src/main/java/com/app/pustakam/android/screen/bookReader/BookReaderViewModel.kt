package com.app.pustakam.android.screen.bookReader

// 🔧 18-Jul-2026: NEW FEATURE (book reader) — loads a note and flattens EVERY content type into
//   real book pages (text chunks, one page per PDF page, image/media/doc/link/location pages).
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.domain.repositories.usecases.ReadNoteUseCase
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.io.File

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

// 🔧 18-Jul-2026: a book page holds fewer chars than an editor block — feels like a real page
private const val CHARS_PER_BOOK_PAGE = 700
// 🔧 18-Jul-2026: text files above this are truncated for pagination (protects memory)
private const val MAX_TEXT_FILE_BYTES = 2L * 1024 * 1024

class BookReaderViewModel : BaseViewModel() {
    private val readNoteUseCase by inject<ReadNoteUseCase>()

    private val _uiState = MutableStateFlow(BookUiState())
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    private var appContext: Context? = null
    private var startContentId: String? = null

    fun load(context: Context, noteId: String, startContentId: String? = null) {
        appContext = context.applicationContext
        this.startContentId = startContentId
        makeAWish(NOTES_CODES.READ) { readNoteUseCase.invoke(noteId) }
    }

    override fun onLoading(taskCode: TaskCode) {
        _uiState.update { it.copy(isLoading = true) }
    }

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        val note = result.data.data as Note
        // 🔧 18-Jul-2026: page building does file IO (pdf page counts, txt reads) → IO dispatcher
        viewModelScope.launch(Dispatchers.IO) {
            val pages = buildPages(note)
            val start = startContentId?.let { id -> pages.indexOfFirst { it.sourceContentId == id } } ?: -1
            _uiState.update {
                it.copy(isLoading = false, note = note, pages = pages, startPageIndex = if (start >= 0) start else 0)
            }
        }
    }

    override fun onFailure(taskCode: TaskCode, error: Error) {
        _uiState.update { it.copy(isLoading = false, error = (error as? NetworkError)?.getError() ?: "Couldn't open the book.") }
    }

    override suspend fun logoutUserForcefully() { readNoteUseCase.logoutUser() }

    override fun clearError() { _uiState.update { it.copy(error = null) } }

    // 🔧 18-Jul-2026: contents → pages; unreadable files degrade to DocFilePage, never crash
    private fun buildPages(note: Note): List<BookPage> {
        val pages = mutableListOf<BookPage>()
        val contents = note.contents.sortedBy { it.position }
        pages.add(
            BookPage.Cover(
                title = note.title?.ifBlank { "Untitled note" } ?: "Untitled note",
                subtitle = "${contents.size} entries",
            )
        )
        contents.forEach { content ->
            when (content) {
                is NoteContentModel.TextContent ->
                    pages.addAll(paginate(content.text, content.id))

                is NoteContentModel.MediaContent -> when (content.type) {
                    ContentType.IMAGE, ContentType.GIF ->
                        pages.add(BookPage.ImagePage(content.localPath ?: content.url, content.title, content.id))

                    ContentType.VIDEO, ContentType.AUDIO ->
                        pages.add(BookPage.MediaPage(content, content.id))

                    ContentType.PDF -> pages.addAll(pdfSheets(content))

                    ContentType.TXT, ContentType.MD -> pages.addAll(textFilePages(content))

                    // DOCX/EPUB/OTHER → file page with an "Open" action (system viewer)
                    else -> pages.add(BookPage.DocFilePage(content, content.id))
                }

                is NoteContentModel.Link -> pages.add(BookPage.LinkPage(content.url, content.id))

                is NoteContentModel.Location ->
                    pages.add(BookPage.LocationPage(content.latitude, content.longitude, content.address, content.id))
            }
        }
        return pages
    }

    // 🔧 18-Jul-2026: word-boundary pagination — same cut preference as TextBlockSplitter
    private fun paginate(text: String, sourceId: String?): List<BookPage.TextPage> {
        if (text.isBlank()) return emptyList()
        val chunks = mutableListOf<String>()
        var remaining = text
        while (remaining.length > CHARS_PER_BOOK_PAGE) {
            val window = remaining.substring(0, CHARS_PER_BOOK_PAGE)
            val cut = window.lastIndexOf('\n').takeIf { it > CHARS_PER_BOOK_PAGE / 2 }
                ?: window.lastIndexOf(' ').takeIf { it > CHARS_PER_BOOK_PAGE / 2 }
                ?: CHARS_PER_BOOK_PAGE
            chunks.add(remaining.substring(0, cut))
            remaining = remaining.substring(cut).trimStart('\n', ' ')
        }
        if (remaining.isNotEmpty()) chunks.add(remaining)
        return chunks.mapIndexed { i, chunk -> BookPage.TextPage(chunk, i + 1, chunks.size, sourceId) }
    }

    // 🔧 18-Jul-2026: one book sheet per PDF page; count read here, bitmaps rendered lazily by the UI
    private fun pdfSheets(media: NoteContentModel.MediaContent): List<BookPage> {
        val path = media.localPath ?: return listOf(BookPage.DocFilePage(media, media.id))
        return try {
            val file = File(path)
            if (!file.exists()) return listOf(BookPage.DocFilePage(media, media.id))
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val count = PdfRenderer(descriptor).use { it.pageCount }
            (0 until count).map { BookPage.PdfSheet(path, it, count, media.title, media.id) }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(BookPage.DocFilePage(media, media.id))
        }
    }

    // 🔧 18-Jul-2026: txt/md files become paginated text pages (md shown as plain text v1)
    private fun textFilePages(media: NoteContentModel.MediaContent): List<BookPage> {
        val path = media.localPath ?: return listOf(BookPage.DocFilePage(media, media.id))
        return try {
            val file = File(path)
            if (!file.exists()) return listOf(BookPage.DocFilePage(media, media.id))
            val text = file.inputStream().use { stream ->
                stream.readBytes().let { bytes ->
                    if (bytes.size > MAX_TEXT_FILE_BYTES) bytes.copyOf(MAX_TEXT_FILE_BYTES.toInt()) else bytes
                }.decodeToString()
            }
            paginate(text, media.id).ifEmpty { listOf(BookPage.DocFilePage(media, media.id)) }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(BookPage.DocFilePage(media, media.id))
        }
    }
}
