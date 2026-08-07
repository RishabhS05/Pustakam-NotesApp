package com.app.pustakam.core.richtext.presentation

import com.app.pustakam.core.richtext.model.FormatSet
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.TextAlign
import com.app.pustakam.core.richtext.model.TextFormat

/**
 * The Swift-facing surface of the editor. Kotlin objects and enum entries do not have a stable
 * spelling once exported to Swift, so both platforms build intents through these functions and
 * switch on the string keys below rather than on the enums themselves.
 */
object SmartTextCommands {

    /** The intent a toolbar button dispatches, or null when it opens a sheet instead. */
    fun forToolbar(action: ToolbarAction): SmartTextIntent? = when (action) {
        ToolbarAction.BOLD -> SmartTextIntent.ToggleFormat(TextFormat.BOLD)
        ToolbarAction.ITALIC -> SmartTextIntent.ToggleFormat(TextFormat.ITALIC)
        ToolbarAction.UNDERLINE -> SmartTextIntent.ToggleFormat(TextFormat.UNDERLINE)
        ToolbarAction.STRIKETHROUGH -> SmartTextIntent.ToggleFormat(TextFormat.STRIKETHROUGH)
        ToolbarAction.HIGHLIGHT -> SmartTextIntent.ToggleFormat(TextFormat.HIGHLIGHT)
        ToolbarAction.INLINE_CODE -> SmartTextIntent.ToggleFormat(TextFormat.CODE)
        ToolbarAction.SUPERSCRIPT -> SmartTextIntent.ToggleFormat(TextFormat.SUPERSCRIPT)
        ToolbarAction.SUBSCRIPT -> SmartTextIntent.ToggleFormat(TextFormat.SUBSCRIPT)
        ToolbarAction.BULLET_LIST -> SmartTextIntent.ToggleList(ListStyle.BULLET)
        ToolbarAction.NUMBERED_LIST -> SmartTextIntent.ToggleList(ListStyle.NUMBERED)
        ToolbarAction.CHECKLIST -> SmartTextIntent.ToggleList(ListStyle.CHECKLIST)
        ToolbarAction.INDENT -> SmartTextIntent.Indent
        ToolbarAction.OUTDENT -> SmartTextIntent.Outdent
        ToolbarAction.QUOTE -> SmartTextIntent.ToggleQuote
        ToolbarAction.CODE_BLOCK -> SmartTextIntent.InsertCodeBlock
        ToolbarAction.DIVIDER -> SmartTextIntent.InsertDivider
        ToolbarAction.CLEAR_FORMATTING -> SmartTextIntent.ClearFormatting
        ToolbarAction.UNDO -> SmartTextIntent.Undo
        ToolbarAction.REDO -> SmartTextIntent.Redo
        ToolbarAction.TEXT_STYLE,
        ToolbarAction.TEXT_COLOR,
        ToolbarAction.BACKGROUND_COLOR,
        ToolbarAction.FONT_SIZE,
        ToolbarAction.ALIGN,
        ToolbarAction.TABLE,
        ToolbarAction.LINK,
        ToolbarAction.FIND_REPLACE,
        ToolbarAction.MORE -> null
    }

    /**
     * Which sheet a button opens: 0 = none, otherwise one of the SHEET_ ordinals below.
     * An Int rather than a string or enum because it is the one form whose Swift spelling
     * the Objective-C exporter cannot change.
     */
    fun sheetIndex(action: ToolbarAction): Int = when (action) {
        ToolbarAction.TEXT_STYLE -> SHEET_TEXT_STYLE
        ToolbarAction.TEXT_COLOR -> SHEET_TEXT_COLOR
        ToolbarAction.BACKGROUND_COLOR -> SHEET_BACKGROUND_COLOR
        ToolbarAction.FONT_SIZE -> SHEET_FONT_SIZE
        ToolbarAction.ALIGN -> SHEET_ALIGN
        ToolbarAction.TABLE -> SHEET_TABLE
        ToolbarAction.LINK -> SHEET_LINK
        ToolbarAction.FIND_REPLACE -> SHEET_FIND_REPLACE
        else -> SHEET_NONE
    }

    /** Stable icon key each platform maps to its own icon set. */
    fun iconKey(action: ToolbarAction): String = when (action) {
        ToolbarAction.BOLD -> "bold"
        ToolbarAction.ITALIC -> "italic"
        ToolbarAction.UNDERLINE -> "underline"
        ToolbarAction.STRIKETHROUGH -> "strikethrough"
        ToolbarAction.HIGHLIGHT -> "highlight"
        ToolbarAction.INLINE_CODE -> "inlineCode"
        ToolbarAction.SUPERSCRIPT -> "superscript"
        ToolbarAction.SUBSCRIPT -> "subscript"
        ToolbarAction.TEXT_STYLE -> "textStyle"
        ToolbarAction.TEXT_COLOR -> "textColor"
        ToolbarAction.BACKGROUND_COLOR -> "backgroundColor"
        ToolbarAction.FONT_SIZE -> "fontSize"
        ToolbarAction.ALIGN -> "align"
        ToolbarAction.BULLET_LIST -> "bulletList"
        ToolbarAction.NUMBERED_LIST -> "numberedList"
        ToolbarAction.CHECKLIST -> "checklist"
        ToolbarAction.INDENT -> "indent"
        ToolbarAction.OUTDENT -> "outdent"
        ToolbarAction.QUOTE -> "quote"
        ToolbarAction.CODE_BLOCK -> "codeBlock"
        ToolbarAction.DIVIDER -> "divider"
        ToolbarAction.TABLE -> "table"
        ToolbarAction.LINK -> "link"
        ToolbarAction.CLEAR_FORMATTING -> "clearFormatting"
        ToolbarAction.UNDO -> "undo"
        ToolbarAction.REDO -> "redo"
        ToolbarAction.FIND_REPLACE -> "findReplace"
        ToolbarAction.MORE -> "more"
    }

    fun isMore(action: ToolbarAction): Boolean = action == ToolbarAction.MORE

    fun isLink(action: ToolbarAction): Boolean = action == ToolbarAction.LINK

    /** True for the buttons that render as a filled accent chip when active. */
    fun isPrimaryFormat(action: ToolbarAction): Boolean = when (action) {
        ToolbarAction.BOLD, ToolbarAction.ITALIC, ToolbarAction.UNDERLINE,
        ToolbarAction.STRIKETHROUGH, ToolbarAction.HIGHLIGHT -> true

        else -> false
    }

    fun isEnabled(action: ToolbarAction, toolbar: ToolbarState, canUndo: Boolean, canRedo: Boolean): Boolean =
        when (action) {
            ToolbarAction.UNDO -> canUndo
            ToolbarAction.REDO -> canRedo
            ToolbarAction.INDENT -> toolbar.canIndent
            ToolbarAction.OUTDENT -> toolbar.canOutdent
            else -> true
        }

    fun typeText(blockId: String, text: String, caret: Int): SmartTextIntent =
        SmartTextIntent.TypeText(blockId, text, caret)

    fun selectionChanged(blockId: String, start: Int, end: Int): SmartTextIntent =
        SmartTextIntent.SelectionChanged(DocumentSelection(blockId, start, blockId, end))

    fun splitBlock(blockId: String, caret: Int): SmartTextIntent =
        SmartTextIntent.SplitBlock(blockId, caret)

    fun mergeWithPrevious(blockId: String): SmartTextIntent =
        SmartTextIntent.MergeWithPrevious(blockId)

    fun toggleChecked(blockId: String): SmartTextIntent = SmartTextIntent.ToggleChecked(blockId)

    fun updateCodeBlock(blockId: String, code: String): SmartTextIntent =
        SmartTextIntent.UpdateCodeBlock(blockId, code)

    fun updateTableCell(blockId: String, row: Int, column: Int, text: String): SmartTextIntent =
        SmartTextIntent.UpdateTableCell(blockId, row, column, text)

    fun tableAction(blockId: String, command: TableCommand): SmartTextIntent =
        SmartTextIntent.TableAction(blockId, command)

    fun insertTable(rows: Int, columns: Int): SmartTextIntent =
        SmartTextIntent.InsertTable(rows, columns)

    fun setParagraphStyle(style: ParagraphStyle): SmartTextIntent =
        SmartTextIntent.SetParagraphStyle(style)

    fun setAlignment(align: TextAlign): SmartTextIntent = SmartTextIntent.SetAlignment(align)

    fun setTextColor(color: String?): SmartTextIntent = SmartTextIntent.SetTextColor(color)

    fun setBackgroundColor(color: String?): SmartTextIntent =
        SmartTextIntent.SetBackgroundColor(color)

    fun setFontSize(size: Float?): SmartTextIntent = SmartTextIntent.SetFontSize(size)

    fun setLink(url: String?): SmartTextIntent =
        if (url == null) SmartTextIntent.RemoveLink else SmartTextIntent.SetLink(url)

    fun setToolbarExpanded(expanded: Boolean): SmartTextIntent =
        SmartTextIntent.SetToolbarExpanded(expanded)

    fun setSearchQuery(query: String, matchCase: Boolean): SmartTextIntent =
        SmartTextIntent.SetSearchQuery(query, matchCase)

    fun setReplacement(replacement: String): SmartTextIntent =
        SmartTextIntent.SetReplacement(replacement)

    fun setSearchActive(active: Boolean): SmartTextIntent = SmartTextIntent.SetSearchActive(active)

    fun findNext(): SmartTextIntent = SmartTextIntent.FindNext

    fun findPrevious(): SmartTextIntent = SmartTextIntent.FindPrevious

    fun replaceCurrent(): SmartTextIntent = SmartTextIntent.ReplaceCurrent

    fun replaceAll(): SmartTextIntent = SmartTextIntent.ReplaceAll

    fun copy(): SmartTextIntent = SmartTextIntent.Copy

    fun cut(): SmartTextIntent = SmartTextIntent.Cut

    fun paste(text: String, keepFormatting: Boolean): SmartTextIntent =
        SmartTextIntent.Paste(text, keepFormatting)

    fun selectAll(): SmartTextIntent = SmartTextIntent.SelectAll

    fun duplicateSelection(): SmartTextIntent = SmartTextIntent.DuplicateSelection

    fun selectWord(blockId: String, offset: Int): SmartTextIntent =
        SmartTextIntent.SelectWord(blockId, offset)

    fun selectParagraph(blockId: String): SmartTextIntent = SmartTextIntent.SelectParagraph(blockId)

    fun deleteFocusedBlock(): SmartTextIntent = SmartTextIntent.DeleteFocusedBlock

    fun tableAddRow(at: Int): TableCommand = TableCommand.AddRow(at)

    fun tableDeleteRow(at: Int): TableCommand = TableCommand.DeleteRow(at)

    fun tableAddColumn(at: Int): TableCommand = TableCommand.AddColumn(at)

    fun tableDeleteColumn(at: Int): TableCommand = TableCommand.DeleteColumn(at)

    fun tableMergeCells(row: Int, column: Int, columnSpan: Int, rowSpan: Int): TableCommand =
        TableCommand.MergeCells(row, column, columnSpan, rowSpan)

    fun tableSplitCell(row: Int, column: Int): TableCommand = TableCommand.SplitCell(row, column)

    fun tableCellAlignment(row: Int, column: Int, align: TextAlign): TableCommand =
        TableCommand.CellAlignment(row, column, align)

    fun tableCellBackground(row: Int, column: Int, color: String?): TableCommand =
        TableCommand.CellBackground(row, column, color)

    fun tableResizeColumn(column: Int, width: Float): TableCommand =
        TableCommand.ResizeColumn(column, width)

    fun tableToggleHeaderRow(): TableCommand = TableCommand.ToggleHeaderRow

    fun tableToggleHeaderColumn(): TableCommand = TableCommand.ToggleHeaderColumn

    const val SHEET_NONE = 0
    const val SHEET_TEXT_STYLE = 1
    const val SHEET_TEXT_COLOR = 2
    const val SHEET_BACKGROUND_COLOR = 3
    const val SHEET_FONT_SIZE = 4
    const val SHEET_ALIGN = 5
    const val SHEET_TABLE = 6
    const val SHEET_LINK = 7
    const val SHEET_FIND_REPLACE = 8
}

/** Ordered option lists with their labels, so both pickers show identical wording. */
object SmartTextCatalog {

    val paragraphStyles: List<ParagraphStyle> = ParagraphStyle.all

    val alignments: List<TextAlign> = listOf(
        TextAlign.START, TextAlign.CENTER, TextAlign.END, TextAlign.JUSTIFY
    )

    val listStyles: List<ListStyle> = listOf(
        ListStyle.BULLET, ListStyle.NUMBERED, ListStyle.CHECKLIST
    )

    val textColors: List<String> = listOf(
        "#E9A33C", "#D9662F", "#C0392B", "#8E5AA8", "#3D6FB4",
        "#2E8B72", "#7A8B2E", "#8A6A4B", "#6B6259", "#2B2723"
    )

    val highlightColors: List<String> = listOf(
        "#FBE6A0", "#FBD0A0", "#F8B4B4", "#E3C9F5", "#BFD9F5",
        "#B6E3D4", "#DCE8AE", "#E8DCC8", "#DDD6CC", "#FFFFFF"
    )

    fun paragraphLabel(style: ParagraphStyle): String = when (style) {
        ParagraphStyle.PARAGRAPH -> "Paragraph"
        ParagraphStyle.TITLE -> "Title"
        ParagraphStyle.SUBTITLE -> "Subtitle"
        ParagraphStyle.HEADING_1 -> "Heading 1"
        ParagraphStyle.HEADING_2 -> "Heading 2"
        ParagraphStyle.HEADING_3 -> "Heading 3"
        ParagraphStyle.HEADING_4 -> "Heading 4"
        ParagraphStyle.HEADING_5 -> "Heading 5"
        ParagraphStyle.HEADING_6 -> "Heading 6"
        ParagraphStyle.CAPTION -> "Caption"
        ParagraphStyle.QUOTE -> "Quote"
    }

    fun alignLabel(align: TextAlign): String = when (align) {
        TextAlign.START -> "Left"
        TextAlign.CENTER -> "Centre"
        TextAlign.END -> "Right"
        TextAlign.JUSTIFY -> "Justify"
    }

    fun listLabel(style: ListStyle): String = when (style) {
        ListStyle.BULLET -> "Bullet list"
        ListStyle.NUMBERED -> "Numbered list"
        ListStyle.CHECKLIST -> "Checklist"
    }

    /** Presets offered by the table sheet, as flat pairs Swift can index. */
    val tablePresetRows: List<Int> = listOf(2, 3, 4)

    val tablePresetColumns: List<Int> = listOf(2, 3, 3)

    fun styleKey(style: ParagraphStyle): String = style.name

    fun alignKey(align: TextAlign): String = align.name

    fun listKey(style: ListStyle): String = style.name

    fun isQuote(style: ParagraphStyle): Boolean = style == ParagraphStyle.QUOTE

    fun isCaption(style: ParagraphStyle): Boolean = style == ParagraphStyle.CAPTION

    fun isTitleLike(style: ParagraphStyle): Boolean =
        style == ParagraphStyle.TITLE || style == ParagraphStyle.SUBTITLE || style.isHeading
}

/** Format predicates, so Swift never has to spell a [TextFormat] entry. */
object SmartTextFormats {

    fun isBold(formats: FormatSet): Boolean =
        formats.has(TextFormat.BOLD)

    fun isItalic(formats: FormatSet): Boolean =
        formats.has(TextFormat.ITALIC)

    fun isUnderline(formats: FormatSet): Boolean =
        formats.has(TextFormat.UNDERLINE)

    fun isStrikethrough(formats: FormatSet): Boolean =
        formats.has(TextFormat.STRIKETHROUGH)

    fun isHighlight(formats: FormatSet): Boolean =
        formats.has(TextFormat.HIGHLIGHT)

    fun isCode(formats: FormatSet): Boolean =
        formats.has(TextFormat.CODE)

    fun isSuperscript(formats: FormatSet): Boolean =
        formats.has(TextFormat.SUPERSCRIPT)

    fun isSubscript(formats: FormatSet): Boolean =
        formats.has(TextFormat.SUBSCRIPT)
}
