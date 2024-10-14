
package com.app.pustakam.android.screen

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.screen.home.BottomNavigationItem
import com.app.pustakam.android.screen.home.HomeScreen
import com.app.pustakam.android.screen.login.LoginView
import com.app.pustakam.android.screen.notes.NotesView
import com.app.pustakam.android.screen.notes.NotificationView
import com.app.pustakam.android.screen.notes.SearchView
import com.app.pustakam.android.screen.signup.SignUpView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
              NavAppHost(navController = navController)
              { route ->
                  navigateTo(route, navController)
              }
            }
        }
    }
    @Composable
    fun BottomNavigationSetup(navController: NavController) {
        var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
        val bottomNavigation = listOf(
            BottomNavigationItem("Notes",
                selectedIcon = Icons.Filled.Add,
                unselectedIcon = Icons.Default.Add,
                hasNotification = false, route = Screen.NotesScreen),
            BottomNavigationItem("Search",
                selectedIcon = Icons.Default.Search,
                unselectedIcon = Icons.Default.Search,
                hasNotification = false, route = Screen.SearchScreen),
            BottomNavigationItem("Notification",
                selectedIcon = Icons.Default.Notifications,
                unselectedIcon = Icons.Default.Notifications,
                hasNotification = false, route = Screen.NotificationScreen)
        )
        NavigationBar {
            bottomNavigation.forEachIndexed { index, item ->
                NavigationBarItem(selected = navController.currentBackStackEntry?.destination?.route?.contains(item.route::class.qualifiedName.toString())==true,
                    onClick = {
                        selectedIndex = index
                        navigateBottomBar(destination = item.route, navController = navController)
                    },
                    label = { Text(text = item.title) },
                    icon = {
                        BadgedBox(badge = {
                            if (item.batchCount != null) {
                                Badge {
                                    Text(text = item.batchCount.toString())
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (index == selectedIndex) {
                                    item.selectedIcon
                                } else {
                                    item.unselectedIcon
                                }, contentDescription = item.title
                            )

                        }
                    })
            }
        }
    }
    private fun navigateBottomBar(navController: NavController, destination: Screen) {
        navController.clearBackStack<String>()
        navController.navigate(destination) {
            navController.graph.startDestinationRoute?.let { route ->
                popUpTo(Screen.NotesScreen::class.qualifiedName!!) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    private fun NavController.shouldShowBottomBar(
        currentDest: String?
    ) : Boolean{
        return currentDest?.contains(Screen.NotesScreen.toString()) == true
                || currentDest?.contains(Screen.NotificationScreen.toString()) == true
                || currentDest?.contains(Screen.SearchScreen.toString()) == true

    }
    private fun navigateTo(
        route: Screen,
        navController: NavController
    ) {
        navController.navigate(route)
    }

    @Composable
    fun NavAppHost(
        navController  : NavHostController = rememberNavController(),
        onNavigate: (Screen)-> Unit
    ) {
        var showBottomName by rememberSaveable {
            mutableStateOf(false)
        }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentScreen = backStackEntry?.destination?.route
        Scaffold(
            bottomBar = {
                 showBottomName = navController.
                 shouldShowBottomBar(currentDest = currentScreen)
                if (showBottomName)
                    BottomNavigationSetup( navController = navController)
            },
            topBar = {},
            floatingActionButton = {}) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.LoginScreen,
                modifier = Modifier.padding(padding)
            ) {
                composable<Screen.LoginScreen> {
                    LoginView(onNavigate = onNavigate){
                        println( navController.clearBackStack(route = Screen.LoginScreen::class.qualifiedName!!))
                    }
                }
                composable<Screen.SignUpScreen> {
                    SignUpView(onNavigate = onNavigate)
                }
                composable<Screen.HomeScreen> {
                    HomeScreen(onNavigate = onNavigate)
                }
                composable<Screen.NotesScreen> {
                    NotesView(onNavigate = onNavigate)
                }
                composable<Screen.SearchScreen> {
                    SearchView(onNavigate = onNavigate)
                }
                composable<Screen.NotificationScreen> {
                    NotificationView(onNavigate = onNavigate)
                }
            }
BackHandler {
    navController.popBackStack()
}
        }

    }

}

@Composable
fun GreetingView(text: String) {
    Text(text = text)
}

@Preview
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        GreetingView("Hello, Android!")
    }
}
