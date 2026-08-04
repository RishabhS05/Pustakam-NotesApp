package com.app.pustakam.android.screen.navigation

import com.app.pustakam.core.common.config.AuthConfig
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import com.app.pustakam.android.screen.AppViewModel
import com.app.pustakam.core.database.localdb.preferences.UserPreference


@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navHostController: PustakmNavController = rememberPustakmNavController(),
                ) {
    val appViewModel: AppViewModel = viewModel()
   val user = appViewModel.authState
       .collectAsStateWithLifecycle(initialValue = UserPreference()).value
    val route = if (AuthConfig.BYPASS_AUTH || user.isAuthenticated) Route.Home else Route.Authentication
    NavHost(
        navController = navHostController.navController,
        startDestination = route,
        modifier = modifier
    ) {
        AuthNavGraph(navHostController)
        HomeNavGraph(navHostController)
    }
}