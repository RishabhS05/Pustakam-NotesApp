package com.app.pustakam.core.filesys.reader

import com.app.pustakam.core.model.models.response.notes.Note

// 📖 01-Aug-2026 Step 4 — the ONLY place pages are generated. Both reading modes consume this
//   output: page mode renders pages[i], scroll mode stacks the same list. Switching mode never
//   re-runs this, so page boundaries and reading progress stay identical between the two.
object PageLayoutEngine {

    /** Note -> pages. The single entry point both platforms call. */
    fun buildPages(note: Note, policy: PageLayoutPolicy): List<ReaderPage> =
        paginate(ReaderBlockBuilder.build(note, policy), policy)

    /** Swift-facing overload — Kotlin default arguments are not exposed to Swift. */
    fun buildPagesForNote(note: Note): List<ReaderPage> =
        buildPages(note, PageLayoutPolicy.standard())

    /**
     * Fill each page until the next block no longer fits, then start a new one.
     * A block is never split across pages: one taller than a whole page gets a page to itself.
     */
    fun paginate(blocks: List<ReaderBlock>, policy: PageLayoutPolicy): List<ReaderPage> {
        if (blocks.isEmpty()) return emptyList()
        val pages = mutableListOf<ReaderPage>()
        var current = mutableListOf<ReaderBlock>()
        // the container that reports what this page occupies, so the next block can be tested
        var budget = PageBudget.of(policy)

        fun flush() {
            if (current.isEmpty()) return
            pages.add(ReaderPage(pages.size, current.toList(), budget.occupied))
            current = mutableListOf()
            budget = PageBudget.of(policy)
        }

        for (block in blocks) {
            val measured = BlockHeightEstimator.measure(block, policy)

            // taller than a whole page — give it its own page rather than splitting it
            if (measured.height > policy.usableHeight) {
                flush()
                pages.add(ReaderPage(pages.size, listOf(block), measured.size))
                continue
            }
            if (!budget.fits(measured)) flush()
            current.add(block)
            budget = budget.place(measured)
        }
        flush()
        return pages
    }

    /** Page index carrying [contentId] — resolves a jump target without re-generating pages. */
    fun pageIndexOf(pages: List<ReaderPage>, contentId: String?): Int =
        pages.indexOfFirst { it.contains(contentId) }
}
