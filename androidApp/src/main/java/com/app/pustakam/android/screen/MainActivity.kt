package com.app.pustakam.android.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.screen.navigation.AppNavGraph
import com.app.pustakam.android.screen.navigation.BottomBar
import com.app.pustakam.android.screen.navigation.PustakmNavController
import com.app.pustakam.android.screen.navigation.Route
import com.app.pustakam.android.screen.navigation.rememberPustakmNavController
import com.app.pustakam.android.widgets.fabWidget.AddNewNoteFAB
import com.app.pustakam.extensions.isNotnull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val navController = rememberPustakmNavController()
                AppUi(navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUi(navController: PustakmNavController = rememberPustakmNavController()) {
    val currentRoute = navController.currentRoute()
    Scaffold(topBar = {
        if (currentRoute.isNotnull() && navController.shouldShowTopBar) TopAppBar(title = {
            Text(text = currentRoute!!, textAlign = TextAlign.Center)
        })
    }, bottomBar = {
        if (navController.shouldShowBottomBar) BottomBar(navController = navController)
    }, floatingActionButton = {
        if (navController.shouldShowFloatingButton) {
            when (currentRoute) {
                Route.Notes -> AddNewNoteFAB {
                    navController.navigateTo(Route.NotesEditor)
                }
            }
        }
    }) { paddingValues ->
        AppNavGraph(
            navController, modifier = Modifier.padding(paddingValues)
        )
    }

}

