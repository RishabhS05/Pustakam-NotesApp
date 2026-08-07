package com.app.pustakam.core.richtext.engine

import com.app.pustakam.core.richtext.model.TableCell
import com.app.pustakam.core.richtext.model.TableData
import com.app.pustakam.core.richtext.model.TableRow
import com.app.pustakam.core.richtext.model.TextAlign

/** Structural table edits. Every function returns a rectangular, consistent table. */
object TableEngine {

    fun addRow(table: TableData, at: Int = table.rowCount): TableData {
        val columns = table.columnCount.coerceAtLeast(1)
        val row = TableRow(List(columns) { TableCell() })
        return table.copy(rows = table.rows.toMutableList().apply { add(at.coerceIn(0, size), row) })
    }

    fun deleteRow(table: TableData, at: Int): TableData {
        if (table.rowCount <= 1 || at !in 0 until table.rowCount) return table
        return table.copy(rows = table.rows.toMutableList().apply { removeAt(at) })
    }

    fun addColumn(table: TableData, at: Int = table.columnCount): TableData {
        val index = at.coerceIn(0, table.columnCount)
        return table.copy(
            rows = table.rows.map { row ->
                TableRow(row.cells.toMutableList().apply { add(index, TableCell()) })
            },
            columnWidths = table.columnWidths.toMutableList()
                .apply { add(index.coerceIn(0, size), TableData.DEFAULT_COLUMN_WIDTH) }
        )
    }

    fun deleteColumn(table: TableData, at: Int): TableData {
        if (table.columnCount <= 1 || at !in 0 until table.columnCount) return table
        return table.copy(
            rows = table.rows.map { row ->
                TableRow(row.cells.toMutableList().apply { if (at < size) removeAt(at) })
            },
            columnWidths = table.columnWidths.toMutableList()
                .apply { if (at < size) removeAt(at) }
        )
    }

    fun updateCell(table: TableData, row: Int, column: Int, update: (TableCell) -> TableCell): TableData {
        val target = table.cellAt(row, column) ?: return table
        return table.copy(
            rows = table.rows.mapIndexed { rowIndex, tableRow ->
                if (rowIndex != row) tableRow
                else TableRow(tableRow.cells.toMutableList().apply { set(column, update(target)) })
            }
        )
    }

    fun setCellAlignment(table: TableData, row: Int, column: Int, align: TextAlign): TableData =
        updateCell(table, row, column) { it.copy(align = align) }

    fun setCellBackground(table: TableData, row: Int, column: Int, color: String?): TableData =
        updateCell(table, row, column) { it.copy(backgroundColor = color) }

    /** Merges [column, column + span) of one row; the absorbed cells stay as hidden placeholders. */
    fun mergeCells(table: TableData, row: Int, column: Int, columnSpan: Int, rowSpan: Int = 1): TableData {
        val anchor = table.cellAt(row, column) ?: return table
        val safeColumnSpan = columnSpan.coerceIn(1, table.columnCount - column)
        val safeRowSpan = rowSpan.coerceIn(1, table.rowCount - row)
        if (safeColumnSpan == 1 && safeRowSpan == 1) return table

        return table.copy(
            rows = table.rows.mapIndexed { rowIndex, tableRow ->
                if (rowIndex !in row until row + safeRowSpan) return@mapIndexed tableRow
                TableRow(tableRow.cells.mapIndexed { columnIndex, cell ->
                    when {
                        rowIndex == row && columnIndex == column ->
                            anchor.copy(columnSpan = safeColumnSpan, rowSpan = safeRowSpan, merged = false)

                        columnIndex in column until column + safeColumnSpan ->
                            cell.copy(merged = true, columnSpan = 1, rowSpan = 1)

                        else -> cell
                    }
                })
            }
        )
    }

    fun splitCell(table: TableData, row: Int, column: Int): TableData {
        val anchor = table.cellAt(row, column) ?: return table
        if (!anchor.isSpanning) return table
        val columnRange = column until column + anchor.columnSpan
        val rowRange = row until row + anchor.rowSpan

        return table.copy(
            rows = table.rows.mapIndexed { rowIndex, tableRow ->
                if (rowIndex !in rowRange) return@mapIndexed tableRow
                TableRow(tableRow.cells.mapIndexed { columnIndex, cell ->
                    when {
                        rowIndex == row && columnIndex == column ->
                            cell.copy(columnSpan = 1, rowSpan = 1, merged = false)

                        columnIndex in columnRange -> cell.copy(merged = false)
                        else -> cell
                    }
                })
            }
        )
    }

    fun toggleHeaderRow(table: TableData): TableData = table.copy(hasHeaderRow = !table.hasHeaderRow)

    fun toggleHeaderColumn(table: TableData): TableData =
        table.copy(hasHeaderColumn = !table.hasHeaderColumn)

    fun resizeColumn(table: TableData, column: Int, width: Float): TableData {
        if (column !in 0 until table.columnCount) return table
        val widths = table.columnWidths
            .ifEmpty { List(table.columnCount) { TableData.DEFAULT_COLUMN_WIDTH } }
            .toMutableList()
        widths[column] = width.coerceAtLeast(MIN_COLUMN_WIDTH)
        return table.copy(columnWidths = widths)
    }

    fun isHeader(table: TableData, row: Int, column: Int): Boolean =
        (table.hasHeaderRow && row == 0) || (table.hasHeaderColumn && column == 0)

    const val MIN_COLUMN_WIDTH = 56f
}
