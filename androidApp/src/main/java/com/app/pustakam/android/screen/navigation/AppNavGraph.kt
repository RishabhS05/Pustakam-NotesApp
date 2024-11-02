package com.app.pustakam.android.screen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.app.pustakam.android.screen.AppViewModel


@Composable
fun AppNavGraph(navHostController: NavHostController = rememberNavController(),
                modifier: Modifier = Modifier) {
     val appViewModel: AppViewModel = viewModel()
   val user =  appViewModel.appUIState.collectAsState().value
    val route = if (user.userId.isNullOrEmpty() && user.token.isNullOrEmpty()) Route.Authentication else Route.Home
    NavHost(
        navController = navHostController,
        startDestination = route,
        modifier = modifier
    ) {
        AuthNavGraph(navHostController)
        HomeNavGraph(navHostController)
    }
}