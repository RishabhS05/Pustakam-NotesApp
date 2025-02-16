@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.app.pustakam.android.widgets.audio

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.R
import com.app.pustakam.android.extension.startServiceWrapper
import com.app.pustakam.android.hardware.audio.player.AudioPlayingUIEvent
import com.app.pustakam.android.hardware.audio.player.PlayMediaViewModel
import com.app.pustakam.android.hardware.audio.player.PlayerUiState
import com.app.pustakam.android.services.mediaSessionService.PustakmMediaPlayerService
import com.app.pustakam.android.theme.typography
import com.app.pustakam.data.models.response.notes.NoteContentModel


@OptIn(UnstableApi::class)
@Composable
fun AudioPlayerUIState(
    noteContentModel: NoteContentModel.MediaContent, onDelete: (NoteContentModel) -> Unit = {}
) {
    val noteContent = remember { noteContentModel }
    val viewModel: PlayMediaViewModel = viewModel()
    val localContext = LocalContext.current as Activity
    val state = viewModel.state.collectAsStateWithLifecycle()
    val mediaState = state.value.mediaStates[noteContent.id] ?: PlayerUiState(noteContent = noteContent)

    if (mediaState.noteContent.position == noteContent.position) {
        AudioPlayView(state = mediaState, onDelete = {
            viewModel.onPlayingIntent(AudioPlayingUIEvent.PlayOrPauseUIEvent(mediaId = noteContent.id))
            onDelete(noteContentModel)
        }, onSeek = {
            viewModel.onPlayingIntent(AudioPlayingUIEvent.SeekToUIEvent(it, noteContent.id))
        }, onPlay = {
            if (!state.value.isServiceIsRunning) localContext.startServiceWrapper(Intent(localContext, PustakmMediaPlayerService::class.java))
            viewModel.onPlayingIntent(AudioPlayingUIEvent.SelectedAudioChange(noteContent.id))
        })
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AudioPlayView(
    state: PlayerUiState, onDelete: () -> Unit = {}, onPlay: () -> Unit = {}, onSeek: (Float) -> Unit = {},
) {

    val iconModifier = Modifier.size(28.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Card(modifier = Modifier.padding(8.dp)) {
        Column(modifier = Modifier) {
            Box(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp).fillMaxWidth(),
            ) {
                Text(
                    state.totalDuration, style = typography.labelSmall, modifier = Modifier.align(Alignment.TopStart)
                )
                Text(
                    state.timeElapsed, style = typography.labelMedium, modifier = Modifier.align(Alignment.TopCenter)
                )
                Text(
                    state.timeRemaining, style = typography.labelSmall, modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 6.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            ) {
                Slider(
                    modifier = Modifier.weight(0.5f).padding(horizontal = 6.dp).padding(bottom = 4.dp),
                    value = state.progress,
                    valueRange = 0f..100f,
                    onValueChange = {newValue ->
                        onSeek(newValue)
                    },
                    interactionSource = interactionSource,
                    track = { sliderPositions ->
                        SliderDefaults.Track(
                            sliderState = sliderPositions,
                            modifier = Modifier.height(8.dp),
                        )
                    },
                    thumb = {
                        SliderDefaults.Thumb(
                            modifier = Modifier.scale(scaleX = 0.5f, scaleY =1f),
                            interactionSource = interactionSource,
                        )
                    }
                )
                IconButton(onClick = onPlay) {
                    val drawable = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    Icon(
                        painter = painterResource(drawable), contentDescription = "", tint = colorScheme.onPrimaryContainer, modifier = iconModifier
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, contentDescription = "", tint = colorScheme.error, modifier = iconModifier
                    )
                }
            }
        }

    }
}

@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)
@Composable
private fun AudioPlayerPreview() {
    MyApplicationTheme {
        val state = PlayerUiState(
            totalDuration = "00 sec", progress = 10f, timeRemaining = "00:20", timeElapsed = "00:20",
            duration = 100, noteContent = NoteContentModel.MediaContent(noteId = "", duration = 100, position = 1)
        )
        AudioPlayView(state = state)
    }
}
