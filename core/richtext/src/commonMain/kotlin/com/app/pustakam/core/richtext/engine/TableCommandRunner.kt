package com.app.pustakam.core.richtext.engine

import com.app.pustakam.core.richtext.model.TableData
import com.app.pustakam.core.richtext.presentation.TableCommand

/** Maps a [TableCommand] onto [TableEngine] so the reducer stays free of table branching. */
object TableCommandRunner {

    fun run(table: TableData, command: TableCommand): TableData = when (command) {
        is TableCommand.AddRow -> TableEngine.addRow(table, command.at)
        is TableCommand.DeleteRow -> TableEngine.deleteRow(table, command.at)
        is TableCommand.AddColumn -> TableEngine.addColumn(table, command.at)
        is TableCommand.DeleteColumn -> TableEngine.deleteColumn(table, command.at)
        is TableCommand.MergeCells -> TableEngine.mergeCells(
            table, command.row, command.column, command.columnSpan, command.rowSpan
        )

        is TableCommand.SplitCell -> TableEngine.splitCell(table, command.row, command.column)
        is TableCommand.CellAlignment ->
            TableEngine.setCellAlignment(table, command.row, command.column, command.align)

        is TableCommand.CellBackground ->
            TableEngine.setCellBackground(table, command.row, command.column, command.color)

        is TableCommand.ResizeColumn -> TableEngine.resizeColumn(table, command.column, command.width)
        TableCommand.ToggleHeaderRow -> TableEngine.toggleHeaderRow(table)
        TableCommand.ToggleHeaderColumn -> TableEngine.toggleHeaderColumn(table)
    }

    fun updateCellText(table: TableData, row: Int, column: Int, text: String): TableData {
        val cell = table.cellAt(row, column) ?: return table
        val spans = SpanEngine.normalize(
            SpanEngine.afterInsert(cell.spans, cell.text.length, 0), text.length
        )
        return TableEngine.updateCell(table, row, column) { it.copy(text = text, spans = spans) }
    }
}
