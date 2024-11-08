package com.app.pustakam.android.screen.noteEditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotesEditorView(
    noteTitle: String = "", noteContent: String = "", isRuledEnabled: Boolean = true, onNoteChange: (String) -> Unit = {}
) {
    // Note content state
    val textState = remember { mutableStateOf(noteTitle) }
    val textTitleState = remember { mutableStateOf(noteContent) }
    val isRuledEnabledState by remember { mutableStateOf(isRuledEnabled) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(4.dp)
    ) {
        // Draw the ruled notebook background
        if (isRuledEnabledState) RuledPage()
        Column {
            TextField(
                value = textTitleState.value, placeholder = {
                    Text(
                        "Title : Keep your thoughts alive.", modifier = Modifier.padding(start = 100.dp), style = TextStyle(
                            color = Color.DarkGray,
                            fontSize = 18.sp,
                        )
                    )
                }, colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ), onValueChange = {
                    textTitleState.value = it
                    onNoteChange(it)
                }, textStyle = TextStyle(
                    color = Color.Black, fontSize = 24.sp
                ), modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(top = 2.dp)
            )
            // Note text input field
            TextField(
                value = textState.value, onValueChange = {
                    textState.value = it
                    onNoteChange(it)
                },

                placeholder = {
                    Text(
                        "Hi, whats in your mind take a quick note, before it get lost.", modifier = Modifier.padding(start = 100.dp), style = TextStyle(
                            color = Color.Gray, fontSize = 18.sp, lineHeight = 32.sp
                        )
                    )
                }, textStyle = TextStyle(
                    color = Color.Black, fontSize = 18.sp, lineHeight = 32.sp
                ), colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ), modifier = Modifier.fillMaxSize() // Padding to simulate left margin
            )
        }
    }
}

@Composable
fun RuledPage() {
    val lineColor = Color.LightGray
    val marginColor = Color.Red
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineSpacing = 35.dp.toPx() // Adjust for line spacing
        val startX = 80.dp.toPx() // Left margin

        // Draw horizontal lines
        var y = lineSpacing + 80
        while (y < size.height) {
            drawLine(
                color = lineColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx()
            )
            y += lineSpacing
        }

        // Draw vertical margin line
        drawLine(
            color = marginColor, start = androidx.compose.ui.geometry.Offset(startX, 0f), end = androidx.compose.ui.geometry.Offset(startX, size.height), strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = marginColor, start = androidx.compose.ui.geometry.Offset(startX + 20f, 0f), end = androidx.compose.ui.geometry.Offset(startX + 20f, size.height), strokeWidth = 2.dp.toPx()
        )
    }
}

@Preview
@Composable
private fun NoteEditorPreview() {
    NotesEditorView()

}