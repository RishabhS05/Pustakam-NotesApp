package com.app.pustakam.core.richtext.master.presentation

import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.TextAlign
import com.app.pustakam.core.richtext.model.TextFormat

sealed class MasterTextIntent {

    data class Edit(val text: String, val selectionStart: Int, val selectionEnd: Int) :
        MasterTextIntent()

    data class SelectionChanged(val start: Int, val end: Int) : MasterTextIntent()

    data object SelectAll : MasterTextIntent()

    data class SelectWord(val offset: Int) : MasterTextIntent()

    data class SelectParagraph(val offset: Int) : MasterTextIntent()

    data class ToggleFormat(val format: TextFormat) : MasterTextIntent()

    data class SetParagraphStyle(val style: ParagraphStyle) : MasterTextIntent()

    data class SetAlignment(val align: TextAlign) : MasterTextIntent()

    data class ToggleList(val style: ListStyle) : MasterTextIntent()

    data class ToggleChecked(val offset: Int) : MasterTextIntent()

    data object Indent : MasterTextIntent()

    data object Outdent : MasterTextIntent()

    data object ToggleQuote : MasterTextIntent()

    data class SetTextColor(val color: String?) : MasterTextIntent()

    data class SetBackgroundColor(val color: String?) : MasterTextIntent()

    data class SetFontSize(val size: Float?) : MasterTextIntent()

    data class SetFontWeight(val weight: Int?) : MasterTextIntent()

    data class SetLink(val url: String?) : MasterTextIntent()

    data object ClearFormatting : MasterTextIntent()

    data object Undo : MasterTextIntent()

    data object Redo : MasterTextIntent()

    data class SetToolbarExpanded(val expanded: Boolean) : MasterTextIntent()

    data object DismissToolbar : MasterTextIntent()
}
