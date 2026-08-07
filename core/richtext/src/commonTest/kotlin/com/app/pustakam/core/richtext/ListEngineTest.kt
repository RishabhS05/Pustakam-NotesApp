package com.app.pustakam.core.richtext

import com.app.pustakam.core.richtext.engine.ListEngine
import com.app.pustakam.core.richtext.model.ListMarker
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListEngineTest {

    private fun item(text: String, style: ListStyle, level: Int): RichBlock.Text =
        RichDocument.newTextBlock(text = text, list = ListMarker(style, level))

    @Test
    fun toggleAddsThenRemovesTheMarker() {
        val plain = RichDocument.newTextBlock("item")
        val bulleted = ListEngine.toggle(plain, ListStyle.BULLET)
        assertEquals(ListStyle.BULLET, bulleted.list?.style)

        val cleared = ListEngine.toggle(bulleted, ListStyle.BULLET)
        assertNull(cleared.list)
    }

    @Test
    fun switchingStyleKeepsTheLevel() {
        val nested = item("item", ListStyle.BULLET, level = 2)
        val numbered = ListEngine.toggle(nested, ListStyle.NUMBERED)

        assertEquals(ListStyle.NUMBERED, numbered.list?.style)
        assertEquals(2, numbered.list?.level)
    }

    @Test
    fun indentAndOutdentWalkTheLevels() {
        var block = item("item", ListStyle.BULLET, level = 0)
        block = ListEngine.indent(block)
        assertEquals(1, block.list?.level)

        block = ListEngine.outdent(block)
        assertEquals(0, block.list?.level)

        block = ListEngine.outdent(block)
        assertNull(block.list)
    }

    @Test
    fun indentIsCappedAtMaxLevel() {
        var block = item("item", ListStyle.BULLET, level = ListEngine.MAX_LEVEL)
        block = ListEngine.indent(block)

        assertEquals(ListEngine.MAX_LEVEL, block.list?.level)
        assertFalse(ListEngine.canIndent(block))
    }

    @Test
    fun numberingCountsPerLevel() {
        val blocks = listOf(
            item("one", ListStyle.NUMBERED, 0),
            item("two", ListStyle.NUMBERED, 0),
            item("two-a", ListStyle.NUMBERED, 1),
            item("two-b", ListStyle.NUMBERED, 1),
            item("three", ListStyle.NUMBERED, 0)
        )
        val numbers = ListEngine.numbering(blocks)

        assertEquals(1, numbers[blocks[0].id])
        assertEquals(2, numbers[blocks[1].id])
        assertEquals(1, numbers[blocks[2].id])
        assertEquals(2, numbers[blocks[3].id])
        assertEquals(3, numbers[blocks[4].id])
    }

    @Test
    fun nestedLevelRestartsAfterSteppingBackOut() {
        val blocks = listOf(
            item("one", ListStyle.NUMBERED, 0),
            item("one-a", ListStyle.NUMBERED, 1),
            item("two", ListStyle.NUMBERED, 0),
            item("two-a", ListStyle.NUMBERED, 1)
        )
        val numbers = ListEngine.numbering(blocks)

        assertEquals(1, numbers[blocks[1].id])
        assertEquals(1, numbers[blocks[3].id])
    }

    @Test
    fun aPlainBlockEndsTheSequence() {
        val blocks = listOf(
            item("one", ListStyle.NUMBERED, 0),
            RichDocument.newTextBlock("interruption"),
            item("restarted", ListStyle.NUMBERED, 0)
        )
        val numbers = ListEngine.numbering(blocks)

        assertEquals(1, numbers[blocks[0].id])
        assertEquals(1, numbers[blocks[2].id])
    }

    @Test
    fun restartNumberingHonoursTheStartNumber() {
        val blocks = listOf(
            item("one", ListStyle.NUMBERED, 0),
            ListEngine.restartNumbering(item("five", ListStyle.NUMBERED, 0), startNumber = 5),
            item("six", ListStyle.NUMBERED, 0)
        )
        val numbers = ListEngine.numbering(blocks)

        assertEquals(5, numbers[blocks[1].id])
        assertEquals(6, numbers[blocks[2].id])
    }

    @Test
    fun mixedNestingKeepsNumberedCountersIntact() {
        val blocks = listOf(
            item("one", ListStyle.NUMBERED, 0),
            item("bullet", ListStyle.BULLET, 1),
            item("two", ListStyle.NUMBERED, 0)
        )
        val numbers = ListEngine.numbering(blocks)

        assertEquals(1, numbers[blocks[0].id])
        assertEquals(2, numbers[blocks[2].id])
    }

    @Test
    fun checklistTogglesOnlyForChecklists() {
        val checklist = ListEngine.toggleChecked(item("task", ListStyle.CHECKLIST, 0))
        assertTrue(checklist.checked)

        val bullet = ListEngine.toggleChecked(item("task", ListStyle.BULLET, 0))
        assertFalse(bullet.checked)
    }

    @Test
    fun emptyListItemExitsTheList() {
        assertTrue(ListEngine.shouldExitList(item("", ListStyle.BULLET, 0)))
        assertFalse(ListEngine.shouldExitList(item("text", ListStyle.BULLET, 0)))
    }
}
