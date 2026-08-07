package com.app.pustakam.android.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

// Extension function to get a shared ViewModel from a NavBackStackEntry
@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    navController: NavHostController,
): T {
    val navGraphRoute = destination.parent?.route ?: return viewModel()
    val parentEntry = remember(this) { navController.getBackStackEntry(navGraphRoute) }
    return viewModel(parentEntry)
}

/**
 * Scopes to the NavHost's ROOT graph, so the instance is shared across nav graphs.
 *
 * 🔧 07-Aug-2026 — [sharedViewModel] scopes to `destination.parent`, which means a destination in
 * HomeNavGraph and one in EditorNavGraph get DIFFERENT instances. The camera lives in Home and the
 * editor that consumes the captured paths lives in Editor, so ImageDataViewModel must be scoped
 * here instead or the handoff silently drops the media.
 *
 * The root graph's route is null on a NavHost, so this resolves by id.
 */
@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.appSharedViewModel(
    navController: NavHostController,
): T {
    val rootEntry = remember(this) {
        runCatching { navController.getBackStackEntry(navController.graph.id) }.getOrNull()
    } ?: return sharedViewModel(navController)
    return viewModel(rootEntry)
}
