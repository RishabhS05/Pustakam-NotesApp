package com.app.pustakam.android.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.screen.navigation.AppNavGraph
import com.app.pustakam.android.screen.navigation.BottomBar
import com.app.pustakam.android.screen.navigation.PustakmNavController
import com.app.pustakam.android.screen.navigation.Route
import com.app.pustakam.android.screen.navigation.rememberPustakmNavController
import com.app.pustakam.android.theme.ThemeMode
import com.app.pustakam.android.widgets.fabWidget.AddNewNoteFAB
import com.app.pustakam.core.database.localdb.preferences.BasePreferences
import com.app.pustakam.core.common.extensions.isNotnull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 🎨 22-Jul-2026 — Granth spec §6: the persisted Appearance pick drives the whole app.
            //   SYSTEM defers to isSystemInDarkTheme() so scheduled Dark Mode keeps working.
            val themeMode = rememberGranthThemeMode()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.AMOLED -> true
            }
            MyApplicationTheme(isDarkTheme = isDark, isAmoled = themeMode.isAmoled) {
                AppUi()
            }
        }
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()
    }
}


// 🎨 22-Jul-2026 — reads the persisted Appearance pick from the shared DataStore (same
//   "granth.themeMode" key iOS uses). Emits on every change, so picking a tile in Settings
//   retints the app immediately and the choice survives restart.
@Composable
private fun rememberGranthThemeMode(): ThemeMode {
    val prefs = koinInject<BasePreferences>()
    val modeFlow = remember(prefs) { prefs.userPreferencesFlow.map { ThemeMode.from(it.themeMode) } }
    val mode by modeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    return mode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUi(navController: PustakmNavController = rememberPustakmNavController()) {
    val currentRoute = navController.currentRoute()
    Scaffold(topBar = {
        if (currentRoute.isNotnull() && navController.shouldShowTopBar) TopAppBar(title = {
            Text(text = currentRoute!!, textAlign = TextAlign.Center)
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background)
        )
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
    }
        ) { paddingValues ->
        AppNavGraph(
            modifier = Modifier.padding(paddingValues),
            navController,
        )
    }

}

