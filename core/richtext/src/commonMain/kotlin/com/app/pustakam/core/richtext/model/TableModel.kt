package com.app.pustakam.core.richtext.model

import kotlinx.serialization.Serializable

@Serializable
data class TableCell(
    val text: String = "",
    val spans: List<RichSpan> = emptyList(),
    val align: TextAlign = TextAlign.START,
    val backgroundColor: String? = null,
    val rowSpan: Int = 1,
    val columnSpan: Int = 1,
    val merged: Boolean = false
) {
    val isSpanning: Boolean get() = rowSpan > 1 || columnSpan > 1
}

@Serializable
data class TableRow(val cells: List<TableCell> = emptyList()) {
    val size: Int get() = cells.size
}

@Serializable
data class TableData(
    val rows: List<TableRow> = emptyList(),
    val hasHeaderRow: Boolean = true,
    val hasHeaderColumn: Boolean = false,
    val columnWidths: List<Float> = emptyList()
) {
    val rowCount: Int get() = rows.size

    val columnCount: Int get() = rows.firstOrNull()?.size ?: 0

    fun cellAt(row: Int, column: Int): TableCell? = rows.getOrNull(row)?.cells?.getOrNull(column)

    companion object {
        const val DEFAULT_COLUMN_WIDTH = 120f

        fun empty(rows: Int, columns: Int): TableData {
            val safeRows = rows.coerceAtLeast(1)
            val safeColumns = columns.coerceAtLeast(1)
            return TableData(
                rows = List(safeRows) { TableRow(List(safeColumns) { TableCell() }) },
                columnWidths = List(safeColumns) { DEFAULT_COLUMN_WIDTH }
            )
        }
    }
}
