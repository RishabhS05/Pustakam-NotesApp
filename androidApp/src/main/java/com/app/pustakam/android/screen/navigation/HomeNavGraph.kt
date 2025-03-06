package com.app.pustakam.android.screen.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.app.pustakam.android.hardware.camera.CameraPreviewScreen
import com.app.pustakam.android.hardware.camera.ImageDataViewModel
import com.app.pustakam.android.screen.noteEditor.NotesEditorView
import com.app.pustakam.android.screen.notes.list.NotesView
import com.app.pustakam.android.screen.notification.NotificationView
import com.app.pustakam.android.screen.search.SearchView


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
        ) {
            NotesEditorView( onBack = navController::upPress
            , onCameraPreview = { navController.navigateTo(Route.CameraPreview) })
        }
        composable(
            route = Route.NotesEditor+"/{noteId}"
        ) {
           val noteId =  it.arguments?.getString("noteId") ?: ""
            NotesEditorView(id = noteId, onBack =
                navController::upPress, onCameraPreview = { navController.navigateTo(Route.CameraPreview) })
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
        composable(
            route = Route.CameraPreview
        ) {
            val imageViewModel: ImageDataViewModel = viewModel<ImageDataViewModel>()
            CameraPreviewScreen(imageViewModel, navController::upPress)
        }
    }
}