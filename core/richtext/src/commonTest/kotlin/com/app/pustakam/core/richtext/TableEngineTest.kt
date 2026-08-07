package com.app.pustakam.core.richtext

import com.app.pustakam.core.richtext.engine.TableEngine
import com.app.pustakam.core.richtext.model.TableData
import com.app.pustakam.core.richtext.model.TextAlign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TableEngineTest {

    private val table = TableData.empty(3, 3)

    @Test
    fun rowsAndColumnsAreAddedAndRemoved() {
        assertEquals(4, TableEngine.addRow(table).rowCount)
        assertEquals(2, TableEngine.deleteRow(table, 0).rowCount)
        assertEquals(4, TableEngine.addColumn(table).columnCount)
        assertEquals(2, TableEngine.deleteColumn(table, 0).columnCount)
    }

    @Test
    fun theLastRowAndColumnCannotBeDeleted() {
        val single = TableData.empty(1, 1)
        assertEquals(1, TableEngine.deleteRow(single, 0).rowCount)
        assertEquals(1, TableEngine.deleteColumn(single, 0).columnCount)
    }

    @Test
    fun addingAColumnStaysRectangular() {
        val wider = TableEngine.addColumn(table, 1)
        assertTrue(wider.rows.all { it.size == 4 })
        assertEquals(4, wider.columnWidths.size)
    }

    @Test
    fun mergeMarksTheAbsorbedCells() {
        val merged = TableEngine.mergeCells(table, 0, 0, columnSpan = 2, rowSpan = 1)

        assertEquals(2, merged.cellAt(0, 0)?.columnSpan)
        assertTrue(merged.cellAt(0, 1)?.merged == true)
    }

    @Test
    fun splitRestoresTheAbsorbedCells() {
        val merged = TableEngine.mergeCells(table, 0, 0, columnSpan = 2, rowSpan = 1)
        val split = TableEngine.splitCell(merged, 0, 0)

        assertEquals(1, split.cellAt(0, 0)?.columnSpan)
        assertFalse(split.cellAt(0, 1)?.merged == true)
    }

    @Test
    fun headerFlagsToggle() {
        assertFalse(TableEngine.toggleHeaderRow(table).hasHeaderRow)
        assertTrue(TableEngine.toggleHeaderColumn(table).hasHeaderColumn)
    }

    @Test
    fun columnWidthHasAFloor() {
        val resized = TableEngine.resizeColumn(table, 0, 5f)
        assertEquals(TableEngine.MIN_COLUMN_WIDTH, resized.columnWidths[0])
    }

    @Test
    fun cellAlignmentAndBackgroundArePerCell() {
        var updated = TableEngine.setCellAlignment(table, 1, 1, TextAlign.CENTER)
        updated = TableEngine.setCellBackground(updated, 1, 1, "#FFEEDD")

        assertEquals(TextAlign.CENTER, updated.cellAt(1, 1)?.align)
        assertEquals("#FFEEDD", updated.cellAt(1, 1)?.backgroundColor)
        assertEquals(TextAlign.START, updated.cellAt(0, 0)?.align)
    }

    @Test
    fun headerDetectionFollowsTheFlags() {
        assertTrue(TableEngine.isHeader(table, 0, 1))
        assertFalse(TableEngine.isHeader(table, 1, 1))
    }
}
