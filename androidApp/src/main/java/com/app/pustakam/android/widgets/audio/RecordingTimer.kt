package com.app.pustakam.android.widgets.audio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.app.pustakam.android.theme.typography
import com.app.pustakam.core.common.extensions.getTimerFormatedString
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RecordingTimer(
    isTimerRunning: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = typography.bodyMedium,
    elapsedTime: MutableLongState,
) {
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (isTimerRunning) {
                delay(1000.milliseconds)
                elapsedTime.value += 1
            }
        }
    }
    Text(elapsedTime.longValue.getTimerFormatedString(),style = style, modifier = modifier)
}
