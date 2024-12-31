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
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomBar(
    navController: PustakmNavController = rememberPustakmNavController(),
) {

    NavigationBar {
        val currentRoute = navController.currentRoute()
        navController.navigationScreen.forEach { item ->
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
                    navController.navigateToBottomBarRoute(item.route)
                },
            )
        }
    }
}