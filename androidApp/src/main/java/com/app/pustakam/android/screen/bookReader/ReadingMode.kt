package com.app.pustakam.android.screen.bookReader

// 📖 23-Jul-2026: NEW — reading mode. PAGE keeps the book feel (curling leaves); SCROLL lays the
//   same pages out as one continuous document, which suits long PDFs and text. Mirrors iOS
//   ReadingMode in BookReaderView.swift; both platforms persist the same raw strings.
enum class ReadingMode(val key: String, val title: String) {
    PAGE("page", "Page curl"),
    SCROLL("scroll", "Scrolling");

    fun toggled(): ReadingMode = if (this == PAGE) SCROLL else PAGE

    companion object {
        fun from(key: String?): ReadingMode = entries.firstOrNull { it.key == key } ?: PAGE
    }
}
