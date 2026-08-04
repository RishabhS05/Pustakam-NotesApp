package com.app.pustakam.android.screen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import com.app.pustakam.android.screen.AppViewModel
import com.app.pustakam.android.screen.navigation.NavRouteRegistry.buildAll


@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navHostController: PustakmNavController = rememberPustakmNavController(),
                ) {
    val appViewModel: AppViewModel = viewModel()
    val isAuthenticated = appViewModel.isAuthenticated
        .collectAsStateWithLifecycle(initialValue = false).value
    val route = if (isAuthenticated) Route.Home else Route.Authentication
    NavHost(
        navController = navHostController.navController,
        startDestination = route,
        modifier = modifier
    ) {
        buildAll(navHostController)
    }
}
