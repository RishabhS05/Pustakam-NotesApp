package com.app.pustakam.android.screen.navigation


import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.findViewTreeViewModelStoreOwner

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.NavType   // 🔧 18-Jul-2026: optional contentId arg
import androidx.navigation.navArgument   // 🔧 18-Jul-2026: optional contentId arg
import androidx.navigation.navDeepLink   // 🔧 18-Jul-2026: widget → book deep link
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.app.pustakam.android.screen.notebookReader.NoteBookReaderScreen   // 🔧 18-Jul-2026: book reader
import com.app.pustakam.android.hardware.camera.CameraStreamingScreen
import com.app.pustakam.android.hardware.camera.ImageDataViewModel
import com.app.pustakam.android.hardware.camera.ImageEditorScreen
import com.app.pustakam.android.hardware.camera.MediaProcessingEvent
import com.app.pustakam.android.hardware.video.VideoPreviewScreen
import com.app.pustakam.android.screen.bookReading.BookReaderScreen
import com.app.pustakam.android.screen.noteEditor.NoteEditorScreen
import com.app.pustakam.android.screen.noteEditor.NoteEditorViewModel
import com.app.pustakam.android.screen.notes.list.NotesView
import com.app.pustakam.android.screen.notification.NotificationView
import com.app.pustakam.android.screen.search.SearchView
import com.app.pustakam.android.screen.settings.SettingsScreen
import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.model.models.CameraData
import com.app.pustakam.core.model.models.response.notes.getMediaUrl


fun NavGraphBuilder.HomeNavGraph(navController: PustakmNavController){
    navigation(
        route = Route.Home,
        startDestination = Route.Notes,
    ) {
        composable(
            route = Route.Notes
        ) {
            NotesView(onNavigateNote = { noteId ->
                navController.navigateTo(Route.NotesEditor+"/${noteId}")
            })
        }
        composable(
            route = Route.Notification
        ) {
            NotificationView(onNavigate = {})
        }
        composable(
            route = Route.Search
        ) {
            SearchView(onNavigateNote = { noteId ->
                navController.navigateTo(Route.NotesEditor + "/${noteId}")
            })
        }
        composable(
            route = Route.Settings
        ) {
            SettingsScreen(onNavigate = navController::navigateTo,  onBack = navController::upPress)
        }
        composable(
            route = Route.NoteBookReader + "/{noteId}?contentId={contentId}&single={single}",
            arguments = listOf(
                navArgument("contentId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                },
                navArgument("single") { type = NavType.BoolType; defaultValue = false },
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "pustakam://book/{noteId}" })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            val contentId = backStackEntry.arguments?.getString("contentId")
            val single = backStackEntry.arguments?.getBoolean("single") ?: false
            val imageViewModel: ImageDataViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ComponentActivity)
            NoteBookReaderScreen(
                noteId = noteId,
                startContentId = contentId,
                singleContent = single,
                onBack = navController::upPress,
                // 📖 01-Aug-2026: a document card inside the note opens the dedicated reader
                onOpenDocument = { media ->
                    navController.navigateTo(Route.BookReader + "/$noteId?contentId=${media.id}")
                },
                onOpenMedia = { media ->
                    imageViewModel.onSetMediaToPreview(media.getMediaUrl(), media.type, mediaId = media.id)
                    when (media.type) {
                        ContentType.IMAGE, ContentType.GIF -> navController.navigateTo(Route.ImagePreview)
                        ContentType.VIDEO -> navController.navigateTo(Route.VideoPreview)
                        else -> Unit
                    }
                },
            )
        }
        composable(
            route = Route.BookReader + "/{noteId}?contentId={contentId}",
            arguments = listOf(
                navArgument("contentId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            val contentId = backStackEntry.arguments?.getString("contentId")
            BookReaderScreen(
                bookId = contentId,
                onBack = navController::upPress
            )
        }

        /** Routes For handling videos and Images*/
        composable(route = Route.ImagePreview) {
            val activity = requireNotNull(LocalView.current.findViewTreeViewModelStoreOwner()) {
                "No ViewModelStoreOwner found"
            }
            val imageViewModel: ImageDataViewModel = viewModel(viewModelStoreOwner = activity)
            ImageEditorScreen(imageViewModel,     navController::popBackInclusive )
        }
        composable(route = Route.VideoPreview) {
            val activity = requireNotNull(LocalView.current.findViewTreeViewModelStoreOwner()) {
                "No ViewModelStoreOwner found"
            }
            val imageViewModel: ImageDataViewModel = viewModel(viewModelStoreOwner = activity)
            VideoPreviewScreen(imageViewModel, onDismiss = navController::upPress)
        }
        composable<CameraData> { backStackEntry ->
            val data = backStackEntry.toRoute<CameraData>()
            val activity = requireNotNull(LocalView.current.findViewTreeViewModelStoreOwner()) {
                "No ViewModelStoreOwner found"
            }

            val imageViewModel: ImageDataViewModel = viewModel(viewModelStoreOwner =activity)
            imageViewModel.onHandleMediaOperation(MediaProcessingEvent.SetNoteId(data.noteId))
            CameraStreamingScreen(imageViewModel,navController::upPress,
                { navController.navigateTo(it) })
        }
    }
}

fun NavGraphBuilder.EditorNavGraph(navController: PustakmNavController){

    navigation(
        route = Route.NotesEditorRouter,
        startDestination = Route.NotesEditor,
    ){

        composable(
            route = Route.NotesEditor
        ) {
            val viewModel: NoteEditorViewModel = viewModel()
            val activity = requireNotNull(LocalView.current.findViewTreeViewModelStoreOwner()) {
                "No ViewModelStoreOwner found"
            }
            val imageViewModel: ImageDataViewModel =viewModel(activity)
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
            val activity = requireNotNull(LocalView.current.findViewTreeViewModelStoreOwner()) {
                "No ViewModelStoreOwner found"
            }
            val imageViewModel: ImageDataViewModel =viewModel(viewModelStoreOwner =activity)
            NoteEditorScreen(id = noteId,
                noteEditorViewModel = viewModel,
                imageDataViewModel = imageViewModel,
                onBack = navController::upPress, navController::navigateTo)
        }
    }

}