package com.app.pustakam.android

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import com.app.pustakam.android.theme.typography
import darkTheme
import lightTheme
import shapes

@Composable
fun MyApplicationTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
val context = LocalContext.current
    val colorScheme = when {
        //dynamic ui
//        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ->{
//            if (isDarkTheme) dynamicDarkColorScheme(context)
//            else dynamicLightColorScheme(context)
//        }
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

val extendedLight = ExtendedColorScheme(
    extra = ColorFamily(
        backgroundVariant = Color(0xFFEEEEEE), // Example light variant
    ),
)
val extendedDark = ExtendedColorScheme(
    extra = ColorFamily(
        backgroundVariant = Color(0xFF333333), // Example dark variant
    ),
)

val LocalExColorScheme = staticCompositionLocalOf { ExtendedColorScheme() }