package com.app.pustakam.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import com.app.pustakam.android.theme.typography
import amoledTheme
import darkTheme
import lightTheme
import shapes

// 🎨 20-Jul-2026 — Granth spec §2 / §6 Appearance: three theme modes (Light/Dark/AMOLED). Additive —
//   the existing isDarkTheme boolean path is preserved; AMOLED is opt-in via the new isAmoled flag.
@Composable
fun MyApplicationTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false, // 🎨 opt-in true-black OLED mode (settings "AMOLED" tile)
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        //dynamic ui
//        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ->{
//            if (isDarkTheme) dynamicDarkColorScheme(context)
//            else dynamicLightColorScheme(context)
//        }
        isAmoled -> amoledTheme          // 🎨 true-black OLED variant
        isDarkTheme -> darkTheme
        else -> lightTheme
    }
    val extendedColorScheme = if (isDarkTheme) extendedDark else extendedLight



    CompositionLocalProvider(
        LocalExColorScheme provides extendedColorScheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

@Immutable
data class ColorFamily(
    val backgroundVariant: Color,
)

@Immutable
data class ExtendedColorScheme(
    val extra: ColorFamily = extendedLight.extra,
)

// 🎨 20-Jul-2026 — warm surface-2 variants (spec §2.2 `surface-2`) replace the placeholder grays.
val extendedLight = ExtendedColorScheme(
    extra = ColorFamily(
        backgroundVariant = Color(0xFFF6EEDB), // Granth surface-2 (light)
    ),
)
val extendedDark = ExtendedColorScheme(
    extra = ColorFamily(
        backgroundVariant = Color(0xFF2A2219), // Granth surface-2 (dark)
    ),
)

val LocalExColorScheme = staticCompositionLocalOf { ExtendedColorScheme() }