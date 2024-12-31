package com.app.pustakam.android.widgets.textField

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.app.pustakam.android.screen.noteEditor.rememberFocusRequester
import com.app.pustakam.data.models.response.notes.NoteContentModel

@Composable
fun NoteTextField(
    noteContentModel: NoteContentModel.TextContent, focusRequester: FocusRequester = rememberFocusRequester(), modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    var textState by remember { mutableStateOf(noteContentModel.text) }
    TextField(value = textState, onValueChange = {
        textState = it
    },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = {
        focusManager.moveFocus(FocusDirection.Down)
    },
        onDone = {
        noteContentModel.copy(text = textState)
        focusManager.clearFocus()
    }), modifier = modifier.fillMaxWidth().focusRequester(focusRequester))
}
