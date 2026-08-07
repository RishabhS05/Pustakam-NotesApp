package com.app.pustakam.core.richtext.presentation

import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichSpan
import com.app.pustakam.core.richtext.model.TextAlign
import com.app.pustakam.core.richtext.model.TextFormat

/** Every user action the editor understands. Both platforms dispatch exactly these. */
sealed class SmartTextIntent {

    data class TypeText(val blockId: String, val text: String, val caret: Int) : SmartTextIntent()

    data class SelectionChanged(val selection: DocumentSelection) : SmartTextIntent()

    data class SplitBlock(val blockId: String, val caret: Int) : SmartTextIntent()

    data class MergeWithPrevious(val blockId: String) : SmartTextIntent()

    data class ToggleFormat(val format: TextFormat) : SmartTextIntent()

    data class SetParagraphStyle(val style: ParagraphStyle) : SmartTextIntent()

    data class SetAlignment(val align: TextAlign) : SmartTextIntent()

    data class ToggleList(val style: ListStyle) : SmartTextIntent()

    data class ToggleChecked(val blockId: String) : SmartTextIntent()

    data object Indent : SmartTextIntent()

    data object Outdent : SmartTextIntent()

    data class SetFirstLineIndent(val enabled: Boolean) : SmartTextIntent()

    data class SetLineHeight(val value: Float) : SmartTextIntent()

    data class SetParagraphSpacing(val value: Float) : SmartTextIntent()

    data class SetTextColor(val color: String?) : SmartTextIntent()

    data class SetBackgroundColor(val color: String?) : SmartTextIntent()

    data class SetFontSize(val size: Float?) : SmartTextIntent()

    data class SetFontWeight(val weight: Int?) : SmartTextIntent()

    data class SetFontFamily(val family: String?) : SmartTextIntent()

    data object ToggleQuote : SmartTextIntent()

    data object InsertDivider : SmartTextIntent()

    data object InsertCodeBlock : SmartTextIntent()

    data class UpdateCodeBlock(val blockId: String, val code: String) : SmartTextIntent()

    data class InsertTable(val rows: Int, val columns: Int) : SmartTextIntent()

    data class UpdateTableCell(
        val blockId: String,
        val row: Int,
        val column: Int,
        val text: String
    ) : SmartTextIntent()

    data class TableAction(val blockId: String, val action: TableCommand) : SmartTextIntent()

    data class SetLink(val url: String?) : SmartTextIntent()

    data object RemoveLink : SmartTextIntent()

    data object ClearFormatting : SmartTextIntent()

    data object Undo : SmartTextIntent()

    data object Redo : SmartTextIntent()

    data object Copy : SmartTextIntent()

    data object Cut : SmartTextIntent()

    data class Paste(val text: String, val keepFormatting: Boolean = true) : SmartTextIntent()

    data object SelectAll : SmartTextIntent()

    data object DuplicateSelection : SmartTextIntent()

    data class SelectWord(val blockId: String, val offset: Int) : SmartTextIntent()

    data class SelectParagraph(val blockId: String) : SmartTextIntent()

    data class SetSearchQuery(val query: String, val matchCase: Boolean = false) : SmartTextIntent()

    data class SetReplacement(val replacement: String) : SmartTextIntent()

    data object FindNext : SmartTextIntent()

    data object FindPrevious : SmartTextIntent()

    data object ReplaceCurrent : SmartTextIntent()

    data object ReplaceAll : SmartTextIntent()

    data class SetSearchActive(val active: Boolean) : SmartTextIntent()

    data class SetToolbarExpanded(val expanded: Boolean) : SmartTextIntent()

    data class ApplySpanStyle(val span: RichSpan) : SmartTextIntent()

    data object DeleteFocusedBlock : SmartTextIntent()
}

/** Structural table commands, kept separate so the table sheet can list them generically. */
sealed class TableCommand {
    data class AddRow(val at: Int) : TableCommand()
    data class DeleteRow(val at: Int) : TableCommand()
    data class AddColumn(val at: Int) : TableCommand()
    data class DeleteColumn(val at: Int) : TableCommand()
    data class MergeCells(val row: Int, val column: Int, val columnSpan: Int, val rowSpan: Int) : TableCommand()
    data class SplitCell(val row: Int, val column: Int) : TableCommand()
    data class CellAlignment(val row: Int, val column: Int, val align: TextAlign) : TableCommand()
    data class CellBackground(val row: Int, val column: Int, val color: String?) : TableCommand()
    data class ResizeColumn(val column: Int, val width: Float) : TableCommand()
    data object ToggleHeaderRow : TableCommand()
    data object ToggleHeaderColumn : TableCommand()
}
