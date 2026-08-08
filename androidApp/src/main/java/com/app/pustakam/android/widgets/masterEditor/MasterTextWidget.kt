package com.app.pustakam.android.widgets.masterEditor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.widgets.smartText.SmartTextTokens
import com.app.pustakam.core.richtext.master.presentation.MasterTextCommands
import com.app.pustakam.core.richtext.master.presentation.MasterTextIntent
import com.app.pustakam.core.richtext.master.presentation.MasterTextState

@Composable
fun MasterTextWidget(
    state: MasterTextState,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    readOnly: Boolean = false,
    scale: Float = 1f,
    placeholder: String = "Keep your thoughts alive.",
    onIntent: (MasterTextIntent) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val colors = SmartTextTokens.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val baseSize: TextUnit = SmartTextTokens.baseFontSize * scale

    var fieldValue by remember { mutableStateOf(TextFieldValue(state.text)) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    if (fieldValue.text != state.text) {
        fieldValue = TextFieldValue(
            text = state.text,
            selection = TextRange(
                state.selection.normalizedStart.coerceIn(0, state.text.length),
                state.selection.normalizedEnd.coerceIn(0, state.text.length)
            )
        )
    }

    val rendered = remember(state.document, state.text, colors, baseSize) {
        MasterTextRenderer.annotate(state, colors, baseSize)
    }

    val selectionColors = TextSelectionColors(
        handleColor = colors.selectionHandle,
        backgroundColor = colors.accent.copy(alpha = 0.24f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (state.text.isEmpty()) {
            Text(
                text = placeholder,
                style = TextStyle(color = colors.onSurfaceMuted, fontSize = baseSize),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    fieldValue = newValue
                    if (newValue.text != state.text) {
                        onIntent(
                            MasterTextCommands.edit(
                                text = newValue.text,
                                selectionStart = newValue.selection.start,
                                selectionEnd = newValue.selection.end
                            )
                        )
                    } else {
                        onIntent(
                            MasterTextCommands.selectionChanged(
                                start = newValue.selection.start,
                                end = newValue.selection.end
                            )
                        )
                    }
                },
                readOnly = readOnly,
                textStyle = TextStyle(color = colors.onSurface, fontSize = baseSize),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions.Default,
                onTextLayout = { layout = it },
                visualTransformation = remember(rendered) {
                    VisualTransformation { TransformedText(rendered, OffsetMapping.Identity) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) }
                    .drawBehind {
                        layout?.let { result ->
                            MasterTextRenderer.drawMarkers(
                                scope = this,
                                state = state,
                                layout = result,
                                measurer = measurer,
                                colors = colors,
                                baseSize = baseSize,
                                density = density
                            )
                        }
                    }
                    .pointerInput(state.document, readOnly) {
                        detectTapGestures(
                            onDoubleTap = { position ->
                                layout?.let { result ->
                                    onIntent(
                                        MasterTextCommands.selectWord(
                                            result.getOffsetForPosition(position)
                                        )
                                    )
                                }
                            },
                            onTap = { position ->
                                val result = layout ?: return@detectTapGestures
                                MasterTextRenderer
                                    .markerHitOffset(state, result, position, density)
                                    ?.let { onIntent(MasterTextCommands.toggleChecked(it)) }
                            },
                            onLongPress = { position ->
                                layout?.let { result ->
                                    onIntent(
                                        MasterTextCommands.selectParagraph(
                                            result.getOffsetForPosition(position)
                                        )
                                    )
                                }
                            }
                        )
                    }
            )
        }
    }
}
