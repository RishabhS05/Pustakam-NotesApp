package com.app.pustakam.android.screen.notebookReader

import com.app.pustakam.android.screen.bookUIView.PdfPageRenderer
import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.common.util.log_d
import com.app.pustakam.core.filesys.pagination.BookPaginator
import com.app.pustakam.core.filesys.reader.EmbeddedDocumentSource
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import java.io.File

// 📖 15-Aug-2026: how many sheets a document has, answered ONCE per file. PDFs are counted by the
//   renderer, txt/md are paginated by BookPaginator — the same numbers the full reader uses.
object EmbeddedDocumentProbe {

    fun probe(media: NoteContentModel.MediaContent): EmbeddedDocumentSource {
        val unreadable = EmbeddedDocumentSource.unsupported(media.id)
        val path = media.localPath?.takeIf { it.isNotEmpty() } ?: run {
            log_d("InlineDoc", "probe ${media.id} type=${media.type} NO localPath")
            return unreadable
        }
        val file = File(path)
        if (!file.exists()) {
            log_d("InlineDoc", "probe ${media.id} type=${media.type} file missing at $path")
            return unreadable
        }
        return try {
            when (media.type) {
                ContentType.PDF -> PdfPageRenderer.pageCount(path)
                    .takeIf { it > 0 }
                    ?.let { EmbeddedDocumentSource.pdf(media.id, it) }
                    ?: unreadable

                ContentType.TXT, ContentType.MD -> BookPaginator.paginate(BookPageFactory.readCappedText(file))
                    .map { it.text }
                    .takeIf { it.isNotEmpty() }
                    ?.let { EmbeddedDocumentSource.text(media.id, it) }
                    ?: unreadable

                else -> unreadable
            }.also { log_d("InlineDoc", "probe ${media.id} type=${media.type} -> kind=${it.kind} pages=${it.pageCount}") }
        } catch (e: Exception) {
            e.printStackTrace(); unreadable
        }
    }
}
