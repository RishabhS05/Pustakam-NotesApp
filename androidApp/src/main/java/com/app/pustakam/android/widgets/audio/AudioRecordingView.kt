package com.app.pustakam.android.widgets.audio

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.R
import com.app.pustakam.android.hardware.audio.recorder.AudioLifecycle
import com.app.pustakam.android.hardware.audio.recorder.AudioRecordingIntent
import com.app.pustakam.android.hardware.audio.recorder.AudioState
import com.app.pustakam.android.hardware.audio.recorder.AudioViewModel
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.data.models.response.notes.NoteContentModel
import kotlinx.coroutines.flow.MutableStateFlow


@Composable
fun AudioRecording(modifier: Modifier = Modifier,
    noteContentModel: NoteContentModel.MediaContent,
    onStop: (NoteContentModel.MediaContent) -> Unit = {},
                    onDelete: (NoteContentModel.MediaContent) -> Unit = {}
) {
    val viewModel: AudioViewModel = viewModel<AudioViewModel>()

    OnLifecycleEvent{ _, event->
        when {
            event == Lifecycle.Event.ON_CREATE->{
               viewModel.updateContent(noteContentModel)
            }
        }
    }
    val state = viewModel.state.collectAsStateWithLifecycle()
    state.value.apply {
        when {
            audioLifecycle == AudioLifecycle.stop -> state.value.noteContentModel?.let { onStop(it) }
        }
    }
    AudioRecordView(state = state, onAction = viewModel::handleIntent)
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun AudioRecordView(modifier: Modifier = Modifier,
    state: State<AudioState>, onAction: (AudioRecordingIntent) -> Unit
) {
    val iconModifier = Modifier.size(28.dp)
    val elapsedTime = remember { mutableLongStateOf(0) }
    val isRecording = state.value.audioLifecycle ==
            AudioLifecycle.start || state.value.audioLifecycle == AudioLifecycle.resume
    Card(modifier = modifier.padding(12.dp), elevation = CardDefaults
        .elevatedCardElevation(defaultElevation = 12.dp) ) {
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
            //wave animation
            IconButton(onClick = {
                if (!isRecording) {
                    onAction(AudioRecordingIntent.StartRecordingIntent)
                } else {
                    onAction(AudioRecordingIntent.StopRecordingIntent(duration = elapsedTime.longValue))
                }
            }) {
                val drawable = if (isRecording) Icons.Rounded.Stop else ImageVector.vectorResource(R.drawable.ic_record)
                Icon(imageVector = drawable, contentDescription = "", modifier = iconModifier)
            }

            IconButton(onClick = {
                if (isRecording) {
                    onAction(AudioRecordingIntent.PauseRecordingIntent)
                } else {
                    onAction(AudioRecordingIntent.ResumeRecordingIntent)
                }
            }) {
                val drawable = if (state.value.audioLifecycle == AudioLifecycle.pause) Icons.Rounded.PlayArrow else Icons.Rounded.Pause
                Icon(imageVector =drawable, contentDescription = "", modifier = iconModifier)
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
        AudioRecordView(state = state.collectAsStateWithLifecycle(), onAction = {},)
    }
}

