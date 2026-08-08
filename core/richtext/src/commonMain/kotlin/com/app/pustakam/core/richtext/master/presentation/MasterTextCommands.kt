package com.app.pustakam.core.richtext.master.presentation

import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.TextAlign
import com.app.pustakam.core.richtext.model.TextFormat
import com.app.pustakam.core.richtext.presentation.SmartTextCommands
import com.app.pustakam.core.richtext.presentation.ToolbarAction
import com.app.pustakam.core.richtext.presentation.ToolbarState

object MasterTextCommands {

    fun stateOf(document: RichDocument): MasterTextState = MasterTextState.of(document)

    fun edit(text: String, selectionStart: Int, selectionEnd: Int): MasterTextIntent =
        MasterTextIntent.Edit(text, selectionStart, selectionEnd)

    fun selectionChanged(start: Int, end: Int): MasterTextIntent =
        MasterTextIntent.SelectionChanged(start, end)

    fun selectAll(): MasterTextIntent = MasterTextIntent.SelectAll

    fun selectWord(offset: Int): MasterTextIntent = MasterTextIntent.SelectWord(offset)

    fun selectParagraph(offset: Int): MasterTextIntent = MasterTextIntent.SelectParagraph(offset)

    fun toggleChecked(offset: Int): MasterTextIntent = MasterTextIntent.ToggleChecked(offset)

    fun setParagraphStyle(style: ParagraphStyle): MasterTextIntent =
        MasterTextIntent.SetParagraphStyle(style)

    fun setAlignment(align: TextAlign): MasterTextIntent = MasterTextIntent.SetAlignment(align)

    fun setTextColor(color: String?): MasterTextIntent = MasterTextIntent.SetTextColor(color)

    fun setBackgroundColor(color: String?): MasterTextIntent =
        MasterTextIntent.SetBackgroundColor(color)

    fun setFontSize(size: Float?): MasterTextIntent = MasterTextIntent.SetFontSize(size)

    fun setLink(url: String?): MasterTextIntent = MasterTextIntent.SetLink(url)

    fun undo(): MasterTextIntent = MasterTextIntent.Undo

    fun redo(): MasterTextIntent = MasterTextIntent.Redo

    fun setToolbarExpanded(expanded: Boolean): MasterTextIntent =
        MasterTextIntent.SetToolbarExpanded(expanded)

    fun dismissToolbar(): MasterTextIntent = MasterTextIntent.DismissToolbar

    fun forToolbar(action: ToolbarAction): MasterTextIntent? = when (action) {
        ToolbarAction.BOLD -> MasterTextIntent.ToggleFormat(TextFormat.BOLD)
        ToolbarAction.ITALIC -> MasterTextIntent.ToggleFormat(TextFormat.ITALIC)
        ToolbarAction.UNDERLINE -> MasterTextIntent.ToggleFormat(TextFormat.UNDERLINE)
        ToolbarAction.STRIKETHROUGH -> MasterTextIntent.ToggleFormat(TextFormat.STRIKETHROUGH)
        ToolbarAction.HIGHLIGHT -> MasterTextIntent.ToggleFormat(TextFormat.HIGHLIGHT)
        ToolbarAction.INLINE_CODE -> MasterTextIntent.ToggleFormat(TextFormat.CODE)
        ToolbarAction.SUPERSCRIPT -> MasterTextIntent.ToggleFormat(TextFormat.SUPERSCRIPT)
        ToolbarAction.SUBSCRIPT -> MasterTextIntent.ToggleFormat(TextFormat.SUBSCRIPT)
        ToolbarAction.BULLET_LIST -> MasterTextIntent.ToggleList(ListStyle.BULLET)
        ToolbarAction.NUMBERED_LIST -> MasterTextIntent.ToggleList(ListStyle.NUMBERED)
        ToolbarAction.CHECKLIST -> MasterTextIntent.ToggleList(ListStyle.CHECKLIST)
        ToolbarAction.INDENT -> MasterTextIntent.Indent
        ToolbarAction.OUTDENT -> MasterTextIntent.Outdent
        ToolbarAction.QUOTE -> MasterTextIntent.ToggleQuote
        ToolbarAction.CLEAR_FORMATTING -> MasterTextIntent.ClearFormatting
        ToolbarAction.UNDO -> MasterTextIntent.Undo
        ToolbarAction.REDO -> MasterTextIntent.Redo
        else -> null
    }

    fun sheetIndex(action: ToolbarAction): Int = SmartTextCommands.sheetIndex(action)

    fun isMore(action: ToolbarAction): Boolean = SmartTextCommands.isMore(action)

    fun isDismiss(action: ToolbarAction): Boolean = SmartTextCommands.isDismiss(action)

    fun isLink(action: ToolbarAction): Boolean = SmartTextCommands.isLink(action)

    fun isPrimaryFormat(action: ToolbarAction): Boolean = SmartTextCommands.isPrimaryFormat(action)

    fun iconKey(action: ToolbarAction): String = SmartTextCommands.iconKey(action)

    fun isEnabled(
        action: ToolbarAction,
        toolbar: ToolbarState,
        canUndo: Boolean,
        canRedo: Boolean
    ): Boolean = SmartTextCommands.isEnabled(action, toolbar, canUndo, canRedo)
}
