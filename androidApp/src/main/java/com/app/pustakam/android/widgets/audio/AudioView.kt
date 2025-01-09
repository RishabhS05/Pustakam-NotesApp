package com.app.pustakam.android.widgets.audio


import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.R
import com.app.pustakam.android.hardware.audio.AudioState
import com.app.pustakam.android.hardware.audio.AudioViewModel
import com.app.pustakam.android.theme.typography
import com.app.pustakam.data.models.response.notes.NoteContentModel
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import com.app.pustakam.android.hardware.audio.AudioMode
import com.app.pustakam.android.hardware.audio.AudioRecordingIntent
import com.app.pustakam.android.widgets.alert.DeleteNoteAlert
import kotlinx.coroutines.flow.MutableStateFlow


@Composable
fun AudioRecording(noteContentModel: NoteContentModel.AudioContent,
                   onStop:(NoteContentModel.AudioContent)-> Unit = {} ,
                   onDelete: (NoteContentModel.AudioContent) -> Unit = {}){
    val viewModel : AudioViewModel = viewModel()
    viewModel.updateContent(noteContentModel)
    val state = viewModel.state.collectAsStateWithLifecycle()
    state.value.apply {
        when{
            showDeleteDialog -> {
                DeleteNoteAlert(noteTitle = "Recording",
                    onConfirm = {
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
    AudioRecordView(state = state, onAction =
    viewModel::handleIntent,onDelete= {
        viewModel.showDeleteAlert(true)
    } )
}
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun AudioRecordView(state : State<AudioState>,onAction : (AudioRecordingIntent) -> Unit, onDelete:()-> Unit) {
    val iconModifier  =Modifier.size(28.dp)
    val isRecording = state.value.audioMode == AudioMode.start || state.value.audioMode == AudioMode.resume
    Card {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic), contentDescription = "mic",
                tint = Color.White,
                modifier = Modifier.padding(4.dp).background(color =  colorScheme.error, shape = RoundedCornerShape(100)).padding(4.dp)
            )
            RecordingTimer(
                isTimerRunning = isRecording,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(onClick = {
                if (!isRecording) {
                    onAction(AudioRecordingIntent.StartRecordingIntent)
                } else {
                   onAction(AudioRecordingIntent.StopRecordingIntent)
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
                Icon(painter = painterResource(drawable), contentDescription = "",modifier = iconModifier)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "", tint = colorScheme.error, modifier = iconModifier)
            }
        }
    }
}

@Composable
fun AudioPlayView() {
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(60f) }
    var currentTime by remember { mutableStateOf(0L) }
    var timeLapsed by remember { mutableStateOf(0L) }
    val onProgress: (Float) -> Unit = {
        currentProgress = it
    }

    Card {
        Column(modifier = Modifier.requiredWidth(180.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "7min", style = typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp)
                )
                Text(
                    "00:00", style = typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                Icon(painter = painterResource(R.drawable.ic_mic), contentDescription = "mic")
                Text("00:00", style = typography.bodyMedium, modifier = Modifier.padding(horizontal = 6.dp))
                Spacer(modifier = Modifier)
                IconButton(onClick = {
                    isPlaying = !isPlaying
                    if (isPlaying) {
//                    audioRecorder.start()
                    } else {
//                    player.stop()
                    }
                }) {
                    val drawable = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    Icon(painter = painterResource(drawable), contentDescription = "", tint = colorScheme.onPrimaryContainer)
                }
//                    CircularProgressIndicator(progress = { currentProgress }, modifier = Modifier.width(38.dp).align(alignment = Alignment.Center), color = colorScheme.secondary, trackColor = colorScheme.primary)
            }
            Slider(modifier = Modifier.padding(horizontal = 6.dp), value = currentProgress, onValueChange = { onProgress(it) }, valueRange = 0f..100f)
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
        val audioRecorder = (LocalContext.current)
        AudioPlayView()
    }
}