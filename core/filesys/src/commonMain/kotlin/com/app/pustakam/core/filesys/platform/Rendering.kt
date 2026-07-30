package com.app.pustakam.core.filesys.platform

import com.app.pustakam.core.filesys.model.ThumbnailRequest
import com.app.pustakam.core.filesys.model.ThumbnailResult

// 🔧 30-Jul-2026 02:10 Phase 3 — the three seams that wrap a heavyweight platform framework.
//   Kept separate from FileSystem.kt because these are the ones that stay native for good:
//   BitmapFactory / PdfRenderer / StaticLayout on Android, UIImage / PDFKit / UIFont on iOS.
//   The POLICY around them (target size, quality, frame time, pagination) is shared — see
//   ThumbnailPolicy and ExportLayoutBuilder.

/**
 * Produce a thumbnail. The size, quality and video frame time come from [ThumbnailRequest],
 * which is built by the shared ThumbnailPolicy — the implementation only decodes and scales.
 */
interface ThumbnailGenerator {
    fun generate(request: ThumbnailRequest): ThumbnailResult
}

/** Read structural facts out of a document without rendering it. */
interface DocumentRenderer {
    /** Page count for a PDF, or 0 when it cannot be opened. */
    fun pageCount(relativePath: String): Int
}

/**
 * Measure text so [com.app.pustakam.core.filesys.export.ExportLayoutBuilder] can paginate without
 * knowing how either platform lays out glyphs.
 *
 * An interface rather than an `expect` on purpose: a fake measurer in `commonTest` is the only way
 * to prove Android and iOS paginate exports identically.
 */
interface TextMeasurer {
    /** Height in points that [text] needs when wrapped to [width] at [style]. */
    fun measureHeight(text: String, style: TextStyle, width: Float): Float
}

/** The four text roles the exporter draws. Sizes live here so both platforms agree. */
enum class TextStyle(val sizePx: Float, val bold: Boolean) {
    TITLE(44f, true),
    BODY(30f, false),
    CAPTION(24f, false),
    ACCENT(28f, false),
}
