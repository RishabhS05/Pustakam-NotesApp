package com.app.pustakam.android.widgets.colorPalete

import com.app.pustakam.android.theme.DarkBrown1
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.toHexString
import com.app.pustakam.android.widgets.PrimaryFilledButton
import com.app.pustakam.core.model.models.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTagBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSubmit: (Tag) -> Unit
) {
    var selectedColor by remember { mutableStateOf(DarkBrown1) }
    val focusManager = LocalFocusManager.current
    var textState: String by rememberSaveable { mutableStateOf("") }
    var isColorPicked by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column {
            TextField(
                value = textState,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        isColorPicked = !isColorPicked
                    }) {
                        Icon(
                            Icons.Filled.ColorLens, contentDescription = "Select Color",
                            tint = selectedColor
                        )
                    }
                },
                onValueChange = {
                    textState = it
                },
                placeholder = {
                    Text(
                        "Tag?", textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                }),
                colors = TextFieldDefaults.colors(
                    cursorColor = colorScheme.secondary
                ),
            )
            if (isColorPicked)
                ColorSelector(initial = selectedColor, onColorChanged = {
                    selectedColor = it
                })
            PrimaryFilledButton(
                label = "Submit", modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp).padding(bottom = 12.dp),
            ) {
                onSubmit(Tag(color = selectedColor.toHexString(), label = textState))
                onDismissRequest()
            }
        }
    }
}