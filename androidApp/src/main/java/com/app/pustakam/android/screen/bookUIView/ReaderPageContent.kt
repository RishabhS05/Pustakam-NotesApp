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
import androidx.compose.ui.graphics.RectangleShape
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
    documents: InlineDocumentUiState = InlineDocumentUiState.Disabled,
    onOpenDocument: (NoteContentModel.MediaContent) -> Unit = {},
    onOpenImage: (NoteContentModel.MediaContent) -> Unit = {},
) {
    // 📖 15-Aug-2026: a document sheet is the page — edge to edge, no paper margins, no rounding
    val isDocument = page.isDocumentSheet
    Box(
        modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.height(policy.pageHeight.dp))
            .then(if (isDocument) Modifier else Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
            .background(PaperColor, if (isDocument) RectangleShape else RoundedCornerShape(6.dp))
            .clipToBounds()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = if (isDocument) 0.dp else policy.marginStart.dp,
                    end = if (isDocument) 0.dp else policy.marginEnd.dp,
                    top = if (isDocument) 0.dp else policy.marginTop.dp,
                    bottom = if (isDocument) 0.dp else policy.marginBottom.dp,
                ),
            verticalArrangement = if (isDocument) Arrangement.Top else Arrangement.spacedBy(policy.blockGap.dp),
        ) {
            page.blocks.forEach { block ->
                ReaderBlockContent(
                    block = block,
                    policy = policy,
                    documents = documents,
                    onOpenDocument = onOpenDocument,
                    onOpenImage = onOpenImage,
                )
            }
        }
        // the spine shading is book paper, not part of a document sheet
        if (!isDocument) Box(
            Modifier.fillMaxHeight().width(14.dp).background(
                Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .18f), Color.Transparent)),
                RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
            )
        )
    }
}
