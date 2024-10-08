package com.app.pustakam.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MyApplicationTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val lightTheme = lightColorScheme(
        primary = Color(0xFFFFFFFF),
        secondary = Color(0xFF9D8563),
        tertiary = Color(0xFFF5BF4F),
        outline = Color(0xFF9D8563),
        background = Color(0xFFFFFFFF)
    )
    var darkTheme = darkColorScheme(
        primary = Color(0xFFFFFFFF),
        secondary = Color(0xFF9D8563),
        tertiary = Color(0xFFF5BF4F),
        outline = Color(0xFF9D8563),
    )
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
    val typography = Typography(
        titleLarge = TextStyle(fontSize = 20.sp),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
        )
    )
    val shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(0.dp)
    )

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