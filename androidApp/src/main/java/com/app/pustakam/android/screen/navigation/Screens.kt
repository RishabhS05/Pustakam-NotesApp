package com.app.pustakam.android.screen.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

object Route {
    const val Home = "HOME"
    const val Search = "Search"
    const val Notification = "NOTIFICATION"
    const val Login = "LOGIN"
    const val Signup = "SIGNUP"
    const val Notes = "NOTES"
    const val Authentication = "AUTH"
}

sealed class BottomNavigationItem(
    val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector, val batchCount: Int? = null, val hasNotification: Boolean, val route: String
)

sealed class Screen(val route: String) {
    data object Authentication : Screen(route = Route.Authentication) {
        data object SignUpScreen : Screen(route = Route.Signup)
        data object LoginScreen : Screen(route = Route.Login)
    }

    data object HomeScreen : BottomNavigationItem(
        "Notes", selectedIcon = Icons.Filled.Add, unselectedIcon = Icons.Default.Add, hasNotification = false, route = Route.Home
    ) {
        object NotesScreen : BottomNavigationItem(
            "Notes", selectedIcon = Icons.Filled.Add, unselectedIcon = Icons.Default.Add, hasNotification = false, route = Route.Notes
        )

        object SearchScreen : BottomNavigationItem(
            "Search", selectedIcon = Icons.Default.Search, unselectedIcon = Icons.Default.Search, hasNotification = false, route = Route.Search
        )

        object NotificationScreen : BottomNavigationItem(
            "Notification", selectedIcon = Icons.Default.Notifications, unselectedIcon = Icons.Default.Notifications, hasNotification = false, route = Route.Notification
        )
    }
}

