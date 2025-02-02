package com.app.pustakam.android.widgets.audio

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.R
import com.app.pustakam.android.extension.startServiceWrapper
import com.app.pustakam.android.hardware.audio.player.AudioPlayingIntent
import com.app.pustakam.android.hardware.audio.player.AudioUiState
import com.app.pustakam.android.hardware.audio.player.PlayMediaViewModel
import com.app.pustakam.android.hardware.audio.recorder.AudioMode

import com.app.pustakam.android.hardware.audio.recorder.AudioRecordingIntent
import com.app.pustakam.android.hardware.audio.recorder.AudioState
import com.app.pustakam.android.hardware.audio.recorder.AudioViewModel
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.services.mediaSessionService.PustakmMediaPlayerService

import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.alert.DeleteNoteAlert
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.extensions.getReadableDuration
import com.app.pustakam.extensions.timerRemaining
import kotlinx.coroutines.flow.MutableStateFlow


@Composable
fun AudioRecording(
    noteContentModel: NoteContentModel.AudioContent, onStop: (NoteContentModel.AudioContent) -> Unit = {}, onDelete: (NoteContentModel.AudioContent) -> Unit = {}
) {
    val viewModel: AudioViewModel = viewModel()
    OnLifecycleEvent{ _, event ->
        when(event){
            Lifecycle.Event.ON_CREATE -> {
                viewModel.updateContent(noteContentModel)
            }
            else->{}
        }
    }
    val state = viewModel.state.collectAsStateWithLifecycle()
    state.value.apply {
        when {
            showDeleteDialog -> {
                DeleteNoteAlert(noteTitle = "Recording", onConfirm = {
                    viewModel.handleIntent(AudioRecordingIntent.DeleteRecordingIntent)
                    viewModel.showDeleteAlert(false)
                    state.value.noteContentModel?.let { onDelete(it) }
                }) {
                    viewModel.showDeleteAlert(false)
                }
            }

            audioMode == AudioMode.stop -> state.value.noteContentModel?.let { onStop(it) }
        }
    }
    AudioRecordView(state = state, onAction = viewModel::handleIntent, onDelete = {
        viewModel.showDeleteAlert(true)
    })
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun AudioRecordView(state: State<AudioState>, onAction: (AudioRecordingIntent) -> Unit, onDelete: () -> Unit) {
    val iconModifier = Modifier.size(28.dp)
    val elapsedTime = remember { mutableLongStateOf(0) }
    val isRecording = state.value.audioMode == AudioMode.start || state.value.audioMode == AudioMode.resume
    Card(modifier = Modifier.padding(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = "mic",
                tint = Color.White,
                modifier = Modifier.padding(4.dp).background(color = colorScheme.error,
                    shape = RoundedCornerShape(100)).padding(4.dp)
            )
            RecordingTimer(
                isTimerRunning = isRecording,
                elapsedTime = elapsedTime,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(onClick = {
                if (!isRecording) {
                    onAction(AudioRecordingIntent.StartRecordingIntent)
                } else {
                    onAction(AudioRecordingIntent.StopRecordingIntent(duration = elapsedTime.longValue))
                }
            }) {
                val drawable = if (isRecording) R.drawable.ic_stop else R.drawable.ic_record
                Icon(painter = painterResource(drawable), contentDescription = "", modifier = iconModifier)
            }

            IconButton(onClick = {
                if (isRecording) {
                    onAction(AudioRecordingIntent.PauseRecordingIntent)
                } else {
                    onAction(AudioRecordingIntent.ResumeRecordingIntent)
                }
            }) {
                val drawable = if (state.value.audioMode == AudioMode.pause) R.drawable.ic_play else R.drawable.ic_pause
                Icon(painter = painterResource(drawable), contentDescription = "", modifier = iconModifier)
            }
            IconButton(onClick = {
               onAction(AudioRecordingIntent.StopRecordingIntent(elapsedTime.longValue))
                onDelete()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "", tint = colorScheme.error, modifier = iconModifier)
            }
        }
    }
}


@Composable
fun AudioPlayerUIState(onDelete: (NoteContentModel) -> Unit = {}
) {
    val viewModel: PlayMediaViewModel = viewModel()
    val state = viewModel.state.collectAsStateWithLifecycle()
    state.value.apply {
        when {
            showDeleteDialog -> {
                DeleteNoteAlert(noteTitle = "Recording", onConfirm = {
                    viewModel.onPlayingIntent(AudioPlayingIntent.DeleteRecordingIntent)
                    viewModel.showDeleteAlert(false)
                    state.value.currentPlayingAudio?.let { onDelete(it) }
                }) {
                    viewModel.showDeleteAlert(false)
                }
            }
        }
    }
    AudioPlayView(state = state, onAction = viewModel::onPlayingIntent, onDelete = {
        viewModel.onPlayingIntent(AudioPlayingIntent.PlayOrPauseIntent)
        viewModel.showDeleteAlert(true)
    }, onSeek = {})
}

@OptIn(UnstableApi::class)
@Composable
fun AudioPlayView(
    state: State<AudioUiState>, onAction: (AudioPlayingIntent) -> Unit, onDelete: () -> Unit = {}, onSeek: (Float) -> Unit = {}
) {
    val stateValue = state.value
    val totalDuration = stateValue.duration
    val activity = LocalContext.current as Activity
    val iconModifier = Modifier.size(30.dp)
    Card(modifier = Modifier.padding(12.dp)) {
        Column(modifier = Modifier) {
            Box(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp).fillMaxWidth(),
            ) {
                Text(
                    totalDuration.getReadableDuration(), style = typography.labelSmall, modifier = Modifier.align(Alignment.TopStart)
                )
                Text("${stateValue.progress}",
                    style = typography.titleMedium, modifier = Modifier.align(Alignment.TopCenter)
                )
                Text(
                   totalDuration.timerRemaining(stateValue.progress), style = typography.labelSmall, modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 6.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            ) {
                Slider(
                    modifier = Modifier.weight(0.5f).padding(horizontal = 6.dp).padding(bottom = 6.dp),
                    value = stateValue.progress.toFloat(),
                    valueRange = 0f..1f,
                    onValueChange = { newValue ->
                        val newPosition = (newValue * totalDuration).toLong()
                        onSeek(newPosition.toFloat())
                    },
                )
                IconButton(onClick = {
                    if(!stateValue.isServiceIsRunning)  activity.startServiceWrapper(Intent(activity, PustakmMediaPlayerService::class.java))
                    onAction(AudioPlayingIntent.PlayOrPauseIntent)
                }) {
                    val drawable = if (stateValue.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    Icon(
                        painter = painterResource(drawable), contentDescription = "",
                        tint = colorScheme.onPrimaryContainer, modifier = iconModifier
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
private fun AudioRecordingPreview() {
    MyApplicationTheme {
        val state = MutableStateFlow(AudioState())
        AudioRecordView(state = state.collectAsStateWithLifecycle(), onAction = {}, onDelete = {})
    }
}

@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)
@Composable
private fun AudioPlayerPreview() {
    MyApplicationTheme {
        val state = MutableStateFlow(
            AudioUiState()
        )
        AudioPlayView(state = state.collectAsStateWithLifecycle(), onAction = {}, onSeek = {})
    }
}