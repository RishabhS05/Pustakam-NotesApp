package com.app.pustakam.android.screen.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

sealed class BottomNavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val batchCount: Int? = null,
    val hasNotification: Boolean, val route: String
)
@Serializable
sealed class Screen(val route: String) {
    @Serializable
    data object Authentication : Screen(route = Route.Authentication) {
        @Serializable
        data object SignUpScreen : Screen(route = Route.Signup)
        @Serializable
        data object LoginScreen : Screen(route = Route.Login)
    }

    data object HomeScreen : BottomNavigationItem(
        "Notes", selectedIcon = Icons.Filled.Add, unselectedIcon = Icons.Default.Add, hasNotification = false, route = Route.Home
    ) {
        data object NotesScreen : BottomNavigationItem(
            "Notes", selectedIcon = Icons.Filled.Add, unselectedIcon = Icons.Default.Add, hasNotification = false, route = Route.Notes
        )

        data object SearchScreen : BottomNavigationItem(
            "Search", selectedIcon = Icons.Default.Search, unselectedIcon = Icons.Default.Search, hasNotification = false, route = Route.Search
        )

        data object NotificationScreen : BottomNavigationItem(
            "Notification", selectedIcon = Icons.Default.Notifications, unselectedIcon = Icons.Default.Notifications, hasNotification = false, route = Route.Notification
        )
    }
}

