package com.app.pustakam.android.screen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.app.pustakam.domain.repositories.BaseRepository

@Composable
fun AppNavGraph(navHostController: NavHostController = rememberNavController(),
                modifier: Modifier = Modifier) {
    NavHost(
        navController = navHostController,
        startDestination = if (BaseRepository.userID.isNullOrEmpty()) Route.Authentication else Route.Home,
        modifier = modifier
    ) {
        AuthNavGraph(navHostController)
        HomeNavGraph(navHostController)
    }
}