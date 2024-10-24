package com.app.pustakam.android.screen.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.app.pustakam.android.screen.login.LoginView
import com.app.pustakam.android.screen.signup.SignUpView

fun NavGraphBuilder.AuthNavGraph(
    navController: NavHostController
) {
    navigation(
        route = Route.Authentication,
        startDestination = Route.Login,
    ) {
        composable(
            route = Route.Login
        ) {
            LoginView(
                onNavigateToHome = {
                    navController.clearBackStack<Screen.Authentication>()
                    navController.navigate(Route.Home) {
                        launchSingleTop =true
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Route.Signup)
                },
            )
        }
        composable(
            route = Route.Signup
        ) {
            SignUpView(onNavigate = {
                navController.navigateUp()
            })
        }
    }
}