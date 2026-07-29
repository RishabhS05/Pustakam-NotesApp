package com.app.pustakam.android.extension

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.fileUtils.mimeTypeFor
import com.app.pustakam.android.fileUtils.saveMediaToGallery
import com.app.pustakam.android.fileUtils.suggestedFileName
import com.app.pustakam.android.fileUtils.writeMediaToUri
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.common.util.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 🔧 14-Jul-2026: NEW FEATURE — "Save media to device" overlay.
//   Renders the given media card with a small save icon pinned top-end.
//   • IMAGE / VIDEO  -> saveMediaToGallery() writes silently into the system Gallery.
//   • AUDIO / PDF / DOCX / GIF -> opens the ACTION_CREATE_DOCUMENT picker (default
//     location Downloads on most devices) so the user picks the path, then the file
//     bytes are copied to the chosen Uri via writeMediaToUri().
//   A Toast confirms success/failure. Purely additive — the wrapped card is unchanged.
//   Usage:  MediaSaveOverlay(media = contentImage) { ImageCard(...) }

// 🔧 14-Jul-2026: @OptIn required — ModalBottomSheet / rememberModalBottomSheetState are experimental.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.MediaSaveOverlay(
    media: NoteContentModel.MediaContent,
    onDelete: () -> Unit = {},
    onShare : ()-> Unit ={},
) {
    val context = LocalContext.current
    // 🔧 14-Jul-2026: PERF — coroutine scope so file copies run off the main thread.
    val scope = rememberCoroutineScope()
    val isGalleryType = media.type == ContentType.IMAGE || media.type == ContentType.VIDEO

    // SAF picker for non-gallery media (pdf/docx/gif). The pre-filled name carries the correct
    // extension; the returned Uri is the user-selected destination. (Audio is handled inside
    // the audio player card — see AudioPlayerUIState.)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeTypeFor(media.type))
    ) { uri ->
        if (uri != null) {
            // 🔧 14-Jul-2026: PERF — copy bytes on IO, confirm on the main thread.
            scope.launch {
                val ok = withContext(Dispatchers.IO) { writeMediaToUri(context, media, uri) }
                Toast.makeText(
                    context,
                    if (ok) "Saved to selected location" else "Save failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 🔧 14-Jul-2026: NEW FEATURE — controls the "Save to Gallery / Save as file" bottom sheet.
    //   Only used for IMAGE/VIDEO; other types still export straight to the SAF picker.
    var showSaveSheet by remember { mutableStateOf(false) }

    // 🔧 14-Jul-2026: helper — silent MediaStore save, run on IO, Toast on the main thread.
    val saveToGallery: () -> Unit = {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { saveMediaToGallery(context, media) }
            Toast.makeText(
                context,
                if (ok) "Saved to Gallery" else "Save failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Row(modifier = Modifier.align(Alignment.BottomEnd)) {
        IconButton(
            onClick = {
                if (isGalleryType) {
                    // 🔧 14-Jul-2026: CHANGED — was a silent gallery save; now opens the two-option
                    //   bottom sheet so the user picks Gallery vs. a file location.
                    showSaveSheet = true
                } else {
                    // Opens the picker with the suggested file name; default folder = Downloads.
                    exportLauncher.launch(suggestedFileName(media))
                }
            },
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SaveAlt,
                contentDescription = "Save media to device",
                tint = colorScheme.primary,
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Save media to device",
                tint = colorScheme.primary,
            )
        }
    }

    // 🔧 14-Jul-2026: NEW FEATURE — the save-options bottom sheet (IMAGE/VIDEO only).
    //   Row 1 → Save to Gallery (silent MediaStore write).
    //   Row 2 → Save to location as file (SAF picker, default Downloads).
    if (showSaveSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSaveSheet = false },
            sheetState = sheetState,
        ) {
            ListItem(
                headlineContent = { Text("Save to Gallery") },
                leadingContent = {
                    Icon(Icons.Filled.SaveAlt, contentDescription = null, tint = colorScheme.primary)
                },
                modifier = Modifier.clickable {
                    showSaveSheet = false
                    saveToGallery()
                }
            )
            ListItem(
                headlineContent = { Text("Save to location as file") },
                leadingContent = {
                    Icon(Icons.Filled.SaveAs, contentDescription = null, tint = colorScheme.primary)
                },
                modifier = Modifier.clickable {
                    showSaveSheet = false
                    // Reuses the existing SAF export; mimeTypeFor already handles image/png & video/mp4.
                    exportLauncher.launch(suggestedFileName(media))
                }
            )
        }
    }
}
