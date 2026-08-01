package com.app.pustakam.core.filesys.reader

// 📖 01-Aug-2026 Step 2 — the virtual page. A4 at 96dpi, every number configurable, nothing hardcoded
//   at a call site. Both platforms construct the SAME policy so page boundaries are identical.
data class PageLayoutPolicy(
    val pageWidth: Float = A4_WIDTH,
    val pageHeight: Float = A4_HEIGHT,
    val marginTop: Float = MARGIN,
    val marginBottom: Float = MARGIN,
    val marginStart: Float = MARGIN,
    val marginEnd: Float = MARGIN,
    val blockGap: Float = 12f,
    // typography — drives the text line estimate
    val titleHeight: Float = 96f,
    val bodyFontSize: Float = 16f,
    val bodyLineHeight: Float = 26f,
    val captionHeight: Float = 22f,
    // media
    val imageMinHeight: Float = 180f,
    val imageMaxHeight: Float = 520f,
    val gridColumns: Int = 2,
    val gridSpacing: Float = 8f,
    val gridMaxCells: Int = 4,
    // grid cells are 4:3 landscape — square cells waste enough height to push a mixed page over
    val gridCellAspect: Float = 0.75f,
    val audioHeight: Float = 88f,
    val documentHeight: Float = 150f,
    val linkHeight: Float = 130f,
    val locationHeight: Float = 150f,
) {
    /** Step 2 — usable page box after margins. */
    val usableHeight: Float get() = pageHeight - marginTop - marginBottom
    val usableWidth: Float get() = pageWidth - marginStart - marginEnd

    /** Characters that fit on one line at [bodyFontSize] — the basis of the text estimate. */
    val charsPerLine: Int
        get() = (usableWidth / (bodyFontSize * AVG_CHAR_WIDTH_RATIO)).toInt().coerceAtLeast(1)

    /** Lines that fit on a full page — a Paragraph longer than this is split before pagination. */
    val linesPerPage: Int get() = (usableHeight / bodyLineHeight).toInt().coerceAtLeast(1)

    /** Longest text a single Paragraph block may hold before it is split at a word boundary. */
    val maxCharsPerParagraph: Int get() = charsPerLine * linesPerPage

    /** Cell width inside the 2-column grid. */
    val gridCellWidth: Float
        get() = (usableWidth - gridSpacing * (gridColumns - 1)) / gridColumns

    companion object {
        // A4 210×297mm at 96dpi
        const val A4_WIDTH = 794f
        const val A4_HEIGHT = 1123f
        const val MARGIN = 40f
        // mean glyph advance as a fraction of font size — serif body copy
        const val AVG_CHAR_WIDTH_RATIO = 0.5f

        /** Swift-facing factory — Kotlin default arguments are not exposed to Swift. */
        fun standard(): PageLayoutPolicy = PageLayoutPolicy()

        /** Swift-facing factory for a screen-sized page (scroll mode on a phone). */
        fun sized(width: Float, height: Float): PageLayoutPolicy =
            PageLayoutPolicy(pageWidth = width, pageHeight = height)
    }
}
