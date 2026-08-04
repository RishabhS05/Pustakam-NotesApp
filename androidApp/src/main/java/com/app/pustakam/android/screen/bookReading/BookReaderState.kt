package com.app.pustakam.android.screen.bookReading

import com.app.pustakam.android.screen.notebookReader.BookPage
import com.app.pustakam.android.screen.notebookReader.ReadingMode
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

interface BookReaderIntent {
    data class LoadBook(val bookId: String?) : BookReaderIntent
    data class ToggleReadingMode(val readingMode: ReadingMode) : BookReaderIntent
    data class Bookmark(val book: NoteContentModel.MediaContent, val  page: Int) : BookReaderIntent
    data class PageChanged(val book : NoteContentModel.MediaContent, val page: Int) : BookReaderIntent
}
data class  BookUIState(
    val isLoading: Boolean = true,
    val doc : NoteContentModel.MediaContent? = null,
    val pageProgress : Int = 0,
    val readingMode: ReadingMode = ReadingMode.PAGE,
    val pages: List<BookPage> = emptyList(),
    val error: String? = null,
)