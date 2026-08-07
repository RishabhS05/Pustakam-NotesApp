package com.app.pustakam.android.widgets.smartText

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Subscript
import androidx.compose.material.icons.filled.Superscript
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.app.pustakam.core.richtext.presentation.SmartTextCommands
import com.app.pustakam.core.richtext.presentation.ToolbarAction
import com.app.pustakam.core.richtext.presentation.ToolbarSpec
import com.app.pustakam.core.richtext.presentation.ToolbarState

// floating bar over a selection — primary row always, secondary revealed by MORE
@Composable
fun SmartTextSelectionToolbar(
    toolbar: ToolbarState,
    expanded: Boolean,
    onAction: (ToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SmartTextTokens.colors
    Surface(
        modifier = modifier,
        color = colors.toolbar,
        shape = RoundedCornerShape(SmartTextTokens.toolbarCorner),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToolbarSpec.selectionPrimary.forEach { action ->
                    SmartTextToolbarButton(action, ToolbarSpec.isActive(action, toolbar), true) {
                        onAction(action)
                    }
                }
                ToolbarSeparator()
                SmartTextToolbarButton(ToolbarAction.MORE, expanded, true) {
                    onAction(ToolbarAction.MORE)
                }
                SmartTextToolbarButton(ToolbarAction.DISMISS, false, true) {
                    onAction(ToolbarAction.DISMISS)
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolbarSpec.selectionSecondary
                        .filterNot { it == ToolbarAction.MORE }
                        .forEach { action ->
                            SmartTextToolbarButton(
                                action,
                                ToolbarSpec.isActive(action, toolbar),
                                true
                            ) { onAction(action) }
                        }
                }
            }
        }
    }
}

// pinned above the keyboard — horizontally scrollable, with an expandable tray
@Composable
fun SmartTextKeyboardToolbar(
    toolbar: ToolbarState,
    expanded: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onAction: (ToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SmartTextTokens.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.toolbar,
        shape = RoundedCornerShape(
            topStart = SmartTextTokens.toolbarCorner,
            topEnd = SmartTextTokens.toolbarCorner,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ),
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolbarSpec.keyboardExpanded.forEach { action ->
                        SmartTextToolbarButton(
                            action = action,
                            active = ToolbarSpec.isActive(action, toolbar),
                            enabled = enabledFor(action, toolbar, canUndo, canRedo)
                        ) { onAction(action) }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                ToolbarSpec.keyboardPrimary.forEach { action ->
                    if (action == ToolbarAction.UNDO) ToolbarSeparator()
                    SmartTextToolbarButton(
                        action = action,
                        active = ToolbarSpec.isActive(action, toolbar),
                        enabled = enabledFor(action, toolbar, canUndo, canRedo)
                    ) { onAction(action) }
                }
                ToolbarSeparator()
                SmartTextToolbarButton(ToolbarAction.MORE, expanded, true) {
                    onAction(ToolbarAction.MORE)
                }
                SmartTextToolbarButton(ToolbarAction.DISMISS, false, true) {
                    onAction(ToolbarAction.DISMISS)
                }
            }
        }
    }
}

@Composable
fun SmartTextToolbarButton(
    action: ToolbarAction,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = SmartTextTokens.colors
    val background = when {
        active && isPrimaryFormat(action) -> colors.accent
        active -> colors.accentSoft
        else -> Color.Transparent
    }
    val tint = when {
        active && isPrimaryFormat(action) -> colors.onAccent
        active -> colors.accent
        enabled -> colors.onSurface
        else -> colors.onSurfaceMuted.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(SmartTextTokens.buttonSize)
            .background(background, RoundedCornerShape(SmartTextTokens.buttonCorner))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = ToolbarSpec.label(action) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconFor(action),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun ToolbarSeparator() {
    Spacer(
        Modifier
            .padding(horizontal = 5.dp)
            .width(1.dp)
            .height(22.dp)
            .background(SmartTextTokens.colors.divider)
    )
}

private fun isPrimaryFormat(action: ToolbarAction): Boolean =
    SmartTextCommands.isPrimaryFormat(action)

private fun enabledFor(
    action: ToolbarAction,
    toolbar: ToolbarState,
    canUndo: Boolean,
    canRedo: Boolean
): Boolean = SmartTextCommands.isEnabled(action, toolbar, canUndo, canRedo)

fun iconFor(action: ToolbarAction): ImageVector = when (action) {
    ToolbarAction.BOLD -> Icons.Default.FormatBold
    ToolbarAction.ITALIC -> Icons.Default.FormatItalic
    ToolbarAction.UNDERLINE -> Icons.Default.FormatUnderlined
    ToolbarAction.STRIKETHROUGH -> Icons.Default.FormatStrikethrough
    ToolbarAction.HIGHLIGHT -> Icons.Default.BorderColor
    ToolbarAction.INLINE_CODE -> Icons.Default.Code
    ToolbarAction.SUPERSCRIPT -> Icons.Default.Superscript
    ToolbarAction.SUBSCRIPT -> Icons.Default.Subscript
    ToolbarAction.TEXT_STYLE -> Icons.Default.TextFields
    ToolbarAction.TEXT_COLOR -> Icons.Default.FormatColorText
    ToolbarAction.BACKGROUND_COLOR -> Icons.Default.FormatColorFill
    ToolbarAction.FONT_SIZE -> Icons.Default.FormatSize
    ToolbarAction.ALIGN -> Icons.AutoMirrored.Filled.FormatAlignLeft
    ToolbarAction.BULLET_LIST -> Icons.AutoMirrored.Filled.FormatListBulleted
    ToolbarAction.NUMBERED_LIST -> Icons.Default.FormatListNumbered
    ToolbarAction.CHECKLIST -> Icons.Default.Checklist
    ToolbarAction.INDENT -> Icons.AutoMirrored.Filled.FormatIndentIncrease
    ToolbarAction.OUTDENT -> Icons.AutoMirrored.Filled.FormatIndentDecrease
    ToolbarAction.QUOTE -> Icons.Default.FormatQuote
    ToolbarAction.CODE_BLOCK -> Icons.Default.Terminal
    ToolbarAction.DIVIDER -> Icons.Default.HorizontalRule
    ToolbarAction.TABLE -> Icons.Default.TableChart
    ToolbarAction.LINK -> Icons.Default.Link
    ToolbarAction.CLEAR_FORMATTING -> Icons.Default.FormatClear
    ToolbarAction.UNDO -> Icons.AutoMirrored.Filled.Undo
    ToolbarAction.REDO -> Icons.AutoMirrored.Filled.Redo
    ToolbarAction.FIND_REPLACE -> Icons.Default.FindReplace
    ToolbarAction.MORE -> Icons.Default.MoreHoriz
    ToolbarAction.DISMISS -> Icons.Default.KeyboardArrowDown
}
