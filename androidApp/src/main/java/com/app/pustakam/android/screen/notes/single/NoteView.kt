package com.app.pustakam.android.screen.notes.single

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import baseWhite
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.extensions.toLocalFormat
import sideBrownGradient

@Composable
fun NoteCardView(modifier: Modifier = Modifier, note: Note,
                 onClick: () -> Unit = {}) {
    Card(shape = RoundedCornerShape(5),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp,
            hoveredElevation = 16.dp, focusedElevation = 16.dp,  pressedElevation = 12.dp),
        modifier = modifier.fillMaxWidth(0.5f).clickable { onClick() }
        .heightIn(min = 100.dp, max = 300.dp).wrapContentHeight().padding(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 120.dp, max = 500.dp)
                .background(
                    color=  Color.Transparent,
                )
        ) {
                Text(
                   if (!note.title.isNullOrEmpty())note.title.toString() else "Title?" ,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colorScheme.onPrimaryContainer
                    ), modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                )
            Text(
                note.updatedAt?.toLocalFormat(showTime = false).toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = baseWhite
                ), modifier = Modifier.background(brush = Brush.horizontalGradient(sideBrownGradient),
                    shape = RoundedCornerShape(10)).align(Alignment.TopEnd).padding(4.dp)
            )
        }
    }
    }

@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)

@Composable
private fun NotesPreview() {
    MyApplicationTheme {
        val note: Note = Note(
            id = "1", title = "Hare Rama Hare Rama", updatedAt = "18/03/2025",
            createdAt = "", categoryId = ""
        )
        NoteCardView(note = note)

    }

}