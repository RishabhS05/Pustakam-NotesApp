package com.app.pustakam.android.screen.navigation

import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun BottomBar(
    navController: NavHostController = rememberNavController(),
) {
    val navigationScreen = listOf(
      Screen.HomeScreen.NotesScreen, Screen.HomeScreen.SearchScreen, Screen.HomeScreen.NotificationScreen
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        navigationScreen.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                label = {
                    Text(text = item.title, style = MaterialTheme.typography.bodyMedium)
                },
                icon = {
                    BadgedBox(badge = { }) { }

                    Icon(
                        imageVector = (if (item.route == currentRoute) item.selectedIcon else item.unselectedIcon),
                        contentDescription = item.title
                    )
                },
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}