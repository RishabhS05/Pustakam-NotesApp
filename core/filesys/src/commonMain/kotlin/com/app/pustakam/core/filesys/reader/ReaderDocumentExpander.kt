package com.app.pustakam.core.filesys.reader

import com.app.pustakam.core.model.models.response.notes.NoteContentModel

// 📖 15-Aug-2026: expanding a document is a pure SPLICE into the page list produced by
//   PageLayoutEngine — never a re-pagination. Engine pages are passed through by reference, so
//   everything that is not a document sheet keeps its order, its identity and its page index.
object ReaderDocumentExpander {

    /** Sheets revealed per "Load more" — the first tap opens the same amount. */
    const val PAGE_WINDOW = 10

    /** Swift-facing accessor — a const val on an object has no guaranteed ObjC shape. */
    fun pageWindow(): Int = PAGE_WINDOW

    fun firstWindow(pageCount: Int): Int = PAGE_WINDOW.coerceAtMost(pageCount.coerceAtLeast(0))

    fun nextWindow(loadedCount: Int, pageCount: Int): Int =
        (loadedCount + PAGE_WINDOW).coerceIn(0, pageCount.coerceAtLeast(0))

    fun expand(
        basePages: List<ReaderPage>,
        sources: List<EmbeddedDocumentSource>,
        expansions: List<DocumentExpansion>,
        policy: PageLayoutPolicy,
    ): ExpandedPages {
        if (basePages.isEmpty() || sources.isEmpty() || expansions.isEmpty()) return ExpandedPages(basePages)
        val sourceById = sources.associateBy { it.contentId }
        val loadedById = expansions.associate { it.contentId to it.loadedCount }
        val sheetSize = BlockSize(policy.usableWidth, policy.usableHeight)
        val out = ArrayList<ReaderPage>(basePages.size)
        for (page in basePages) {
            val opens = page.blocks.any { block ->
                block is ReaderBlock.Document &&
                    sourceById[block.item.id]?.isReadable == true &&
                    (loadedById[block.item.id] ?: 0) > 0
            }
            // untouched pages pass through BY REFERENCE — order and identity are never disturbed
            if (!opens) {
                out.add(page)
                continue
            }
            out.addAll(split(page, sourceById, loadedById, sheetSize))
        }
        return ExpandedPages(out)
    }

    /**
     * A page holds several blocks, so whatever the note puts AFTER a document card would end up
     * above the sheets. The page is therefore cut at the card: blocks up to and including it stay,
     * the sheets follow, and the remainder becomes the very next page.
     */
    private fun split(
        page: ReaderPage,
        sourceById: Map<String, EmbeddedDocumentSource>,
        loadedById: Map<String, Int>,
        sheetSize: BlockSize,
    ): List<ReaderPage> {
        val out = mutableListOf<ReaderPage>()
        var buffer = mutableListOf<ReaderBlock>()
        var part = 0

        fun emit() {
            if (buffer.isEmpty()) return
            out.add(
                if (part == 0) page.copy(blocks = buffer.toList())
                else ReaderPage(page.index, buffer.toList(), page.occupied, part)
            )
            buffer = mutableListOf()
            part++
        }

        for (block in page.blocks) {
            buffer.add(block)
            if (block !is ReaderBlock.Document) continue
            val source = sourceById[block.item.id] ?: continue
            if (!source.isReadable) continue
            val loaded = loadedById[block.item.id] ?: continue
            if (loaded <= 0) continue
            emit()
            out.addAll(sheets(page, block.item, source, loaded, sheetSize))
        }
        emit()
        return out
    }

    private fun sheets(
        card: ReaderPage,
        item: NoteContentModel.MediaContent,
        source: EmbeddedDocumentSource,
        loadedCount: Int,
        sheetSize: BlockSize,
    ): List<ReaderPage> {
        val loaded = loadedCount.coerceIn(0, source.pageCount)
        if (loaded <= 0) return emptyList()
        return (1..loaded).map { pageNumber ->
            ReaderPage(
                // a sheet reports its CARD's page index, so reading progress never leaves the base list
                index = card.index,
                blocks = listOf(
                    ReaderBlock.DocumentPage(
                        item = item,
                        ref = EmbeddedPageRef(
                            contentId = source.contentId,
                            kind = source.kind,
                            pageNumber = pageNumber,
                            pageCount = source.pageCount,
                            loadedCount = loaded,
                        ),
                        text = source.textPages.getOrNull(pageNumber - 1),
                        sourceContentIds = listOf(source.contentId),
                    )
                ),
                occupied = sheetSize,
            )
        }
    }
}

// 📖 what the reader renders: base pages plus the sheets currently open under their cards
data class ExpandedPages(val pages: List<ReaderPage>) {
    val size: Int get() = pages.size

    /** Display position -> the engine page index progress is persisted against. */
    fun baseIndexOf(displayIndex: Int): Int = pages.getOrNull(displayIndex)?.index ?: 0

    /** Engine page index -> where that page now sits on screen. */
    fun displayIndexOf(baseIndex: Int): Int =
        pages.indexOfFirst { !it.isDocumentSheet && it.index == baseIndex }.coerceAtLeast(0)
}
