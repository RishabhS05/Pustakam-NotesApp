package com.app.pustakam.android.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay


@Composable
fun SnackBarUi(error: String, modifier: Modifier = Modifier, dismissAction: () -> Unit) {

    Box(modifier = modifier.fillMaxSize()) {
        Snackbar(action = {
            LaunchedEffect(key1 = error) {
                delay(1000)
                dismissAction()
            }
        }, modifier = Modifier
            .graphicsLayer {
                shadowElevation = 5f
            }
            .align(Alignment.BottomCenter)
        ) {
            Text(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(), text = error,
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

