package com.app.pustakam.android.screen.notes.single


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.data.models.response.notes.Note
import orange80


@Composable
fun NoteCardView(modifier: Modifier = Modifier, note: Note, onClick: () -> Unit = {}) {
    val brush = Brush.verticalGradient(listOf(orange80
        ,orange80))
    Card(shape = CardDefaults.outlinedShape,
        modifier = modifier
            .fillMaxWidth(0.5f)
            .heightIn(min = 50.dp, max = 250.dp)
            .wrapContentHeight()
            .clickable { onClick() }
            .padding(4.dp)

    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
        ) {
            Column {
                Text(
                    note.title.toString(),
                    style = MaterialTheme.typography.
                    labelLarge.copy(color =
                    colorScheme.onSecondary),
                    modifier = Modifier
                        .padding(12.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun NotesPreview() {
    MyApplicationTheme {
        val note: Note = Note(
            id = "1",
            title = "Hare Rama Hare Rama",
            updatedAt = "",
            createdAt = "",
            categoryId = ""
        )
        NoteCardView(note = note)
    }

}