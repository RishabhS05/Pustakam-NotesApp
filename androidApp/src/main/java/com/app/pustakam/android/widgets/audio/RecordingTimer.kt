package com.app.pustakam.android.widgets.audio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.app.pustakam.android.theme.typography
import com.app.pustakam.extensions.getTimerFormatedString
import kotlinx.coroutines.delay

@Composable
fun RecordingTimer(
    isTimerRunning: Boolean,
    style: TextStyle = typography.bodyMedium,
    modifier: Modifier = Modifier,
    elapsedTime: MutableLongState,
) {
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (isTimerRunning) {
                delay(1000)
                elapsedTime.value += 1
            }
        }
    }
    Text(elapsedTime.longValue.getTimerFormatedString(),style = style, modifier = modifier)
}
