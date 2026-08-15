package com.app.pustakam.core.filesys.reader

// 📖 15-Aug-2026: a document opened INSIDE a note. The platform reports how many sheets it has;
//   every decision about how many of them to show belongs to ReaderDocumentExpander.
enum class EmbeddedDocumentKind { PDF, TEXT, UNSUPPORTED }

data class EmbeddedDocumentSource(
    val contentId: String,
    val kind: EmbeddedDocumentKind,
    val pageCount: Int,
    // 📖 TEXT only — paginated by the PLATFORM, because char counting differs on Kotlin and Swift
    val textPages: List<String> = emptyList(),
) {
    val isReadable: Boolean get() = kind != EmbeddedDocumentKind.UNSUPPORTED && pageCount > 0

    companion object {
        // 📖 Swift-facing factories — Kotlin default arguments are not exposed to Swift
        fun unsupported(contentId: String): EmbeddedDocumentSource =
            EmbeddedDocumentSource(contentId, EmbeddedDocumentKind.UNSUPPORTED, 0)

        fun pdf(contentId: String, pageCount: Int): EmbeddedDocumentSource =
            EmbeddedDocumentSource(contentId, EmbeddedDocumentKind.PDF, pageCount)

        fun text(contentId: String, pages: List<String>): EmbeddedDocumentSource =
            EmbeddedDocumentSource(contentId, EmbeddedDocumentKind.TEXT, pages.size, pages)
    }
}

// 📖 how much of one document is currently open; absent id or 0 means collapsed
data class DocumentExpansion(
    val contentId: String,
    val loadedCount: Int,
)

// 📖 what one injected sheet needs to draw itself and its collapse bar
data class EmbeddedPageRef(
    val contentId: String,
    val kind: EmbeddedDocumentKind,
    val pageNumber: Int,
    val pageCount: Int,
    val loadedCount: Int,
) {
    val pageIndex: Int get() = pageNumber - 1
    val isLastLoaded: Boolean get() = pageNumber >= loadedCount
    val remaining: Int get() = (pageCount - loadedCount).coerceAtLeast(0)
    val hasMore: Boolean get() = remaining > 0
    val showsLoadMore: Boolean get() = isLastLoaded && hasMore
}
