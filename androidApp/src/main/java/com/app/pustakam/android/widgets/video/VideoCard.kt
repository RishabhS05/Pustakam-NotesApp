package com.app.pustakam.android.widgets.video

import DarkBrown0
import android.app.Activity
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import brown8
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.hardware.audio.player.MediaPlayingUIEvent
import com.app.pustakam.android.hardware.audio.player.PlayMediaViewModel
import com.app.pustakam.android.hardware.audio.player.PlayerUiState
import com.app.pustakam.android.theme.typography
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.util.ContentType


@Composable
fun VideoCard(
    contentVideo: NoteContentModel.MediaContent,
    onClick: () -> Unit,
) {
    val context = LocalContext.current as Activity
    val viewModel: PlayMediaViewModel = viewModel()
    val noteContent = remember { contentVideo }
    val exoPlayer = remember { viewModel.getExoPlayer() }
    val state = viewModel.state.collectAsStateWithLifecycle()
    val mediaState = state.value.mediaStates[noteContent.id] ?: PlayerUiState(noteContent = noteContent)
    Card(modifier = Modifier.requiredWidth(200.dp).requiredHeight(300.dp).padding(8.dp).clickable { }) {
        //Preview Video
        if (mediaState.noteContent.position == noteContent.position)
            Box(Modifier.fillMaxSize()) {
         VideoPlayer(exoPlayer, Modifier.clickable { onClick() })
         VideoControllerUi(state =mediaState,
             onAction = viewModel::onPlayingIntent)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            PlayerView(context).also {
                it.player = exoPlayer
                it.useController = false
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                it.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT  ,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoControllerUi(
    state: PlayerUiState,
    isFullControllerEnabled: Boolean = false,
    onAction: (MediaPlayingUIEvent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val content = state.noteContent
    val iconModifier = Modifier.size(28.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFullScreen by remember{ mutableStateOf(   isFullControllerEnabled) }
    Box(
        modifier = Modifier.fillMaxSize().background(color = Color.DarkGray.copy(alpha = 0.1f))
    ) {
        /***
         * Top Action buttons
         */

        /***
         * Middle Action buttons
         */
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            //skip previous
            if (isFullScreen) IconButton(onClick = {
                onAction(MediaPlayingUIEvent.SeekToPrevious(content.id))
            }) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious, contentDescription = "Play/Pause Media", tint = brown8, modifier = iconModifier
                )
            }

            if (isFullScreen) IconButton(onClick = {
                onAction(MediaPlayingUIEvent.Backward(  mediaId = content.id))
            }) {
                Icon(
                    imageVector = Icons.Filled.Replay10, contentDescription = "forward Media", tint = brown8, modifier = iconModifier
                )
            }
            //play/pause
            IconButton(onClick = {
                onAction(MediaPlayingUIEvent.PlayOrPauseUIEvent(content.id))
            }) {
                val drawable = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                Icon(
                    imageVector = drawable, contentDescription = "Play/Pause Media", tint = brown8, modifier = Modifier.size(34.dp)
                )
            }
            //forward some seconds
            if (isFullScreen) IconButton(onClick = {
                onAction(MediaPlayingUIEvent.Forward(mediaId = content.id))
            }) {
                Icon(
                    imageVector = Icons.Filled.Forward10, contentDescription = "forward Media", tint = brown8, modifier = iconModifier
                )
            }
            //skip next
            if (isFullScreen) IconButton(onClick = {
                onAction(MediaPlayingUIEvent.SeekNextUIEvent(content.id))
            }) {
                Icon(
                    imageVector = Icons.Filled.SkipNext, contentDescription = "Play/Pause Media", tint = colorScheme.primary, modifier = iconModifier
                )
            }
        }
        /**
         * Back Action buttons
         */
        if(isFullScreen)
        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().wrapContentHeight()
        ) {
            Slider(modifier = Modifier.padding(horizontal = 4.dp).padding(bottom =4.dp),
                value = state.progress, valueRange = 0f..100f,
                onValueChange = { newValue ->
                    onAction(MediaPlayingUIEvent.SeekToUIEvent(newValue, content.id))
            }, interactionSource = interactionSource, track = { sliderPositions ->
                SliderDefaults.Track(
                    colors = SliderDefaults.colors(
                        thumbColor = brown8, // Thumb color
                        activeTrackColor =brown8, // Color of the track before the thumb
                        inactiveTrackColor = DarkBrown0 // Color of the track after the thumb
                    ),
                    thumbTrackGapSize = 0.dp,
                    sliderState = sliderPositions,
                    modifier = Modifier.height(8.dp),
                )
            }, thumb = {
                SliderDefaults.Thumb(
                    thumbSize = DpSize(width = 20.dp, height = 20.dp),
                    modifier = Modifier,
                    interactionSource = interactionSource,
                )
            })
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                .padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    state.timeElapsed, style = typography.labelMedium,
                    color = brown8,modifier = Modifier
                )
                Text(
                    state.timeRemaining, style = typography.labelMedium,
                    color = brown8,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }

        }
    }
}

@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)

@Composable
private fun PreviewVideoControllerUi() {
    val state = PlayerUiState(
        progress = 30f,
        noteContent = NoteContentModel.MediaContent(
            position = 0,
            noteId = "123434",
            type = ContentType.VIDEO,
        )
    )
    MyApplicationTheme {
        VideoControllerUi(state, isFullControllerEnabled = true)
    }

}