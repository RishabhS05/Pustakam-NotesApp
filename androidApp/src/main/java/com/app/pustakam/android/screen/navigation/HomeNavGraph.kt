package com.app.pustakam.android.screen.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
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
            NotesEditorView( onBack = {
                navController.upPress()
            })
        }
        composable(
            route = Route.NotesEditor+"/{noteId}"
        ) {
           val noteId =  it.arguments?.getString("noteId") ?: ""
            NotesEditorView(id = noteId, onBack = {
                navController.upPress()
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
            SearchView(onNavigate = {})
        }
    }
}