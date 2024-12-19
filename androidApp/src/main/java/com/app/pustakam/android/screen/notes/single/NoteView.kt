package com.app.pustakam.android.screen.notes.single

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pustakam.data.models.response.notes.Note

@Composable
fun NoteCardView(modifier: Modifier = Modifier, note: Note, onClick: () -> Unit = {}) {
    Card(shape = CardDefaults.outlinedShape,
        modifier = modifier
            .fillMaxWidth(0.5f)
            .heightIn(min = 50.dp, max = 250.dp)
            .wrapContentHeight()
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Column {
            Text(
                note.title.toString(),
                modifier = Modifier
                    .padding(8.dp),
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            )
        }
    }
}

@Preview
@Composable
private fun NotesPreview() {
    val note: Note = Note(
        id = "1",
        title = "Hare Rama Hare Rama",
        updatedAt = "",
        createdAt = "",
        categoryId = ""
    )
    NoteCardView(note = note)
}