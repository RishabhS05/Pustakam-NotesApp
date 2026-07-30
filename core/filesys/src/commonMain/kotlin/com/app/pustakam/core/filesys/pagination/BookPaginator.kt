package com.app.pustakam.core.filesys.pagination

// 🔧 30-Jul-2026 02:10 Phase 2 — book pagination. Ported VERBATIM from androidApp
//   BookPageFactory.paginate, which now delegates here.
//
// 🔧 30-Jul-2026 Phase 4 — DELIBERATELY NOT SHARED WITH iOS. The rule below is business logic and
//   is identical on both platforms, but the UNIT it counts in is platform semantics:
//     • Kotlin String.length  = UTF-16 code units
//     • Swift  String.count   = Characters (grapheme clusters)
//   They agree for ASCII, CJK and precomposed accents, and disagree for emoji and combining marks
//   (a ZWJ family emoji is 1 Character but 11 UTF-16 units). Running iOS through this object would
//   therefore re-paginate any TXT/MD containing such text and MOVE the reader's saved position,
//   since progressPage/totalPages are persisted per MediaContent.
//   iOS keeps its own grapheme-based implementation in BookReaderView.BookPagesBuilder and reads
//   the CONSTANTS below, so the numbers can never drift apart even though the algorithms differ.
//   PDFs are unaffected either way — they paginate by PDF page, not by this.
//   Pure: no file access, no rendering, no platform type.
object BookPaginator {

    /** A book page holds fewer chars than an editor block — it should feel like a real page. */
    const val CHARS_PER_PAGE = 700

    /** Text files above this are truncated before pagination (memory guard). Platform readers
     *  apply the cap when loading bytes; kept here so the number has one home. */
    const val MAX_TEXT_FILE_BYTES = 2L * 1024 * 1024

    // 🔧 30-Jul-2026 Phase 4 — explicit accessors for Swift. A Kotlin `const val` inside an object
    //   does not export to ObjC/Swift in an obvious, guaranteed shape, whereas a function on an
    //   object always does (BookPaginator.shared.charsPerPage()). Kotlin callers keep using the
    //   const directly — it stays a compile-time constant, so BookPageFactory's aliases still work.
    fun charsPerPage(): Int = CHARS_PER_PAGE

    fun maxTextFileBytes(): Long = MAX_TEXT_FILE_BYTES

    /**
     * Where to cut a full-length window, preferring a word boundary.
     * Order is significant and matches the shipped rule exactly:
     *   last newline, else last space — but only past the halfway mark — else a hard cut.
     */
    fun calculatePageBreak(window: String, charsPerPage: Int = CHARS_PER_PAGE): Int {
        val limit = charsPerPage.coerceAtLeast(1)
        val half = limit / 2
        window.lastIndexOf('\n').takeIf { it > half }?.let { return it }
        window.lastIndexOf(' ').takeIf { it > half }?.let { return it }
        return limit
    }

    /**
     * Split [text] into ordered pages. Blank text yields no pages (an empty book, not a blank page)
     * — that is the existing behaviour and the reader relies on it.
     */
    fun paginate(text: String, charsPerPage: Int = CHARS_PER_PAGE): List<TextPageChunk> {
        if (text.isBlank()) return emptyList()
        val limit = charsPerPage.coerceAtLeast(1)
        val chunks = mutableListOf<String>()
        var remaining = text
        while (remaining.length > limit) {
            val window = remaining.substring(0, limit)
            val cut = calculatePageBreak(window, limit)
            chunks.add(remaining.substring(0, cut))
            remaining = remaining.substring(cut).trimStart('\n', ' ')
        }
        if (remaining.isNotEmpty()) chunks.add(remaining)
        return chunks.mapIndexed { index, chunk ->
            TextPageChunk(text = chunk, pageNumber = index + 1, totalPages = chunks.size)
        }
    }

    /**
     * Cheap lower bound on page count, for progress bars and prefetch sizing.
     * NOT authoritative: word-boundary cuts mean the real count is >= this. Never use it to
     * compute or restore a saved reading position — call [paginate] for that.
     */
    fun estimatePages(textLength: Int, charsPerPage: Int = CHARS_PER_PAGE): Int {
        if (textLength <= 0) return 0
        val limit = charsPerPage.coerceAtLeast(1)
        return (textLength + limit - 1) / limit
    }
}
