package com.app.pustakam.android.hardware.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pustakam.android.hardware.camera.ImageDataViewModel
import com.app.pustakam.android.hardware.camera.MediaFileStateHandler
import com.app.pustakam.android.hardware.camera.MediaProcessingEvent

@Composable
fun VideoPreviewScreen(imageDataViewModel: ImageDataViewModel){
    val state = imageDataViewModel
        .mediaFileState.collectAsStateWithLifecycle().value.apply {}
    VideoPreviewEditor(state, imageDataViewModel::onHandleMediaOperation)
}

@Composable
fun  VideoPreviewEditor(state: MediaFileStateHandler,
                        onEditImageAction: (MediaProcessingEvent) -> Unit,
                        modifier: Modifier = Modifier,
                        ) {
}
