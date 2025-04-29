package com.app.pustakam.android.widgets.audio

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import brown3
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.R
import com.app.pustakam.android.hardware.audio.recorder.AudioLifecycle
import com.app.pustakam.android.hardware.audio.recorder.AudioRecordingIntent
import com.app.pustakam.android.hardware.audio.recorder.AudioState
import com.app.pustakam.android.hardware.audio.recorder.AudioViewModel
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.data.models.response.notes.NoteContentModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.sin


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
    val levelState = viewModel.audioLevels.collectAsStateWithLifecycle()
    state.value.apply {
        when {
            audioLifecycle == AudioLifecycle.stop -> state.value.noteContentModel?.let { onStop(it) }
        }
    }
    AudioRecordView(state = state, levelState = levelState,
        onAction = viewModel::handleIntent)
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun AudioRecordView(modifier: Modifier = Modifier,
    state: State<AudioState>,
                    levelState : State<List<Float>>,
                    onAction: (AudioRecordingIntent) -> Unit
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
            AudioVisualizerView(levelState.value, modifier= Modifier.height(60.dp)
                .width(100.dp),)

            IconButton(onClick = {
                if (isRecording) {
                    onAction(AudioRecordingIntent.PauseRecordingIntent)
                } else {
                    onAction(AudioRecordingIntent.ResumeRecordingIntent)
                }
            }) {
                val drawable = ImageVector.vectorResource(R.drawable.ic_record)
                Icon(imageVector =drawable, contentDescription = "", modifier = iconModifier, tint = if (state.value.audioLifecycle ==
                    AudioLifecycle.pause) Color.Gray else Color.Red )
            }
            IconButton(onClick = {
                if (!isRecording) {
                    onAction(AudioRecordingIntent.StartRecordingIntent)
                } else {
                    onAction(AudioRecordingIntent.StopRecordingIntent(duration = elapsedTime.longValue))
                }
            }) {
                val drawable =  Icons.Rounded.Stop
                Icon(imageVector = drawable, contentDescription = "", modifier = iconModifier)
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
        val stateFlow = MutableStateFlow<List<Float>>(
            value = listOf(.1f,.2f,.3f,.4f,.5f,.60f,1f, .1f,.2f)
        )
        AudioRecordView(state = state.collectAsStateWithLifecycle(),
            levelState = stateFlow.collectAsStateWithLifecycle(),
            onAction = {},)
    }
}




@Composable
fun AudioVisualizerView(audioLevels: List<Float>, modifier: Modifier =Modifier) {
    val animatedLevels = audioLevels.map { level ->
        animateFloatAsState(
            targetValue = level,
            animationSpec = tween(durationMillis = 200),
            label = "audioLevelAnimation"
        ).value
    }
    Canvas(modifier = modifier
        .padding(4.dp)
    ) {
        val barWidth = size.width / animatedLevels.size
        val centerY = size.height / 2  // Middle of the canvas
        val waveFrequency = 10  // Controls wave effect

        // Draw a middle horizontal line
        drawLine(
            color = Color.Gray,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 2f
        )

        // Draw pulse wave bars
        animatedLevels.forEachIndexed { index, level ->
            val waveEffect = (sin((index + System.currentTimeMillis() / 100.0) / waveFrequency) + 1) / 2
            val barHeight = level * waveEffect * (size.height / 2)  // Adjust to stay within middle
            drawRoundRect(
                color = brown3,
                topLeft = Offset(x = index * barWidth, y = (centerY - barHeight).toFloat()),
                size = Size(barWidth * 0.8f, (barHeight * 2).toFloat()), // Extend above and below the line
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}