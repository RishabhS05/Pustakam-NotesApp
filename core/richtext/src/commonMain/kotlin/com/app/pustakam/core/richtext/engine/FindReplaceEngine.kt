package com.app.pustakam.core.richtext.engine

import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument

data class SearchMatch(val blockId: String, val start: Int, val end: Int)

/** Find and replace across every text-bearing block, formatting preserved. */
object FindReplaceEngine {

    fun find(
        document: RichDocument,
        query: String,
        matchCase: Boolean = false
    ): List<SearchMatch> {
        if (query.isEmpty()) return emptyList()
        val matches = mutableListOf<SearchMatch>()
        document.blocks.forEach { block ->
            val haystack = when (block) {
                is RichBlock.Text -> block.text
                is RichBlock.Code -> block.code
                else -> return@forEach
            }
            var index = haystack.indexOf(query, 0, ignoreCase = !matchCase)
            while (index >= 0) {
                matches.add(SearchMatch(block.id, index, index + query.length))
                index = haystack.indexOf(query, index + query.length, ignoreCase = !matchCase)
            }
        }
        return matches
    }

    fun next(matches: List<SearchMatch>, current: Int): Int =
        if (matches.isEmpty()) -1 else (current + 1).mod(matches.size)

    fun previous(matches: List<SearchMatch>, current: Int): Int =
        if (matches.isEmpty()) -1 else (current - 1).mod(matches.size)

    fun replaceOne(document: RichDocument, match: SearchMatch, replacement: String): RichDocument {
        val block = document.blockById(match.blockId) ?: return document
        return document.replacingBlock(replaceInBlock(block, match.start, match.end, replacement))
    }

    fun replaceAll(
        document: RichDocument,
        query: String,
        replacement: String,
        matchCase: Boolean = false
    ): RichDocument {
        if (query.isEmpty()) return document
        var result = document
        // right-to-left so earlier offsets stay valid as the text length changes
        find(document, query, matchCase).sortedByDescending { it.start }.forEach { match ->
            result = replaceOne(result, match, replacement)
        }
        return result
    }

    private fun replaceInBlock(
        block: RichBlock,
        start: Int,
        end: Int,
        replacement: String
    ): RichBlock = when (block) {
        is RichBlock.Text -> {
            val newText = block.text.replaceRange(start, end, replacement)
            val afterDelete = SpanEngine.afterDelete(block.spans, start, end)
            val afterInsert = SpanEngine.afterInsert(afterDelete, start, replacement.length)
            block.copy(text = newText, spans = SpanEngine.normalize(afterInsert, newText.length))
        }

        is RichBlock.Code -> block.copy(code = block.code.replaceRange(start, end, replacement))
        else -> block
    }
}
