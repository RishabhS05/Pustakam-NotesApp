package com.app.pustakam.core.richtext.presentation

import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.TextFormat

/**
 * Which buttons exist and in what order. Shared so the Compose toolbar and the SwiftUI toolbar
 * cannot drift; each platform only maps an id to its own icon.
 */
enum class ToolbarAction {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    HIGHLIGHT,
    INLINE_CODE,
    SUPERSCRIPT,
    SUBSCRIPT,
    TEXT_STYLE,
    TEXT_COLOR,
    BACKGROUND_COLOR,
    FONT_SIZE,
    ALIGN,
    BULLET_LIST,
    NUMBERED_LIST,
    CHECKLIST,
    INDENT,
    OUTDENT,
    QUOTE,
    CODE_BLOCK,
    DIVIDER,
    TABLE,
    LINK,
    CLEAR_FORMATTING,
    UNDO,
    REDO,
    FIND_REPLACE,
    MORE
}

object ToolbarSpec {

    /** Floating toolbar shown over a selection — primary row. */
    val selectionPrimary: List<ToolbarAction> = listOf(
        ToolbarAction.BOLD,
        ToolbarAction.ITALIC,
        ToolbarAction.UNDERLINE,
        ToolbarAction.HIGHLIGHT
    )

    /** Floating toolbar — revealed by MORE. */
    val selectionSecondary: List<ToolbarAction> = listOf(
        ToolbarAction.TEXT_STYLE,
        ToolbarAction.TEXT_COLOR,
        ToolbarAction.BACKGROUND_COLOR,
        ToolbarAction.ALIGN,
        ToolbarAction.BULLET_LIST,
        ToolbarAction.NUMBERED_LIST,
        ToolbarAction.LINK,
        ToolbarAction.INLINE_CODE,
        ToolbarAction.QUOTE,
        ToolbarAction.MORE
    )

    /** Toolbar pinned above the keyboard — always visible. */
    val keyboardPrimary: List<ToolbarAction> = listOf(
        ToolbarAction.BOLD,
        ToolbarAction.ITALIC,
        ToolbarAction.UNDERLINE,
        ToolbarAction.TEXT_STYLE,
        ToolbarAction.BULLET_LIST,
        ToolbarAction.NUMBERED_LIST,
        ToolbarAction.UNDO,
        ToolbarAction.REDO
    )

    /** Toolbar above the keyboard — the expandable tray. */
    val keyboardExpanded: List<ToolbarAction> = listOf(
        ToolbarAction.HIGHLIGHT,
        ToolbarAction.STRIKETHROUGH,
        ToolbarAction.TEXT_COLOR,
        ToolbarAction.BACKGROUND_COLOR,
        ToolbarAction.FONT_SIZE,
        ToolbarAction.ALIGN,
        ToolbarAction.CHECKLIST,
        ToolbarAction.INDENT,
        ToolbarAction.OUTDENT,
        ToolbarAction.QUOTE,
        ToolbarAction.INLINE_CODE,
        ToolbarAction.CODE_BLOCK,
        ToolbarAction.SUPERSCRIPT,
        ToolbarAction.SUBSCRIPT,
        ToolbarAction.DIVIDER,
        ToolbarAction.TABLE,
        ToolbarAction.LINK,
        ToolbarAction.FIND_REPLACE,
        ToolbarAction.CLEAR_FORMATTING
    )

    /** Accessibility label, so VoiceOver and TalkBack read the same words. */
    fun label(action: ToolbarAction): String = when (action) {
        ToolbarAction.BOLD -> "Bold"
        ToolbarAction.ITALIC -> "Italic"
        ToolbarAction.UNDERLINE -> "Underline"
        ToolbarAction.STRIKETHROUGH -> "Strikethrough"
        ToolbarAction.HIGHLIGHT -> "Highlight"
        ToolbarAction.INLINE_CODE -> "Inline code"
        ToolbarAction.SUPERSCRIPT -> "Superscript"
        ToolbarAction.SUBSCRIPT -> "Subscript"
        ToolbarAction.TEXT_STYLE -> "Text style"
        ToolbarAction.TEXT_COLOR -> "Text colour"
        ToolbarAction.BACKGROUND_COLOR -> "Background colour"
        ToolbarAction.FONT_SIZE -> "Font size"
        ToolbarAction.ALIGN -> "Alignment"
        ToolbarAction.BULLET_LIST -> "Bullet list"
        ToolbarAction.NUMBERED_LIST -> "Numbered list"
        ToolbarAction.CHECKLIST -> "Checklist"
        ToolbarAction.INDENT -> "Increase indent"
        ToolbarAction.OUTDENT -> "Decrease indent"
        ToolbarAction.QUOTE -> "Quote"
        ToolbarAction.CODE_BLOCK -> "Code block"
        ToolbarAction.DIVIDER -> "Divider"
        ToolbarAction.TABLE -> "Table"
        ToolbarAction.LINK -> "Link"
        ToolbarAction.CLEAR_FORMATTING -> "Clear formatting"
        ToolbarAction.UNDO -> "Undo"
        ToolbarAction.REDO -> "Redo"
        ToolbarAction.FIND_REPLACE -> "Find and replace"
        ToolbarAction.MORE -> "More"
    }

    /** Whether a button should render in its active state. */
    fun isActive(action: ToolbarAction, toolbar: ToolbarState): Boolean = when (action) {
        ToolbarAction.BOLD -> toolbar.activeFormats.has(TextFormat.BOLD)
        ToolbarAction.ITALIC -> toolbar.activeFormats.has(TextFormat.ITALIC)
        ToolbarAction.UNDERLINE -> toolbar.activeFormats.has(TextFormat.UNDERLINE)
        ToolbarAction.STRIKETHROUGH -> toolbar.activeFormats.has(TextFormat.STRIKETHROUGH)
        ToolbarAction.HIGHLIGHT -> toolbar.activeFormats.has(TextFormat.HIGHLIGHT)
        ToolbarAction.INLINE_CODE -> toolbar.activeFormats.has(TextFormat.CODE)
        ToolbarAction.SUPERSCRIPT -> toolbar.activeFormats.has(TextFormat.SUPERSCRIPT)
        ToolbarAction.SUBSCRIPT -> toolbar.activeFormats.has(TextFormat.SUBSCRIPT)
        ToolbarAction.BULLET_LIST -> toolbar.listStyle == ListStyle.BULLET
        ToolbarAction.NUMBERED_LIST -> toolbar.listStyle == ListStyle.NUMBERED
        ToolbarAction.CHECKLIST -> toolbar.listStyle == ListStyle.CHECKLIST
        ToolbarAction.QUOTE -> toolbar.isQuote
        ToolbarAction.CODE_BLOCK -> toolbar.isCodeBlock
        ToolbarAction.LINK -> toolbar.link != null
        else -> false
    }
}
