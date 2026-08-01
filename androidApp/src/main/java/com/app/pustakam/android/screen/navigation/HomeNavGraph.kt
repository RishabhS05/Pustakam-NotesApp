package com.app.pustakam.android.screen.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.NavType   // 🔧 18-Jul-2026: optional contentId arg
import androidx.navigation.navArgument   // 🔧 18-Jul-2026: optional contentId arg
import androidx.navigation.navDeepLink   // 🔧 18-Jul-2026: widget → book deep link
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.app.pustakam.android.screen.notebookReader.NoteBookReaderScreen   // 🔧 18-Jul-2026: book reader
import com.app.pustakam.android.extension.sharedViewModel
import com.app.pustakam.android.hardware.camera.CameraStreamingScreen
import com.app.pustakam.android.hardware.camera.ImageDataViewModel
import com.app.pustakam.android.hardware.camera.ImageEditorScreen
import com.app.pustakam.android.hardware.camera.MediaProcessingEvent
import com.app.pustakam.android.hardware.video.VideoPreviewScreen
import com.app.pustakam.android.screen.bookReading.BookReaderScreen
import com.app.pustakam.android.screen.noteEditor.NoteEditorViewModel
import com.app.pustakam.android.screen.noteEditor.NoteEditorScreen
import com.app.pustakam.android.screen.notebookReader.NoteBookReaderViewModel
import com.app.pustakam.android.screen.notes.list.NotesView
import com.app.pustakam.android.screen.notification.NotificationView
import com.app.pustakam.android.screen.search.SearchView
import com.app.pustakam.android.screen.settings.SettingsScreen
import com.app.pustakam.core.model.models.CameraData


fun NavGraphBuilder.HomeNavGraph(navController: PustakmNavController){
    navigation(
        route = Route.Home,
        startDestination = Route.Notes,
    ) {
        composable(
            route = Route.Notes
        ) {
            // 🔧 15-Jul-2026 Summary query: the list emits the note ID (cards are summaries now)
            NotesView(onNavigateNote = { noteId ->
                navController.navigateTo(Route.NotesEditor+"/${noteId}")
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
                onBack = navController::upPress, navController::navigateTo)
        }
        composable(
            route = Route.Notification
        ) {
            NotificationView(onNavigate = {})
        }

        composable(
            route = Route.Search
        ) {
            // 🔧 15-Jul-2026 Phase 2.2: search results open the tapped note in the editor
            SearchView(onNavigateNote = { noteId ->
                navController.navigateTo(Route.NotesEditor + "/${noteId}")
            })
        }
        composable(
            route = Route.Settings
        ) {
            SettingsScreen(onNavigate = navController::navigateTo,  onBack = navController::upPress)
        }
        // 🔧 18-Jul-2026: NEW — page-flip book reader; deep-linked from the home-screen widget
        composable(
            route = Route.NoteBookReader + "/{noteId}?contentId={contentId}&single={single}",
            // 🔧 18-Jul-2026: contentId is optional — nullable + default keeps plain routes valid
            arguments = listOf(
                navArgument("contentId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                },
                // 🔧 19-Jul-2026: single=true → book contains ONLY the tapped file's pages
                navArgument("single") { type = NavType.BoolType; defaultValue = false },
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "pustakam://book/{noteId}" })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            val contentId = backStackEntry.arguments?.getString("contentId")
            val single = backStackEntry.arguments?.getBoolean("single") ?: false
            val notebookReaderViewModel: NoteBookReaderViewModel = viewModel()
            notebookReaderViewModel.load( noteId, contentId, single)
            NoteBookReaderScreen(
                bookReaderViewModel = notebookReaderViewModel,
                singleContent = single,
                onBack = navController::upPress,
                // 📖 01-Aug-2026: a document card inside the note opens the dedicated reader
                onOpenDocument = { media ->
                    navController.navigateTo(Route.BookReader + "/$noteId?contentId=${media.id}")
                },
            )
        }
        // 📖 01-Aug-2026: DOCUMENT reader — one file opened on its own. No `single` flag and no
        //   widget deep link: that link opens the whole note above.
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
            // 📖 the screen owns the load (LaunchedEffect) so it can't refire on recomposition
            BookReaderScreen(
                bookId = contentId,
                noteId = noteId,
                onBack = navController::upPress
            )
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
            VideoPreviewScreen(imageViewModel, onDismiss = navController::upPress)
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
