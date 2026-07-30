package com.app.pustakam.android.widgets.image

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.widgets.LoadImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 🔧 14-Jul-2026: CHANGED — hover/focus reveal reverted (didn't work on device); the save overlay
//   is now revealed by LONG-PRESS with a 2.5s auto-hide, matching the iOS cards exactly.
//   `onShowActions(true)` fires on long-press, `onShowActions(false)` after 2.5s; a new long-press
//   restarts the timer. Tap still opens the preview via onClick.
//   Usage: ImageCard(imageUrl, onShowActions = { visible -> ... }, onClick = {...}) { overlay }
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCard(modifier: Modifier = Modifier,
              imageUrl : String = "", onClick: ()-> Unit,
              onShowActions: (Boolean) -> Unit = {},
              overlay: @Composable BoxScope.() -> Unit = {}) {
    val scope = rememberCoroutineScope()
    // 🔧 14-Jul-2026: pending auto-hide; cancelled and restarted on every long-press
    val hideJob = remember { mutableStateOf<Job?>(null) }
    Box(modifier = modifier) {
        Card(modifier = Modifier.fillMaxWidth(0.6f).requiredHeight(350.dp)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = {
                    onShowActions(true)
                    hideJob.value?.cancel()
                    hideJob.value = scope.launch {
                        delay(2500)
                        onShowActions(false)
                    }
                }
            )
            .padding(8.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                LoadImage(url = imageUrl, modifier = modifier.matchParentSize())
                overlay()
            }
        }
    }
}
@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)
@Composable
private fun ImageCardPreview() {
    /** App Theme */ /** View */
    MyApplicationTheme {
        /** View */ /** View */ ImageCard(onClick =  {}){}
    }
}
