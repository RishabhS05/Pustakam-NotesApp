package com.app.pustakam.android.screen

import kotlinx.serialization.Serializable
@Serializable
 sealed class Screen(val route : Route) {
     @Serializable
      data object SignUpScreen : Screen(route = Route.Signup)
     @Serializable
     data object  LoginScreen : Screen(route = Route.Login)
     @Serializable
    data object  NotesScreen : Screen(route = Route.Notes)
     @Serializable
    data object HomeScreen : Screen(route = Route.Home)
     @Serializable
    data object  SearchScreen : Screen(route = Route.Search)
     @Serializable
      data object NotificationScreen : Screen(route = Route.Notification){}
 }

enum class Route {
    Home,Search,Notification,Login,Signup,Notes
}