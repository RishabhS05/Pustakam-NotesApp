package com.app.pustakam.android.screen

import android.content.Intent
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
import com.app.pustakam.android.fileimport.IncomingShare
import com.app.pustakam.android.screen.navigation.AppNavGraph
import com.app.pustakam.android.screen.navigation.BottomBar
import com.app.pustakam.android.screen.navigation.PustakmNavController
import com.app.pustakam.android.screen.navigation.Route
import com.app.pustakam.android.screen.navigation.rememberPustakmNavController
import com.app.pustakam.android.theme.ThemeMode
import com.app.pustakam.android.widgets.fabWidget.AddNewNoteFAB
import com.app.pustakam.core.database.localdb.preferences.IAppPreferences
import com.app.pustakam.core.common.extensions.isNotnull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setIntent(intent)
        // 🔧 17-Aug-2026: read the launch intent BEFORE the first composition, so a shared file is
        //   already buffered when the nav graph decides where to send the user. savedInstanceState
        //   != null means a rotation/restore replaying the SAME intent — importing it twice there
        //   is the bug this guard exists for.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
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
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()
    }
  // 🔧 17-Aug-2026: Open With / Share — type detection is NOT done here any more; MimeCatalog
  //   already classifies every uri inside the shared ImportCoordinator.
  private fun handleIntent(intent: Intent?) {
      IncomingShare.accept(intent)
  }
}
@Composable
private fun rememberGranthThemeMode(): ThemeMode {
    val prefs = koinInject<IAppPreferences>()
    val modeFlow = remember(prefs) { prefs.userPreferencesFlow.map { ThemeMode.from(it.themeMode) } }
    val mode by modeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    return mode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUi(navController: PustakmNavController = rememberPustakmNavController()) {
    AppNavGraph(
        navHostController = navController,
    )
}

