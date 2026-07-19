package com.app.pustakam.android.screen.bookReader

// 🔧 19-Jul-2026: NEW (DRY refactor) — page building extracted from BookReaderViewModel so the
//   full-screen reader AND the inline editor book widget build pages through the SAME functions.
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.util.ContentType
import java.io.File

// 🔧 19-Jul-2026: a book page holds fewer chars than an editor block — feels like a real page
const val CHARS_PER_BOOK_PAGE = 700
// 🔧 19-Jul-2026: text files above this are truncated for pagination (protects memory)
const val MAX_TEXT_FILE_BYTES = 2L * 1024 * 1024

object BookPageFactory {

    // 🔧 19-Jul-2026: whole-note book — cover + every content (moved from the ViewModel, unchanged)
    fun buildForNote(note: Note): List<BookPage> {
        val pages = mutableListOf<BookPage>()
        val contents = note.contents.sortedBy { it.position }
        pages.add(
            BookPage.Cover(
                title = note.title?.ifBlank { "Untitled note" } ?: "Untitled note",
                subtitle = "${contents.size} entries",
            )
        )
        contents.forEach { content -> pages.addAll(pagesFor(content)) }
        return pages
    }

    // 🔧 19-Jul-2026: single-file book — ONLY the tapped document's pages (fix: opening the 2nd
    //   file no longer shows the 1st file's pages first). No cover, straight into the file.
    fun buildForContent(content: NoteContentModel): List<BookPage> = pagesFor(content)

    // 🔧 19-Jul-2026: ONE content → its pages; the shared core both builders call (DRY)
    fun pagesFor(content: NoteContentModel): List<BookPage> = when (content) {
        is NoteContentModel.TextContent -> paginate(content.text, content.id)

        is NoteContentModel.MediaContent -> when (content.type) {
            ContentType.IMAGE, ContentType.GIF ->
                listOf(BookPage.ImagePage(content.localPath ?: content.url, content.title, content.id))

            ContentType.VIDEO, ContentType.AUDIO -> listOf(BookPage.MediaPage(content, content.id))

            ContentType.PDF -> pdfSheets(content)

            ContentType.TXT, ContentType.MD -> textFilePages(content)

            else -> listOf(BookPage.DocFilePage(content, content.id))
        }

        is NoteContentModel.Link -> listOf(BookPage.LinkPage(content.url, content.id))

        is NoteContentModel.Location ->
            listOf(BookPage.LocationPage(content.latitude, content.longitude, content.address, content.id))
    }

    // 🔧 19-Jul-2026: word-boundary pagination — same cut preference as TextBlockSplitter
    fun paginate(text: String, sourceId: String?): List<BookPage.TextPage> {
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

    // 🔧 19-Jul-2026: one book sheet per PDF page; count here, bitmaps rendered lazily by the UI
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

    // 🔧 19-Jul-2026: txt/md files become paginated text pages (md shown as plain text v1)
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
