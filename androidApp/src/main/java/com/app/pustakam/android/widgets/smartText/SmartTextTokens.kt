package com.app.pustakam.android.widgets.smartText

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// standalone palette for the editor only — intentionally not wired to MyApplicationTheme
@Immutable
data class SmartTextColors(
    val page: Color,
    val surface: Color,
    val toolbar: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val highlight: Color,
    val divider: Color,
    val codeBackground: Color,
    val selectionHandle: Color,
    val searchHit: Color,
    val searchHitActive: Color
)

private val DarkColors = SmartTextColors(
    page = Color(0xFF161311),
    surface = Color(0xFF1C1815),
    toolbar = Color(0xFF2A2420),
    onSurface = Color(0xFFEFE7DC),
    onSurfaceMuted = Color(0xFFA2968A),
    accent = Color(0xFFE9A33C),
    onAccent = Color(0xFF1C1815),
    accentSoft = Color(0xFF3B2F1C),
    highlight = Color(0xFF5C4A1E),
    divider = Color(0xFF3A322B),
    codeBackground = Color(0xFF232019),
    selectionHandle = Color(0xFFE9A33C),
    searchHit = Color(0xFF4A4128),
    searchHitActive = Color(0xFF8A6A1E)
)

private val LightColors = SmartTextColors(
    page = Color(0xFFFBF6EE),
    surface = Color(0xFFFFFFFF),
    toolbar = Color(0xFFFFFDF9),
    onSurface = Color(0xFF2B2723),
    onSurfaceMuted = Color(0xFF6B6259),
    accent = Color(0xFFE2971F),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFF7E7C8),
    highlight = Color(0xFFFBE6A0),
    divider = Color(0xFFE7DED0),
    codeBackground = Color(0xFFF5EFE4),
    selectionHandle = Color(0xFFE2971F),
    searchHit = Color(0xFFF3E3B4),
    searchHitActive = Color(0xFFEFC968)
)

object SmartTextTokens {

    /**
     * Dark/light follows the *active app theme*, not the OS setting, so the in-app
     * System / Light / Dark / AMOLED tiles drive the editor too. Only the brightness of the
     * theme is read — the palette itself stays independent of the app's colours.
     */
    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val colors: SmartTextColors
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) DarkColors else LightColors

    val baseFontSize: TextUnit = 16.sp
    val codeFontSize: TextUnit = 14.sp
    val blockSpacing: Dp = 2.dp
    val indentStep: Dp = 20.dp
    val toolbarHeight: Dp = 48.dp
    val toolbarCorner: Dp = 12.dp
    val buttonCorner: Dp = 8.dp
    val buttonSize: Dp = 40.dp
    val minTouchTarget: Dp = 44.dp
    val horizontalPadding: Dp = 16.dp
    val quoteBarWidth: Dp = 3.dp
}
