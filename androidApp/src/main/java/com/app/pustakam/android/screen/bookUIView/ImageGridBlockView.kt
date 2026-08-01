package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.app.pustakam.core.filesys.reader.BlockHeightEstimator
import com.app.pustakam.core.filesys.reader.PageLayoutPolicy
import com.app.pustakam.core.filesys.reader.ReaderBlock
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.getMediaUrl

// 📖 01-Aug-2026: 2-column grid showing EVERY image, wrapping downward. Sizes come straight from the
//   policy — policy units are screen units, so no measurement is needed and nothing can collapse.
//   A grid too tall for a page was already split into consecutive blocks by the builder.
@Composable
fun ImageGridBlockView(
    block: ReaderBlock.ImageGrid,
    policy: PageLayoutPolicy,
    modifier: Modifier = Modifier,
    onImageClick: (NoteContentModel.MediaContent) -> Unit = {},
) {
    if (block.items.size == 1) {
        val media = block.items[0]
        ImageCell(
            media = media,
            onClick = { onImageClick(media) },
            modifier = modifier
                .fillMaxWidth()
                .height(BlockHeightEstimator.singleImageHeight(media, policy).dp),
        )
        return
    }
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(policy.gridSpacing.dp),
    ) {
        block.items.chunked(policy.gridColumns).forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(policy.imageCellHeight.dp),
                horizontalArrangement = Arrangement.spacedBy(policy.gridSpacing.dp),
            ) {
                row.forEach { media ->
                    ImageCell(
                        media = media,
                        onClick = { onImageClick(media) },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                }
                // keeps a lone trailing cell at column width instead of stretching it
                if (row.size < policy.gridColumns) repeat(policy.gridColumns - row.size) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ImageCell(
    media: NoteContentModel.MediaContent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = media.getMediaUrl(),
        contentDescription = media.title,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).clickable { onClick() },
    )
}
