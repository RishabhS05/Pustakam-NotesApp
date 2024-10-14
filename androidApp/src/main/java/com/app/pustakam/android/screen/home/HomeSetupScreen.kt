package com.app.pustakam.android.screen.home


import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.vector.ImageVector
import com.app.pustakam.android.screen.Route

import com.app.pustakam.android.screen.Screen
data class BottomNavigationItem(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector, val batchCount: Int? = null, val hasNotification: Boolean, val route : Screen )

@Composable
fun HomeScreen(onNavigate : (Screen) -> Unit) {

}