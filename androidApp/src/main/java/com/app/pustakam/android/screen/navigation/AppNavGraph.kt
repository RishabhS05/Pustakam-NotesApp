package com.app.pustakam.android.screen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.app.pustakam.android.screen.AppViewModel
import com.app.pustakam.data.localdb.preferences.UserPreference


@Composable
fun AppNavGraph(navHostController: NavHostController = rememberNavController(),
                modifier: Modifier = Modifier) {
    val appViewModel: AppViewModel = viewModel()
   val user = appViewModel.authState.collectAsState(initial = UserPreference()).value
    val route = if (user.isAuthenticated) Route.Home else Route.Authentication
    NavHost(
        navController = navHostController,
        startDestination = route,
        modifier = modifier
    ) {
        AuthNavGraph(navHostController)
        HomeNavGraph(navHostController)
    }
}