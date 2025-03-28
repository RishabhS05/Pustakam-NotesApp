package com.app.pustakam.android.screen.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.app.pustakam.android.extension.sharedViewModel
import com.app.pustakam.android.hardware.camera.CameraStreamingScreen
import com.app.pustakam.android.hardware.camera.ImageDataViewModel
import com.app.pustakam.android.hardware.camera.ImageEditorScreen
import com.app.pustakam.android.hardware.camera.MediaProcessingEvent
import com.app.pustakam.android.hardware.video.VideoPreviewScreen
import com.app.pustakam.android.screen.noteEditor.NoteEditorViewModel
import com.app.pustakam.android.screen.noteEditor.NoteEditorScreen
import com.app.pustakam.android.screen.notes.list.NotesView
import com.app.pustakam.android.screen.notification.NotificationView
import com.app.pustakam.android.screen.search.SearchView
import com.app.pustakam.data.models.CameraData


fun NavGraphBuilder.HomeNavGraph(navController: PustakmNavController){
    navigation(
        route = Route.Home,
        startDestination = Route.Notes,
    ) {

        composable(
            route = Route.Notes
        ) {
            NotesView(onNavigateNote = {
                navController.navigateTo(Route.NotesEditor+"/${it.id}")
            })
        }
        composable(
            route = Route.NotesEditor
        ) {backStackEntry->
            val viewModel: NoteEditorViewModel = viewModel()
            val imageViewModel : ImageDataViewModel = backStackEntry
                .sharedViewModel<ImageDataViewModel>(navController.navController)
            NoteEditorScreen(
                noteEditorViewModel = viewModel,
                onBack = navController::upPress,
                imageDataViewModel = imageViewModel,
                navigateTo = navController::navigateTo)
        }
        /** just wanted to use navigation with args style to remember this way of passing data*/
        composable(
            route = Route.NotesEditor+"/{noteId}"
        ) {   backStackEntry->
           val noteId =  backStackEntry.arguments?.getString("noteId") ?: ""
            val viewModel: NoteEditorViewModel = viewModel()
            val imageViewModel : ImageDataViewModel = backStackEntry
                .sharedViewModel<ImageDataViewModel>(navController.navController)
            NoteEditorScreen(id = noteId,
                noteEditorViewModel = viewModel,
                imageDataViewModel = imageViewModel,
                onBack =
                navController::upPress, navController::navigateTo)
        }
        composable(
            route = Route.Notification
        ) {
            NotificationView(onNavigate = {})
        }
        composable(
            route = Route.Search
        ) {
            SearchView(onNavigate = {})
        }

        /** Routes For handling videos and Images*/
        composable(route = Route.ImagePreview) {backStackEntry->
            val imageViewModel : ImageDataViewModel = backStackEntry
                .sharedViewModel<ImageDataViewModel>(navController.navController)
            ImageEditorScreen(imageViewModel,     navController::popBackInclusive )
        }
        composable(route = Route.VideoPreview) {backStackEntry->
            val imageViewModel : ImageDataViewModel = backStackEntry
                .sharedViewModel<ImageDataViewModel>(navController.navController)
            VideoPreviewScreen(imageViewModel)
        }
        composable<CameraData> { backStackEntry ->
            val data = backStackEntry.toRoute<CameraData>()
            val imageViewModel : ImageDataViewModel = backStackEntry
                .sharedViewModel<ImageDataViewModel>(navController.navController)
            imageViewModel.onHandleMediaOperation(MediaProcessingEvent.SetNoteId(data.noteId))
            CameraStreamingScreen(imageViewModel,navController::upPress,
                { navController.navigateTo(it) })
        }
    }
}
