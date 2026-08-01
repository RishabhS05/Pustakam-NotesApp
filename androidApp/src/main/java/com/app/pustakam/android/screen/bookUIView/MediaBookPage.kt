package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.hardware.audio.player.MediaPlayingUIEvent
import com.app.pustakam.android.hardware.audio.player.PlayMediaViewModel
import com.app.pustakam.android.theme.PaperInk
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.audio.AudioPlayerUIState
import com.app.pustakam.android.widgets.video.VideoCard
import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

@Composable
 fun MediaBookPage(media: NoteContentModel.MediaContent) {
    val viewModel: PlayMediaViewModel = viewModel()
    val state = viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(media.id) {
        if (state.value.currentPlayingId != media.id) {
            viewModel.onPlayingIntent(MediaPlayingUIEvent.SelectedMediaChange(media.id))
        }
    }
    Column(
        Modifier.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            media.title.ifBlank { media.type.name }, style = typography.titleSmall,
            color = PaperInk, maxLines = 1, modifier = Modifier.padding(bottom = 8.dp)
        )
        if (media.type == ContentType.AUDIO) {
            AudioPlayerUIState(media)
        } else {
            VideoCard(
                contentVideo = media,
                modifier = Modifier,
                onClick = { viewModel.onPlayingIntent(MediaPlayingUIEvent.SelectedMediaChange(media.id)) },
            )
        }
    }
}
