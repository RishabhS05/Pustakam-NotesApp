package com.app.pustakam.android.widgets.audio


import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
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
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.R
import com.app.pustakam.android.hardware.audio.AudioRecorder
import com.app.pustakam.android.hardware.audio.IAudioRecorder
import com.app.pustakam.android.hardware.audio.IPlayerRecorder
import com.app.pustakam.android.theme.typography


@Composable
fun AudioRecordView(audioRecorder: IAudioRecorder) {
    var isPlaying by remember { mutableStateOf(true) }
    Card {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.requiredWidth(180.dp).padding(horizontal = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic), contentDescription = "mic"
            )
            Text(
                "00:00", style = typography.bodyMedium, modifier = Modifier.padding(horizontal = 6.dp)
            )
            Spacer(Modifier)
            IconButton(onClick = {
                isPlaying = !isPlaying
                if (isPlaying) {
//                    audioRecorder.start()
                } else {
                    audioRecorder.stop()
                }
            }) {
                val drawable = if (isPlaying) R.drawable.ic_stop else R.drawable.ic_record
                Icon(painter = painterResource(drawable), contentDescription = "")
            }
        }
    }
}

@Composable
fun AudioPlayView(player : IPlayerRecorder) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0.6f) }
    var currentTime by remember { mutableStateOf(0L) }
    var timeLapsed by remember { mutableStateOf(0L) }

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
                Box {
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
                    CircularProgressIndicator(progress = { currentProgress }, modifier = Modifier.width(38.dp).align(alignment = Alignment.Center), color = colorScheme.secondary, trackColor = colorScheme.primary)
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
        val audioRecorder = AudioRecorder(LocalContext.current)
        AudioRecordView(audioRecorder)
    }
}

@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)
@Composable
private fun AudioPlayerPreview() {
    MyApplicationTheme {
//        val audioRecorder = (LocalContext.current)
//        AudioPlayView()
    }
}