package com.app.pustakam.android.screen.navigation


import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.app.pustakam.android.screen.login.LoginView
import com.app.pustakam.android.screen.signup.SignUpView
fun NavGraphBuilder.AuthNavGraph(
    navController: PustakmNavController
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
                    navController.goToHomeScreen()
                },
                onNavigateToSignUp = {
                    navController.navigateTo(Route.Signup)
                },
            )
        }
        composable(
            route = Route.Signup
        ) {
            SignUpView(onNavigate = {
                navController.upPress()
            })
        }
    }
}