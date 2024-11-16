package com.app.pustakam.android.widgets.alert

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@Composable
fun DeleteNoteAlert(
    noteTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color =colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = TextStyle(color = colorScheme.secondary))
            }
        },
        title = {
            Text(text = "Delete Note?", style = TextStyle(color = colorScheme.error, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        },
        text = {
            Text(text = "Are you sure you want to permanently delete the note \"$noteTitle\"? This action cannot be undone.",
                style = TextStyle( fontSize = 16.sp, fontWeight = FontWeight.Medium))
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}