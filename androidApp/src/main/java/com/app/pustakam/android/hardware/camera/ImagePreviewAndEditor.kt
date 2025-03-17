package com.app.pustakam.android.hardware.camera

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pustakam.android.fileUtils.createFileWithFolders
import com.app.pustakam.android.screen.navigation.Route
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.getCurrentTimestamp

/**
 * Screen Handle Following features
 * F1 : Preview Captured Image
 * F2 : Crop Image
 * F3 : Save Image
 * F4 : Discard EditChanges
 * F5 : Draw on Image (pencil tool)
 * F6 : undo and redo changes on Image.
 * */
@Composable
fun ImageEditorScreen(imageDataViewModel: ImageDataViewModel,
                      onDismiss: (route: String?) -> Unit, ) {
    val state = imageDataViewModel
        .mediaFileState.collectAsStateWithLifecycle().value
    ImagePreviewAndEditor(state = state, onEditImageAction=imageDataViewModel::onHandleMediaOperation, onDismiss)
}
@Composable
fun ImagePreviewAndEditor(
    state: MediaFileStateHandler,
    onEditImageAction: (MediaProcessingEvent) -> Unit,
    onDismiss: (route: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current as Activity
    val saveImage = {
        val timeStamp = getCurrentTimestamp()
        onEditImageAction(MediaProcessingEvent.OnSaveImage(
            createFileWithFolders(context,
                "${ContentType.IMAGE.name.lowercase()}/${timeStamp}"
                , "${timeStamp}${ContentType.IMAGE.getExt()}")
        ))
    }
    Box(modifier = modifier) {
        state.bitmap?.asImageBitmap()?.let {
            Image(
                it, contentDescription = "Image Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillHeight
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(color = colorScheme.background.copy(alpha = .5f))
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement =Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
               saveImage()
                onDismiss(null)
            }, modifier = Modifier) {
                Icon(imageVector = Icons.Default.Save,
                    contentDescription = "Save Image")
            }
            //todo add image cropping feature

            IconButton(onClick = {
                onEditImageAction(if(state.dataStateEvent == DataStateEvent.Editing) MediaProcessingEvent.CropImage else MediaProcessingEvent.EditImage)
            }, modifier = Modifier) {
                Icon(imageVector =  if(state.dataStateEvent == DataStateEvent.Editing) Icons.Default.Crop else Icons.Filled.Edit,
                    contentDescription = "Crop Image")
            }
        }
        Button(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), onClick = {
                if(state.dataStateEvent == DataStateEvent.Editing) saveImage()
                onDismiss(Route.NotesEditor)
            }){ Text("Done") }
    }
}
