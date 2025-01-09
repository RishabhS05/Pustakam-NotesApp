package com.app.pustakam.android.widgets.audio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.app.pustakam.android.theme.typography
import com.app.pustakam.util.getTimerFormatedString
import kotlinx.coroutines.delay

@Composable
fun RecordingTimer(
    isTimerRunning: Boolean,
    modifier: Modifier = Modifier
) {
    var elapsedTime by remember { mutableLongStateOf(0) }
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (isTimerRunning) {
                delay(1000)
                elapsedTime += 1
            }
        }
    }
    Text(getTimerFormatedString(elapsedTime),style = typography.bodyMedium, modifier = modifier)
}
