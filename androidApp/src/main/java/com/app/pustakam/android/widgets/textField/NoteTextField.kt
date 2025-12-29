package com.app.pustakam.android.widgets.textField

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.app.pustakam.android.screen.noteEditor.rememberFocusRequester
import com.app.pustakam.android.widgets.dynamicWidgets.TextEditorWidget
import com.app.pustakam.data.models.response.notes.NoteContentModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteTextField(
    noteContentModel: NoteContentModel.TextContent,
    focusRequester: FocusRequester = rememberFocusRequester(),
    modifier: Modifier = Modifier,
    onUpdate : (text : String)-> Unit,
    onSelectionChange : (TextFieldValue)-> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var textState: TextFieldValue by remember { mutableStateOf(TextFieldValue(text = noteContentModel.text)) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var dropdownOffset by remember { mutableStateOf(Offset.Zero) }
    var height  by remember { mutableStateOf(0.dp) }
    var isDropDownVisible by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = textState,
            onValueChange = { newValue ->
                textState = newValue
                if (newValue.selection.length > 0 && textLayoutResult != null) {
                    val selectionEnd =
                        newValue.selection.end.coerceAtLeast(1) - 1
                    val rect = textLayoutResult!!.getBoundingBox(
                        (selectionEnd - 1)
                            .coerceIn(0, textState.text.length - 1)
                    )
                    dropdownOffset = dropdownOffset.copy(
                        x = rect.right,
                        y = rect.bottom
                    )
                    isDropDownVisible = true
                } else {
                    isDropDownVisible = false
                }
                onSelectionChange(textState)
                onUpdate(textState.text)
            },
            onTextLayout = {
                textLayoutResult = it
            },
            modifier = modifier
                .padding(8.dp)
                .wrapContentHeight()
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onSizeChanged { height = with(density){ it.height.toDp() } }
             ,
            keyboardOptions = KeyboardOptions.Default,
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                },
                onDone = {
                    if (!focusManager.moveFocus(FocusDirection.Down)) {
                        focusManager.clearFocus()
                    }
                }
            ),
            decorationBox = { innerTextField ->
                if (textState.text.isEmpty()) {
                    Text(
                        text = "Hi start writing from here...",
                        color = Color.Gray
                    )
                }
                TextFieldDefaults.DecorationBox(
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colorScheme.tertiary
                    ),
                    value = textState.text,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = remember { MutableInteractionSource() },
                    contentPadding = PaddingValues(8.dp)
                )
            }
        )
        DropdownMenu(
            offset = with(density) {
                DpOffset(
                    dropdownOffset.x.toDp(),
                    dropdownOffset.y.toDp() - height
                )
            },
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            containerColor = Color.Transparent,
            onDismissRequest = { isDropDownVisible = false },
            expanded = isDropDownVisible,
            properties = PopupProperties(focusable = false)
        ) {
            TextEditorWidget {
                isDropDownVisible = false
            }
        }
    }
}
