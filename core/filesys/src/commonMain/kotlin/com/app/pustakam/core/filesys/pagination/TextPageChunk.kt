package com.app.pustakam.core.filesys.pagination

// 🔧 30-Jul-2026 02:10 Phase 2 — one paginated page of text, platform-agnostic.
//   Platform readers map this onto their own page type (Android BookPage.TextPage,
//   iOS BookPageItem) so the page-splitting rule itself lives in exactly one place.
data class TextPageChunk(
    val text: String,
    /** 1-based, as shown to the reader. */
    val pageNumber: Int,
    val totalPages: Int,
)
