package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.pustakam.core.filesys.reader.PageLayoutPolicy
import com.app.pustakam.core.filesys.reader.ReaderPage
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

// 📖 01-Aug-2026: ONE A4 sheet = a Column of blocks. The engine already decided which blocks land
//   here, so this only stacks them in order. In page mode the sheet fills the screen and scrolls
//   internally, so an expanded document can grow without breaking the page geometry.
@Composable
fun ReaderPageContent(
    page: ReaderPage,
    policy: PageLayoutPolicy,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = true,
    onOpenDocument: (NoteContentModel.MediaContent) -> Unit = {},
    onOpenMedia: (NoteContentModel.MediaContent) -> Unit = {},
) {
    PaperPage(fillHeight = fillHeight, modifier = modifier) {
        Column(
            Modifier
                .then(if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .then(if (fillHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(policy.blockGap.dp),
        ) {
            page.blocks.forEach { block ->
                ReaderBlockContent(
                    block = block,
                    policy = policy,
                    onOpenDocument = onOpenDocument,
                    onOpenMedia = onOpenMedia,
                )
            }
        }
    }
}
