package com.app.pustakam.android.screen.settings

import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.android.screen.bookReader.ReadingMode
import com.app.pustakam.android.theme.ThemeMode
import com.app.pustakam.data.localdb.preferences.BasePreferences
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.get

// 🎨 22-Jul-2026 — Granth spec §6 Settings state. Appearance is persisted through the shared
//   DataStore (same "granth.themeMode" key iOS uses); the remaining rows are real state holders
//   ready for their features to land behind them.
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // 📖 23-Jul-2026: reader layout — page curl vs continuous scroll (also toggled in the reader)
    val readingMode: ReadingMode = ReadingMode.PAGE,
    // Editor & Reading
    val editorFont: String = "Iowan",
    val editorFontSubtitle: String = "Display serif · 16pt",
    val markdownShortcuts: Boolean = true,
    val focusModeDimming: Boolean = true,
    val versionHistorySubtitle: String = "Keep 30 days of snapshots",
    // Sync & Backup
    val cloudSync: Boolean = true,
    val cloudSyncSubtitle: String = "Last synced 2 min ago · 1.2 GB",
    val autoBackup: Boolean = true,
    val offlineMode: Boolean = false,
    // More
    val language: String = "English",
    // Profile
    val profileInitial: String = "R",
    val profileName: String = "Rishabh S",
    val profileSubtitle: String = "rishabhshri2795@gmail.com · 4 devices",
    val profileBadge: String? = "PRO"
)

class SettingsViewModel : BaseViewModel() {

    private val userPrefs = get<BasePreferences>()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // 🎨 restore the persisted Appearance pick so the tiles match the live theme on open
        viewModelScope.launch(Dispatchers.IO) {
            userPrefs.userPreferencesFlow.collect { pref ->
                _uiState.update { it.copy(themeMode = ThemeMode.from(pref.themeMode)) }
            }
        }
        // 📖 23-Jul-2026: same value the reader's toolbar toggle writes — the two stay in sync
        viewModelScope.launch(Dispatchers.IO) {
            userPrefs.readingModeFlow.collect { mode ->
                _uiState.update { it.copy(readingMode = ReadingMode.from(mode)) }
            }
        }
    }

    // 📖 23-Jul-2026: flip reading mode from Settings
    fun onReadingModeChange(scrolling: Boolean) {
        val next = if (scrolling) ReadingMode.SCROLL else ReadingMode.PAGE
        _uiState.update { it.copy(readingMode = next) }
        viewModelScope.launch(Dispatchers.IO) { userPrefs.setReadingMode(next.key) }
    }

    // 🎨 persist the tile pick; the app root observes the same flow and retints immediately
    fun onThemeModeSelected(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch(Dispatchers.IO) { userPrefs.setThemeMode(mode.key) }
    }

    // 🎨 toggle rows — local state until each feature lands
    fun onMarkdownShortcutsChange(enabled: Boolean) =
        _uiState.update { it.copy(markdownShortcuts = enabled) }

    fun onFocusModeDimmingChange(enabled: Boolean) =
        _uiState.update { it.copy(focusModeDimming = enabled) }

    fun onCloudSyncChange(enabled: Boolean) =
        _uiState.update { it.copy(cloudSync = enabled) }

    fun onAutoBackupChange(enabled: Boolean) =
        _uiState.update { it.copy(autoBackup = enabled) }

    fun onOfflineModeChange(enabled: Boolean) =
        _uiState.update { it.copy(offlineMode = enabled) }

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {}

    override suspend fun logoutUserForcefully() {}

    override fun clearError() {}
}
