package com.app.pustakam.android.screen.bookReading

import com.app.pustakam.android.screen.notebookReader.BookPage
import com.app.pustakam.android.screen.notebookReader.ReadingMode
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

interface BookReaderIntent {
    data class LoadBook(val bookId: String?, val noteId: String?) : BookReaderIntent
    data class ToggleReadingMode(val readingMode: ReadingMode) : BookReaderIntent
    data class Bookmark(val book: NoteContentModel.MediaContent, val  page: Int) : BookReaderIntent
    data class PageChanged(val book : NoteContentModel.MediaContent, val page: Int) : BookReaderIntent
}
data class  BookUIState(
    val isLoading: Boolean = true,
    val doc : NoteContentModel.MediaContent? = null,
    val pageProgress : Int = 0,
    val readingMode: ReadingMode = ReadingMode.PAGE,
    // 📖 01-Aug-2026: the document's pages, built by the shared BookPageFactory
    val pages: List<BookPage> = emptyList(),
    val startPageIndex: Int = 0,
    val error: String? = null,
)