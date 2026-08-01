package com.app.pustakam.core.filesys.reader

// 📖 01-Aug-2026 Step 2 — the virtual page. A4 PROPORTIONS (1 : 1.4142) at the device's own width,
//   so a page is a real A4 sheet in shape but its units ARE screen units. That is what makes the
//   estimator exact instead of approximate: 1 policy unit == 1 dp == 1 pt on both platforms.
data class PageLayoutPolicy(
    val pageWidth: Float = A4_WIDTH,
    val pageHeight: Float = A4_HEIGHT,
    val marginTop: Float = 24f,
    val marginBottom: Float = 24f,
    val marginStart: Float = 20f,
    val marginEnd: Float = 20f,
    val blockGap: Float = 12f,
    // typography — drives the text line estimate
    val titleHeight: Float = 64f,
    val bodyFontSize: Float = 16f,
    val bodyLineHeight: Float = 26f,
    val captionHeight: Float = 20f,
    // media
    val gridColumns: Int = 2,
    val gridSpacing: Float = 8f,
    // grid cells are 4:3 landscape — square cells waste too much height on a mixed page
    val gridCellAspect: Float = 0.75f,
    val audioHeight: Float = 88f,
    val documentHeight: Float = 120f,
    val linkHeight: Float = 96f,
    val locationHeight: Float = 96f,
) {
    /** Step 2 — usable page box after margins. */
    val usableHeight: Float get() = pageHeight - marginTop - marginBottom
    val usableWidth: Float get() = pageWidth - marginStart - marginEnd

    /** A lone image never takes more than this, so it can still share a page. */
    val imageMaxHeight: Float get() = usableHeight * 0.55f
    val imageMinHeight: Float get() = usableHeight * 0.18f

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

    val imageCellHeight: Float get() = gridCellWidth * gridCellAspect
    val videoCellHeight: Float get() = gridCellWidth * VIDEO_ASPECT

    companion object {
        const val A4_RATIO = 1.4142f
        const val A4_WIDTH = 794f
        const val A4_HEIGHT = 1123f
        // mean glyph advance as a fraction of font size — serif body copy
        const val AVG_CHAR_WIDTH_RATIO = 0.5f
        const val VIDEO_ASPECT = 9f / 16f

        /** Swift-facing factory — Kotlin default arguments are not exposed to Swift. */
        fun standard(): PageLayoutPolicy = PageLayoutPolicy()

        /**
         * The page both platforms actually use: the device width, A4 proportions.
         * Text stays readable AND the two platforms still produce identical pages at equal widths.
         */
        fun forWidth(width: Float): PageLayoutPolicy =
            PageLayoutPolicy(pageWidth = width, pageHeight = width * A4_RATIO)

        fun sized(width: Float, height: Float): PageLayoutPolicy =
            PageLayoutPolicy(pageWidth = width, pageHeight = height)

        /** Swift-facing accessors — a `const val` has no guaranteed export shape. */
        fun a4Ratio(): Float = A4_RATIO
        fun videoAspect(): Float = VIDEO_ASPECT
    }
}
