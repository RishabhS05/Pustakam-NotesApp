package com.app.pustakam.android.widgets.textField

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.app.pustakam.android.screen.noteEditor.rememberFocusRequester
import com.app.pustakam.data.models.response.notes.NoteContentModel

@Composable
fun NoteTextField(
    noteContentModel: NoteContentModel.TextContent,
    focusRequester: FocusRequester = rememberFocusRequester(),
    modifier: Modifier = Modifier,
    onUpdate : (text : String)-> Unit,
) {
    val focusManager = LocalFocusManager.current
    var textState: String by remember { mutableStateOf(noteContentModel.text) }
    TextField(value = textState,
        onValueChange = {
        textState = it
            onUpdate(it)
    },
        placeholder = { Text("Hi start writing from here...") },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = colorScheme.tertiary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onNext = {
        focusManager.moveFocus(FocusDirection.Down)
    },
        onDone = {
            if(!focusManager.moveFocus(FocusDirection.Down)){
                focusManager.clearFocus()
            }
    }), modifier = modifier.wrapContentHeight().fillMaxWidth().focusRequester(focusRequester))
}
