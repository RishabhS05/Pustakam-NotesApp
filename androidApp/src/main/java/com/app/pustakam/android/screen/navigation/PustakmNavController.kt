package com.app.pustakam.android.screen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

object Route {
    const val Home = "HOME"
    const val Search = "Search"
    const val Notification = "NOTIFICATION"
    const val Login = "LOGIN"
    const val Signup = "SIGNUP"
    const val Notes = "NOTES"
    const val NotesEditor = "NOTES_EDITOR"
    const val Authentication = "AUTH"
    const val CameraPreview = "CAMERA_PREVIEW"
}
@Stable
class PustakmNavController(
    val navController: NavHostController,
) {
    val navigationScreen = listOf(
        Screen.HomeScreen.NotesScreen,
        Screen.HomeScreen.SearchScreen,
        Screen.HomeScreen.NotificationScreen

    )
    val shouldShowBottomBar
        get() = when (navController.currentBackStackEntry?.destination?.route) {
            Route.Notes,
            Route.Search,
            Route.Notification,
                -> true

            else -> false
        }
    val shouldShowTopBar
        get() = when (navController.currentBackStackEntry?.destination?.route) {
            Route.Notes,
            Route.Search,
            Route.Notification,
                -> true
            else -> false
        }
    val shouldShowFloatingButton
        get() = when (navController.currentBackStackEntry?.destination?.route) {
            Route.Notes -> true
            else -> false
        }

    fun upPress() {
        navController.navigateUp()
    }

    fun goToHomeScreen() {
        navController.clearBackStack<Screen.Authentication>()
        navController.navigate(Route.Home) {
            launchSingleTop = true
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route)
    }
    fun navigateTo(route: Any){
        navController.navigate(route)
    }
@Composable
fun currentRoute(): String? {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        return currentRoute
    }

    fun navigateToBottomBarRoute(route: String) {
        if (route != navController.currentDestination?.route) {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
                // Pop up backstack to the first destination and save state. This makes going back
                // to the start destination when pressing back in any other bottom tab.
                popUpTo(findStartDestination(navController.graph).id) {
                    saveState = true
                }
            }
        }
    }
//    fun navigateToNoteEditor(snackId: Long, origin: String, from: NavBackStackEntry) {
//        // In order to discard duplicated navigation events, we check the Lifecycle
//        if (from.lifecycleIsResumed()) {
//            navController.navigate("${Route.NotesEditor}/$snackId?origin=$origin")
//        }
//    }
}

@Composable
fun rememberPustakmNavController(
    navController: NavHostController = rememberNavController()
): PustakmNavController = remember(navController) {
    PustakmNavController(navController)
}

private fun NavBackStackEntry.lifecycleIsResumed() = this.lifecycle.currentState == Lifecycle.State.RESUMED

private val NavGraph.startDestination: NavDestination?
    get() = findNode(startDestinationId)

/**
 * Copied from similar function in NavigationUI.kt
 *
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation/navigation-ui/src/main/java/androidx/navigation/ui/NavigationUI.kt
 */
private tailrec fun findStartDestination(graph: NavDestination): NavDestination {
    return if (graph is NavGraph) findStartDestination(graph.startDestination!!) else graph
}


