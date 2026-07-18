package com.app.pustakam.android.widgets.importsheet

// 🔧 18-Jul-2026: NEW FEATURE (file import) — bottom sheet with the two import paths
//   (device multi-pick / any link) + the link-entry dialog. Pure UI, callbacks out.
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportFilesSheet(
    onDismiss: () -> Unit,
    onPickFromDevice: () -> Unit,
    onImportFromLink: (String) -> Unit,
) {
    var showLinkDialog by remember { mutableStateOf(false) }
    if (!showLinkDialog) {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
            ListItem(
                headlineContent = { Text("Import files from device") },
                supportingContent = { Text("Pick one or more files of any type") },
                leadingContent = { Icon(Icons.Filled.FileOpen, null, tint = colorScheme.primary) },
                modifier = Modifier.clickable { onDismiss(); onPickFromDevice() }
            )
            ListItem(
                headlineContent = { Text("Import from link") },
                supportingContent = { Text("Downloads the file behind any URL") },
                leadingContent = { Icon(Icons.Filled.Link, null, tint = colorScheme.primary) },
                modifier = Modifier.clickable { showLinkDialog = true }
            )
        }
    } else {
        // 🔧 18-Jul-2026: link entry — Import downloads; a non-file link ends as "No file found"
        var link by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Import from link") },
            text = {
                OutlinedTextField(
                    value = link, onValueChange = { link = it },
                    placeholder = { Text("https://example.com/file.pdf") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { onDismiss(); onImportFromLink(link) }, enabled = link.isNotBlank()) {
                    Text("Import")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }
}
