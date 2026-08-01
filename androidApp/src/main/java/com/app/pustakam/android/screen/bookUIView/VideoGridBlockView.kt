package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.hardware.audio.player.MediaPlayingUIEvent
import com.app.pustakam.android.hardware.audio.player.PlayMediaViewModel
import com.app.pustakam.core.filesys.reader.BlockHeightEstimator
import com.app.pustakam.core.filesys.reader.PageLayoutPolicy
import com.app.pustakam.core.filesys.reader.ReaderBlock
import com.app.pustakam.android.widgets.video.VideoCard

// 📖 01-Aug-2026: a grid is a LAYOUT, not a player. Every cell is the existing VideoCard, which
//   already gates on currentPlayingId, so the app still runs exactly ONE ExoPlayer and A.mp4 can
//   never render into B.mp4's cell.
@Composable
fun VideoGridBlockView(
    block: ReaderBlock.VideoGrid,
    policy: PageLayoutPolicy,
    modifier: Modifier = Modifier,
) {
    val playMediaViewModel: PlayMediaViewModel = viewModel()
    val visible = BlockHeightEstimator.visibleCells(block.items.size, policy)
    val shown = block.items.take(visible)

    if (visible == 1) {
        val media = shown[0]
        VideoCard(
            contentVideo = media,
            modifier = modifier.fillMaxWidth(),
            onClick = { playMediaViewModel.onPlayingIntent(MediaPlayingUIEvent.SelectedMediaChange(media.id)) },
        )
        return
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(policy.gridSpacing.dp)) {
        shown.chunked(policy.gridColumns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(policy.gridSpacing.dp)) {
                row.forEach { media ->
                    Box(Modifier.weight(1f)) {
                        VideoCard(
                            contentVideo = media,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                playMediaViewModel.onPlayingIntent(
                                    MediaPlayingUIEvent.SelectedMediaChange(media.id)
                                )
                            },
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
