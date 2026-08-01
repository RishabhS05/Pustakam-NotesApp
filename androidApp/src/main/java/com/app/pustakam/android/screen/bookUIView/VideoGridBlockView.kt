package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.widgets.video.VideoCard
import com.app.pustakam.core.filesys.reader.PageLayoutPolicy
import com.app.pustakam.core.filesys.reader.ReaderBlock
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

// 📖 01-Aug-2026: a grid is a LAYOUT, not a player. Every cell is the existing VideoCard, which
//   gates on currentPlayingId, so the app still runs exactly ONE ExoPlayer. Cards get an explicit
//   height so their requiredHeight can no longer overlap a neighbour.
@Composable
fun VideoGridBlockView(
    block: ReaderBlock.VideoGrid,
    policy: PageLayoutPolicy,
    modifier: Modifier = Modifier,
    onOpenVideo: (NoteContentModel.MediaContent) -> Unit = {},
) {
    if (block.items.size == 1) {
        val media = block.items[0]
        VideoCard(
            contentVideo = media,
            modifier = modifier.fillMaxWidth(),
            widthFraction = 1f,
            fixedHeight = (policy.usableWidth * PageLayoutPolicy.VIDEO_ASPECT).dp,
            onClick = { onOpenVideo(media) },
        )
        return
    }
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(policy.gridSpacing.dp),
    ) {
        block.items.chunked(policy.gridColumns).forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(policy.videoCellHeight.dp),
                horizontalArrangement = Arrangement.spacedBy(policy.gridSpacing.dp),
            ) {
                row.forEach { media ->
                    Box(Modifier.weight(1f)) {
                        VideoCard(
                            contentVideo = media,
                            modifier = Modifier.fillMaxWidth(),
                            widthFraction = 1f,
                            fixedHeight = policy.videoCellHeight.dp,
                            onClick = { onOpenVideo(media) },
                        )
                    }
                }
                if (row.size < policy.gridColumns) repeat(policy.gridColumns - row.size) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}
