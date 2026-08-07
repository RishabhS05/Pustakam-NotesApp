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
import com.app.pustakam.core.common.extensions.isNotnull

object Route {
    const val Home = "HOME"
    const val Search = "Search"
    const val Notification = "NOTIFICATION"
    const val Login = "LOGIN"
    const val Signup = "SIGNUP"
    const val Notes = "NOTES"
    const val NotesEditor = "NOTES_EDITOR"
    const val Authentication = "AUTH"
    const val ImagePreview = "IMAGE_PREVIEW"
    const val VideoPreview = "VIDEO_PREVIEW"
    const val Settings = "SETTINGS"
    const val BookReader = "BOOK_READER"   // 🔧 18-Jul-2026: page-flip book reader
    const val NoteBookReader = "NOTEBOOK_READER"   // 🔧 18-Jul-2026: page-flip book reader
}
@Stable
class PustakmNavController(
    val navController: NavHostController,
) {
    val navigationScreen = listOf(
        Screen.HomeScreen.NotesScreen,
        Screen.HomeScreen.SearchScreen,
        Screen.HomeScreen.NotificationScreen,
        Screen.HomeScreen.SettingsScreen
    )
    private val currentChrome
        get() = NavRouteRegistry.chromeFor(navController.currentBackStackEntry?.destination?.route)

    val shouldShowBottomBar get() = currentChrome.showsBottomBar
    val shouldShowTopBar get() = currentChrome.showsTopBar
    val shouldShowFloatingButton get() = currentChrome.showsFab

    fun upPress() {
        navController.navigateUp()
    }
    fun popBackInclusive (route: String ?= null){
        if(route.isNotnull()) while (navController.currentBackStackEntry?.destination?.route?.startsWith(route!!) == false) {
            upPress()
        }
        else upPress()
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
        if(route is String)
            navigateTo(route)
        else navController.navigate(route)
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
                popUpTo(findStartDestination(navController.graph).id) {
                    saveState = true
                }
            }
        }
    }
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


