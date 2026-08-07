package com.app.pustakam.android.widgets.smartText

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.app.pustakam.core.richtext.engine.ListEngine
import com.app.pustakam.core.richtext.engine.TableEngine
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.presentation.DocumentSelection
import com.app.pustakam.core.richtext.presentation.SmartTextIntent

// one native editor per block — the reason lists, quotes and headings behave the same on both platforms
@Composable
fun SmartTextBlockRow(
    block: RichBlock,
    isFocused: Boolean,
    listNumber: Int?,
    selection: DocumentSelection,
    searchRanges: List<IntRange>,
    activeSearchRange: IntRange?,
    focusRequester: FocusRequester,
    readOnly: Boolean,
    onIntent: (SmartTextIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    when (block) {
        is RichBlock.Text -> TextBlockRow(
            block = block,
            isFocused = isFocused,
            listNumber = listNumber,
            selection = selection,
            searchRanges = searchRanges,
            activeSearchRange = activeSearchRange,
            focusRequester = focusRequester,
            readOnly = readOnly,
            onIntent = onIntent,
            modifier = modifier
        )

        is RichBlock.Divider -> DividerBlockRow(modifier)

        is RichBlock.Code -> CodeBlockRow(block, focusRequester, readOnly, onIntent, modifier)

        is RichBlock.Table -> TableBlockRow(block, readOnly, onIntent, modifier)
    }
}

@Composable
private fun TextBlockRow(
    block: RichBlock.Text,
    isFocused: Boolean,
    listNumber: Int?,
    selection: DocumentSelection,
    searchRanges: List<IntRange>,
    activeSearchRange: IntRange?,
    focusRequester: FocusRequester,
    readOnly: Boolean,
    onIntent: (SmartTextIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SmartTextTokens.colors
    val textStyle = SmartTextStyleMapper.paragraphTextStyle(block, colors, SmartTextTokens.baseFontSize)
    val indent = SmartTextTokens.indentStep * (block.listLevel + block.indent)
    val isQuote = block.style == ParagraphStyle.QUOTE

    var fieldValue by remember(block.id) {
        mutableStateOf(TextFieldValue(block.text, TextRange(block.text.length)))
    }
    // pull external edits (undo, replace-all, markdown rewrite) back into the field
    if (fieldValue.text != block.text) {
        val caret = if (isFocused) selection.normalizedEnd.coerceIn(0, block.text.length)
        else block.text.length
        fieldValue = TextFieldValue(block.text, TextRange(caret))
    }

    val selectionColors = TextSelectionColors(
        handleColor = colors.selectionHandle,
        backgroundColor = colors.accent.copy(alpha = 0.24f)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indent, bottom = block.paragraphSpacing.dp)
            .then(if (isQuote) Modifier.quoteBar(colors.accent) else Modifier)
            .padding(start = if (isQuote) 14.dp else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        block.list?.let { marker ->
            ListMarkerCell(
                style = marker.style,
                level = marker.level,
                number = listNumber,
                checked = block.checked,
                onToggleChecked = { onIntent(SmartTextIntent.ToggleChecked(block.id)) }
            )
        }

        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    val newlineAt = newValue.text.indexOf('\n')
                    if (newlineAt >= 0) {
                        val flattened = newValue.text.removeRange(newlineAt, newlineAt + 1)
                        onIntent(SmartTextIntent.TypeText(block.id, flattened, newlineAt))
                        onIntent(SmartTextIntent.SplitBlock(block.id, newlineAt))
                        return@BasicTextField
                    }
                    fieldValue = newValue
                    // TypeText already positions the caret; a follow-up SelectionChanged would
                    // undo the caret move a markdown shortcut just made
                    if (newValue.text != block.text) {
                        onIntent(
                            SmartTextIntent.TypeText(block.id, newValue.text, newValue.selection.end)
                        )
                    } else {
                        onIntent(
                            SmartTextIntent.SelectionChanged(
                                DocumentSelection(
                                    startBlockId = block.id,
                                    startOffset = newValue.selection.start,
                                    endBlockId = block.id,
                                    endOffset = newValue.selection.end
                                )
                            )
                        )
                    }
                },
                readOnly = readOnly,
                textStyle = textStyle,
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions.Default,
                visualTransformation = remember(block.spans, searchRanges, activeSearchRange, colors) {
                    VisualTransformation { original ->
                        TransformedText(
                            SmartTextStyleMapper.annotate(
                                text = original.text,
                                spans = block.spans,
                                colors = colors,
                                baseSize = SmartTextTokens.baseFontSize * block.style.relativeSize,
                                highlightRanges = searchRanges,
                                activeHighlight = activeSearchRange
                            ),
                            OffsetMapping.Identity
                        )
                    }
                },
                decorationBox = { inner ->
                    Box {
                        if (block.text.isEmpty()) {
                            Text(
                                text = placeholderFor(block),
                                style = textStyle.copy(color = colors.onSurfaceMuted)
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .defaultMinSize(minHeight = 24.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focus ->
                        if (focus.isFocused) {
                            onIntent(
                                SmartTextIntent.SelectionChanged(
                                    DocumentSelection(
                                        block.id,
                                        fieldValue.selection.start,
                                        block.id,
                                        fieldValue.selection.end
                                    )
                                )
                            )
                        }
                    }
                    // backspace with the caret at offset 0 outdents, then merges upward
                    .onPreviewKeyEvent { event ->
                        val atStart = fieldValue.selection.collapsed && fieldValue.selection.start == 0
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && atStart) {
                            onIntent(SmartTextIntent.MergeWithPrevious(block.id))
                            true
                        } else {
                            false
                        }
                    }
                    .semantics { contentDescription = accessibilityLabel(block, listNumber) }
            )
        }
    }
}

private fun Modifier.quoteBar(color: Color): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        size = Size(3.dp.toPx(), size.height),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
}

@Composable
private fun ListMarkerCell(
    style: ListStyle,
    level: Int,
    number: Int?,
    checked: Boolean,
    onToggleChecked: () -> Unit
) {
    val colors = SmartTextTokens.colors
    Box(
        modifier = Modifier
            .padding(end = 8.dp, top = 3.dp)
            .defaultMinSize(minWidth = 22.dp),
        contentAlignment = Alignment.TopStart
    ) {
        when (style) {
            ListStyle.BULLET -> Text(
                text = ListEngine.bulletGlyph(level),
                style = TextStyle(color = colors.accent, fontSize = SmartTextTokens.baseFontSize)
            )

            ListStyle.NUMBERED -> Text(
                text = "${number ?: 1}.",
                style = TextStyle(
                    color = colors.accent,
                    fontSize = SmartTextTokens.baseFontSize,
                    fontWeight = FontWeight.Medium
                )
            )

            ListStyle.CHECKLIST -> Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        color = if (checked) colors.accent else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (checked) colors.accent else colors.onSurfaceMuted,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable(onClick = onToggleChecked)
                    .semantics { contentDescription = if (checked) "Checked" else "Unchecked" },
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DividerBlockRow(modifier: Modifier = Modifier) {
    val colors = SmartTextTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.divider)
        )
        Spacer(
            Modifier
                .padding(horizontal = 8.dp)
                .size(4.dp)
                .background(colors.accent, CircleShape)
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.divider)
        )
    }
}

@Composable
private fun CodeBlockRow(
    block: RichBlock.Code,
    focusRequester: FocusRequester,
    readOnly: Boolean,
    onIntent: (SmartTextIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SmartTextTokens.colors
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(colors.codeBackground, RoundedCornerShape(10.dp))
    ) {
        BasicTextField(
            value = block.code,
            onValueChange = { onIntent(SmartTextIntent.UpdateCodeBlock(block.id, it)) },
            readOnly = readOnly,
            textStyle = TextStyle(
                color = colors.onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = SmartTextTokens.codeFontSize
            ),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(start = 12.dp, end = 44.dp, top = 12.dp, bottom = 12.dp)
                .focusRequester(focusRequester)
                .semantics { contentDescription = "Code block" }
        )
        IconButton(
            onClick = { clipboard.setText(AnnotatedString(block.code)) },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy code",
                tint = colors.onSurfaceMuted,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun TableBlockRow(
    block: RichBlock.Table,
    readOnly: Boolean,
    onIntent: (SmartTextIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SmartTextTokens.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .horizontalScroll(scrollState)
    ) {
        block.data.rows.forEachIndexed { rowIndex, row ->
            Row {
                row.cells.forEachIndexed { columnIndex, cell ->
                    if (cell.merged) return@forEachIndexed
                    val width = block.data.columnWidths.getOrElse(columnIndex) { 120f } * cell.columnSpan
                    val isHeader = TableEngine.isHeader(block.data, rowIndex, columnIndex)
                    BasicTextField(
                        value = cell.text,
                        onValueChange = {
                            onIntent(
                                SmartTextIntent.UpdateTableCell(block.id, rowIndex, columnIndex, it)
                            )
                        },
                        readOnly = readOnly,
                        textStyle = TextStyle(
                            color = colors.onSurface,
                            fontSize = SmartTextTokens.baseFontSize,
                            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = SmartTextStyleMapper.alignOf(cell.align)
                        ),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier
                            .width(width.dp)
                            .background(
                                cell.backgroundColor?.let(SmartTextStyleMapper::parseColor)
                                    ?: if (isHeader) colors.accentSoft else Color.Transparent
                            )
                            .border(0.5.dp, colors.divider)
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .semantics {
                                contentDescription = "Row ${rowIndex + 1} column ${columnIndex + 1}"
                            }
                    )
                }
            }
        }
    }
}

private fun placeholderFor(block: RichBlock.Text): String = when {
    block.list != null -> "List item"
    block.style == ParagraphStyle.QUOTE -> "Quote"
    block.style.isHeading || block.style == ParagraphStyle.TITLE -> "Heading"
    else -> "Keep your thoughts alive."
}

private fun accessibilityLabel(block: RichBlock.Text, listNumber: Int?): String = buildString {
    append(
        when {
            block.style == ParagraphStyle.QUOTE -> "Quote"
            block.style.isHeading -> "Heading ${block.style.name.last()}"
            block.style == ParagraphStyle.TITLE -> "Title"
            block.style == ParagraphStyle.SUBTITLE -> "Subtitle"
            block.style == ParagraphStyle.CAPTION -> "Caption"
            else -> "Paragraph"
        }
    )
    block.list?.let { marker ->
        append(", ")
        append(
            when (marker.style) {
                ListStyle.BULLET -> "bullet item"
                ListStyle.NUMBERED -> "item ${listNumber ?: 1}"
                ListStyle.CHECKLIST -> if (block.checked) "completed task" else "task"
            }
        )
        if (marker.level > 0) append(", level ${marker.level + 1}")
    }
}
