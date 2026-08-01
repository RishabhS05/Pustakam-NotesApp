package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.hardware.audio.player.MediaPlayingUIEvent
import com.app.pustakam.android.hardware.audio.player.PlayMediaViewModel
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.video.VideoCard
import com.app.pustakam.core.filesys.reader.BlockHeightEstimator
import com.app.pustakam.core.filesys.reader.PageLayoutPolicy
import com.app.pustakam.core.filesys.reader.ReaderBlock
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

@Composable
fun VideoGridBlockView(
    block: ReaderBlock.VideoGrid,
    policy: PageLayoutPolicy,
    modifier: Modifier = Modifier,
    onVideoClick: (NoteContentModel.MediaContent) -> Unit = {},
) {
    val visible = BlockHeightEstimator.visibleCells(block.items.size, policy)
    val overflow = BlockHeightEstimator.overflowCount(block.items.size, policy)

    if (visible == 1) {
        VideoCell(
            media = block.items[0],
            overflow = 0,
            cellHeight = (policy.usableWidth * BlockHeightEstimator.VIDEO_ASPECT).dp,
            onClick = onVideoClick,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f / BlockHeightEstimator.VIDEO_ASPECT),
        )
        return
    }
    val shown = block.items.take(visible)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(policy.gridSpacing.dp)) {
        shown.chunked(policy.gridColumns).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(policy.gridSpacing.dp)) {
                row.forEachIndexed { columnIndex, media ->
                    val isLastCell = rowIndex * policy.gridColumns + columnIndex == visible - 1
                    VideoCell(
                        media = media,
                        overflow = if (isLastCell) overflow else 0,
                        cellHeight = (policy.gridCellWidth * BlockHeightEstimator.VIDEO_ASPECT).dp,
                        onClick = onVideoClick,
                        modifier = Modifier.weight(1f).aspectRatio(1f / BlockHeightEstimator.VIDEO_ASPECT),
                    )
                }
                if (row.size < policy.gridColumns) repeat(policy.gridColumns - row.size) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun VideoCell(
    media: NoteContentModel.MediaContent,
    overflow: Int,
    cellHeight: Dp,
    onClick: (NoteContentModel.MediaContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playMediaViewModel: PlayMediaViewModel = viewModel()
    Box(modifier.clip(RoundedCornerShape(6.dp))) {
        VideoCard(
            contentVideo = media,
            modifier = Modifier.fillMaxSize(),
            widthFraction = 1f,
            fixedHeight = cellHeight,
            onClick = {
                playMediaViewModel.onPlayingIntent(MediaPlayingUIEvent.SelectedMediaChange(media.id))
                onClick(media)
            },
        )
        if (overflow > 0) Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = .45f))
                .clickable { onClick(media) },
            contentAlignment = Alignment.Center,
        ) {
            Text("+$overflow", style = typography.headlineSmall, color = Color.White)
        }
    }
}
