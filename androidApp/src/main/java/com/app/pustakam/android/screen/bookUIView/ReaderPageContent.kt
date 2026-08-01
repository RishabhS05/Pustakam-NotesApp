package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.PaperColor
import com.app.pustakam.core.filesys.reader.PageLayoutPolicy
import com.app.pustakam.core.filesys.reader.ReaderPage
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

@Composable
fun ReaderPageContent(
    page: ReaderPage,
    policy: PageLayoutPolicy,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = true,
    zoomEnabled: Boolean = true,
    onOpenDocument: (NoteContentModel.MediaContent) -> Unit = {},
    onOpenImage: (NoteContentModel.MediaContent) -> Unit = {},
) {
    Box(
        modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.height(policy.pageHeight.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(PaperColor, RoundedCornerShape(6.dp))
            .clipToBounds()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = policy.marginStart.dp,
                    end = policy.marginEnd.dp,
                    top = policy.marginTop.dp,
                    bottom = policy.marginBottom.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(policy.blockGap.dp),
        ) {
            page.blocks.forEach { block ->
                ReaderBlockContent(
                    block = block,
                    policy = policy,
                    onOpenDocument = onOpenDocument,
                    onOpenImage = onOpenImage,
                )
            }
        }
        Box(
            Modifier.fillMaxHeight().width(14.dp).background(
                Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .18f), Color.Transparent)),
                RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
            )
        )
    }
}
