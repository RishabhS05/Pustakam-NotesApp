package com.app.pustakam.android.screen.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.app.pustakam.android.screen.notes.NotesView
import com.app.pustakam.android.screen.notes.NotificationView
import com.app.pustakam.android.screen.notes.SearchView

fun NavGraphBuilder.HomeNavGraph(navController: NavHostController){
    navigation(
        route = Route.Home,
        startDestination = Route.Notes,
    ) {
        composable(
            route = Route.Notes
        ) {
            NotesView(onNavigate = {})
        }
        composable(
            route = Route.Notification
        ) {
            NotificationView(onNavigate = {

            })
        }
        composable(
            route = Route.Search
        ) {
            SearchView(onNavigate = {})
        }
    }
}