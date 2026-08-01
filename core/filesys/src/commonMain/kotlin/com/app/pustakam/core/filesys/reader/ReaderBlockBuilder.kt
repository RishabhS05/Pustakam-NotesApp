package com.app.pustakam.core.filesys.reader

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

// 📖 01-Aug-2026 Step 1 — Content -> Block. ONE forward pass over the ordered contents that only
//   ever appends, so reading order cannot be reordered: it is structurally impossible, not merely
//   avoided. Grouping merges CONSECUTIVE runs only, which is why `Image Image Text Image` becomes
//   ImageGrid(2), Paragraph, ImageGrid(1) and never ImageGrid(3).
object ReaderBlockBuilder {

    fun build(note: Note, policy: PageLayoutPolicy): List<ReaderBlock> {
        val blocks = mutableListOf<ReaderBlock>()
        val title = note.title?.takeIf { it.isNotBlank() } ?: "Untitled note"
        val contents = note.contents.sortedBy { it.position }
        blocks.add(ReaderBlock.Title(title, "${contents.size} entries"))
        blocks.addAll(buildBlocks(contents, policy))
        return blocks
    }

    /** Swift-facing overload — Kotlin default arguments are not exposed to Swift. */
    fun buildForNote(note: Note): List<ReaderBlock> = build(note, PageLayoutPolicy.standard())

    fun buildBlocks(contents: List<NoteContentModel>, policy: PageLayoutPolicy): List<ReaderBlock> {
        // anything taller than a page is split BEFORE pagination, so Step 4 never has to
        return group(contents).flatMap { block ->
            when (block) {
                is ReaderBlock.Paragraph -> splitParagraph(block, policy)
                is ReaderBlock.ImageGrid -> splitImageGrid(block, policy)
                is ReaderBlock.VideoGrid -> splitVideoGrid(block, policy)
                else -> listOf(block)
            }
        }
    }

    // ---- Step 1c: a grid that cannot fit one page becomes consecutive grids, order preserved ----

    fun splitImageGrid(block: ReaderBlock.ImageGrid, policy: PageLayoutPolicy): List<ReaderBlock.ImageGrid> {
        if (block.items.size <= 1) return listOf(block)
        val perPage = BlockHeightEstimator.maxItemsPerPage(policy.imageCellHeight, policy)
        if (block.items.size <= perPage) return listOf(block)
        return block.items.chunked(perPage).map { chunk ->
            ReaderBlock.ImageGrid(chunk, chunk.map { it.id })
        }
    }

    fun splitVideoGrid(block: ReaderBlock.VideoGrid, policy: PageLayoutPolicy): List<ReaderBlock.VideoGrid> {
        if (block.items.size <= 1) return listOf(block)
        val perPage = BlockHeightEstimator.maxItemsPerPage(policy.videoCellHeight, policy)
        if (block.items.size <= perPage) return listOf(block)
        return block.items.chunked(perPage).map { chunk ->
            ReaderBlock.VideoGrid(chunk, chunk.map { it.id })
        }
    }

    // ---- Step 1: consecutive-run grouping ----

    private fun group(contents: List<NoteContentModel>): List<ReaderBlock> {
        val blocks = mutableListOf<ReaderBlock>()
        // buffers for the run currently being accumulated; only one is ever non-empty
        val images = mutableListOf<NoteContentModel.MediaContent>()
        val videos = mutableListOf<NoteContentModel.MediaContent>()
        val texts = mutableListOf<NoteContentModel.TextContent>()

        fun flush() {
            if (images.isNotEmpty()) {
                blocks.add(ReaderBlock.ImageGrid(images.toList(), images.map { it.id }))
                images.clear()
            }
            if (videos.isNotEmpty()) {
                blocks.add(ReaderBlock.VideoGrid(videos.toList(), videos.map { it.id }))
                videos.clear()
            }
            if (texts.isNotEmpty()) {
                blocks.add(
                    ReaderBlock.Paragraph(
                        text = texts.joinToString("\n\n") { it.text },
                        chunkIndex = 1, chunkCount = 1,
                        sourceContentIds = texts.map { it.id },
                    )
                )
                texts.clear()
            }
        }

        for (content in contents) {
            when (content) {
                is NoteContentModel.TextContent -> {
                    if (content.text.isBlank()) continue
                    if (images.isNotEmpty() || videos.isNotEmpty()) flush()
                    texts.add(content)
                }

                is NoteContentModel.MediaContent -> when (content.type) {
                    ContentType.IMAGE, ContentType.GIF -> {
                        if (videos.isNotEmpty() || texts.isNotEmpty()) flush()
                        images.add(content)
                    }

                    ContentType.VIDEO -> {
                        if (images.isNotEmpty() || texts.isNotEmpty()) flush()
                        videos.add(content)
                    }

                    // audio is never grouped — AudioPlayView renders one player per block
                    ContentType.AUDIO -> {
                        flush()
                        blocks.add(ReaderBlock.Audio(content, listOf(content.id)))
                    }

                    else -> {
                        flush()
                        blocks.add(ReaderBlock.Document(content, listOf(content.id)))
                    }
                }

                is NoteContentModel.Link -> {
                    flush()
                    blocks.add(ReaderBlock.Link(content.url, listOf(content.id)))
                }

                is NoteContentModel.Location -> {
                    flush()
                    blocks.add(
                        ReaderBlock.Location(
                            content.latitude, content.longitude, content.address, listOf(content.id),
                        )
                    )
                }
            }
        }
        flush()
        return blocks
    }

    // ---- Step 1b: a paragraph never spans a page, so oversized text is cut at a word boundary ----

    fun splitParagraph(block: ReaderBlock.Paragraph, policy: PageLayoutPolicy): List<ReaderBlock.Paragraph> {
        val limit = policy.maxCharsPerParagraph
        if (block.text.length <= limit) return listOf(block)
        val chunks = mutableListOf<String>()
        var remaining = block.text
        while (remaining.length > limit) {
            val window = remaining.substring(0, limit)
            // same cut preference as BookPaginator: last newline, else last space, else hard cut
            val cut = window.lastIndexOf('\n').takeIf { it > limit / 2 }
                ?: window.lastIndexOf(' ').takeIf { it > limit / 2 }
                ?: limit
            chunks.add(remaining.substring(0, cut))
            remaining = remaining.substring(cut).trimStart('\n', ' ')
        }
        if (remaining.isNotEmpty()) chunks.add(remaining)
        return chunks.mapIndexed { index, text ->
            block.copy(text = text, chunkIndex = index + 1, chunkCount = chunks.size)
        }
    }
}
