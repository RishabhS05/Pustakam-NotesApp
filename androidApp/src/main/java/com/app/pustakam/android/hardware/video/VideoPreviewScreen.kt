package com.app.pustakam.android.hardware.video

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.app.pustakam.android.extension.startServiceWrapper
import com.app.pustakam.android.hardware.audio.player.MediaPlayingUIEvent
import com.app.pustakam.android.hardware.audio.player.PlayMediaViewModel
import com.app.pustakam.android.hardware.camera.ImageDataViewModel
import com.app.pustakam.android.hardware.camera.MediaProcessingEvent
import com.app.pustakam.android.services.mediaSessionService.PustakmMediaPlayerService
import com.app.pustakam.android.widgets.video.VideoControllerUi
import com.app.pustakam.android.widgets.video.VideoPlayer
import com.app.pustakam.core.common.util.ScreenOrientation

// 🔧 14-Jul-2026: IMPLEMENTED — this screen was an empty stub (`VideoPreviewEditor(...) {}`), which
//   is exactly the "white screen" seen when opening a video. It is now a full-screen player for the
//   media selected via ImageDataViewModel.onSetMediaToPreview(path, VIDEO, mediaId):
//   • resolves the media by id (fallback: match the file path against the playlist repository)
//   • hands the shared ExoPlayer surface to this screen (VideoPlayer, attachPlayer = true)
//   • if that media is ALREADY the current playing item it is left untouched — leaving and
//     re-entering the screen keeps playback "as is" (media stays standalone, service-backed)
//   • full controller (slider / ±10s / next / previous) via VideoControllerUi(isFullControllerEnabled)
//   How to use: navigate(Route.VideoPreview) after onSetMediaToPreview(...) — see NotesEditorView.
@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewScreen(imageDataViewModel: ImageDataViewModel
                       = viewModel(viewModelStoreOwner =  requireNotNull(LocalView.current.findViewTreeViewModelStoreOwner()))
                       , onDismiss : () -> Unit = {}){
    val previewState = imageDataViewModel.mediaFileState.collectAsStateWithLifecycle().value
    val viewModel: PlayMediaViewModel = viewModel()
    val playerState = viewModel.state.collectAsStateWithLifecycle().value
    val context = LocalContext.current as Activity

    // Resolve which media to show: explicit id first, then path match against the playlist.
    val mediaId = previewState.mediaId ?: playerState.mediaStates.values.firstOrNull {
        val content = it.noteContent
        content.localPath == previewState.mediaFilePath || content.url == previewState.mediaFilePath
    }?.noteContent?.id

    if (mediaId == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Video not available", color = Color.White)
        }
        return
    }

    BackHandler() {
        imageDataViewModel.onHandleMediaOperation(MediaProcessingEvent.ScreenOrientationEvent(ScreenOrientation.UNSPECIFIED))
        onDismiss()
    }
    DisposableEffect(previewState.orientation) {
        context.requestedOrientation = when (previewState.orientation) {
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(mediaId) {
        if (!playerState.isServiceIsRunning) {
            context.startServiceWrapper(Intent(context, PustakmMediaPlayerService::class.java))
        }
        val isAlreadyPlayingThis = playerState.currentPlayingId == mediaId &&
                (playerState.mediaStates[mediaId]?.isPlaying == true)
        // Only kick playback when this media isn't already the live one — no restart on re-entry.
        if (!isAlreadyPlayingThis) {
            viewModel.onPlayingIntent(MediaPlayingUIEvent.SelectedMediaChange(mediaId))
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VideoPlayer(
            exoPlayer = viewModel.getExoPlayer(),
            modifier = Modifier.fillMaxSize().align(Alignment.Center),
            attachPlayer = true
        )
        IconButton(
            onClick = {
                imageDataViewModel.onHandleMediaOperation(MediaProcessingEvent.ScreenOrientationEvent(  if(previewState.orientation == ScreenOrientation.UNSPECIFIED) ScreenOrientation.LANDSCAPE else ScreenOrientation.UNSPECIFIED))
            }
        ) {
            Icon(
                imageVector = Icons.Default.Autorenew,
                tint =  colorScheme.tertiary,
                contentDescription = "Rotate screen"
            )
        }
        playerState.mediaStates[mediaId]?.let { mediaUiState ->
            VideoControllerUi(
                state = mediaUiState,
                isFullControllerEnabled = true,
                onAction = viewModel::onPlayingIntent
            )
        }
    }
}
