package com.app.pustakam.android.theme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import kotlin.math.roundToInt

// app new theme color


val baseWhite = Color(0xFFFFFFFF)
val primaryLight = Color(0xff97d2ff)
val inversePrimaryLight = Color(0xff92cdfa)
val secondaryLight = Color(0xff246cbc)
val onSecondaryLight = baseWhite
val actionIconTintColor= Color.White
val PaperInk = Color(0xFF3E2F1C)
val CoverColor = Color(0xFF5D4033)

val tertiaryLight = Color(0xFF835422)
val onTertiaryLight = Color(0xFFffffff)
val errorLight = Color(0xFFba1a1a)
val onErrorLight = Color(0xFFffffff)
val onPrimaryLight = Color(0xFFffffff)
val primaryContainerLight = Color(0xFF97d2ff)
val onPrimaryContainerLight = Color(0xFF125b82)
val secondaryContainerLight = Color(0xFF007cb2)
val onSecondaryContainerLight = Color(0xFFfcfcff)
val tertiaryContainerLight = Color(0xFFffbf83)
val onTertiaryContainerLight = Color(0xFF794b1a)
val errorContainerLight = Color(0xFFffdad6)
val onErrorContainerLight = Color(0xFF93000a)
val inverseSurface = Color(0xFF2e3134)
val onInverseSurface = Color(0xFFeff1f4)
val surface = Color(0xFFf8f9fd)
val onSurface = Color(0xFF191c1f)
val onSurfaceVar = Color(0xFF41484e)
val surfaceDim = Color(0xFFd8dade)
val surfaceBright = surface
val outline = Color(0xFF71787f)
val outlineVarient = Color(0xFFc0c7cf)

val primaryDark = Color(0xffd4eaff)
val onPrimaryDark = Color(0xFF00344e)
val primaryContainerDark = Color(0xFF97d2ff)
val onPrimaryContainerDark = Color(0xFF125b82)
val inversePrimaryDark = Color(0xFF21648b)
val secondaryDark = Color(0xff8aceff)
val onSecondaryDark = Color(0xff00344e)
val tertiaryDark = Color(0xFFffe3cc)
val onTertiaryDark = Color(0xFF4b2800)
val errorDark = Color(0xFFffb4ab)
val onErrorDark = Color(0xFF690005)

val secondaryContainerDark = Color(0xFF039adc)
val onSecondaryContainerDark = Color(0xFF002c43)
val tertiaryContainerDark = Color(0xFFffbf83)
val onTertiaryContainerDark = Color(0xFF794b1a)
val errorContainerDark = Color(0xFF93000a)
val onErrorContainerDark = Color(0xFFffdad6)
val inverseSurfaceDark = Color(0xFFe1e2e6)
val onInverseSurfaceDark = Color(0xFF2e3134)
val surfaceDark = Color(0xFF111416)

val onSurfaceDark = Color(0xFF191c1f)
val onSurfaceVarDark = Color(0xFF41484e)
val surfaceDimDark = Color(0xFF111416)
val surfaceBrightDark = Color(0xFF37393c)
val outlineDark = Color(0xFF8a9199)
val outlineVarientDark = Color(0xFF41484e)
val bookmarkDefault = Color(0xFFFF6F46)
val PaperColor = Color(0xFFFAF3E3)

//darkbrown
val DarkBrown0 = Color(0xFF382320)
val DarkBrown1 = Color(0xFF321f1c)
val DarkBrown2 = Color(0xFF2b1b19)
val DarkBrown3 = Color(0xFF251715)
val DarkBrown4 = Color(0xFF1f1412)
val DarkBrown5 = Color(0xFF19100e)
val DarkBrown6 = Color(0xFF130c0b)
val DarkBrown7 = Color(0xFF0c0807)

//brown
val brown0 = Color(0XFF9e6225)
val brown1 = Color(0XFFab6a28)
val brown2 = Color(0XFFb8722b)
val brown4 = Color(0XFFcf8336)
val brown5 = Color(0XFFd28b43)
val brown6 = Color(0XFFd59350)
val brown7 = Color(0XFFd38f49)
val brown8 = Color(0XFFe0af7e)
val brown9 = Color(0XFFe3b78b)
val brown10 = Color(0XFFe6c098)
val brown11 = Color(0XFFe9c8a6)
val brown12 = Color(0XFFeed4b9)
val brown13 = Color(0XFFf0e4e0)



//orange
val orange10 = Color (0xFF3e1202)
val orange20 = Color (0xFF6d1f04)
val orange30 = Color (0xFFcc3b08)
val orange40 = Color (0xFFf76e3e)
val orange50 = Color (0xFFf9926d)
val orange80 = Color (0xFFfbb59c)
val orange90 = Color (0xFFfcd8cc)

val Black = Color(0xFF000000)
val OffWhite = Color(0xFFDED8E1)
val OffWhite2 = Color(0xFFFEF7FF)
val Gray = Color(0xFF1D1B20)
val Gray2 = Color(0xFF79747E)
val Gray3 = Color(0xFF151219)
val Gray4 = Color(0xFF3A383F)


val green1 = Color(0xffbcf825)
val green2 = Color(0xff61800b)
val green3 = Color(0xff3a5102)
val blue1 = Color(0xff7295f3)
val blue5 = Color(0xFF8FA9FC)
val blue2 = Color(0xff2d3e75)
val blue3 = Color(0xff223c98)
val blue4 = Color(0xff0f1e4a)
val surfaceContainerLight = blue1.copy(0.1f)
val notegradient = listOf(orange90,orange50)
val sideGreenGradient = listOf(green1,green2,green3)
val allGradient = listOf(orange10,orange30,orange50,blue1,blue3,blue4,green2,green3,DarkBrown1,DarkBrown2)
val sideblueGradient = listOf(blue1,blue2,blue5,blue3,blue4)
val sideBrownGradient = listOf(DarkBrown1.copy(alpha = .5f),DarkBrown2.copy(alpha = .5f))

// 🎨 20-Jul-2026 — Granth design-spec palette (docs/pustakm-design-spec.md §2). Brand primitives are
//   mode-independent; nothing above was removed, these are additive and the schemes below now map onto them.
// Brand primitives (spec §2.1)
val GranthIvory = Color(0xFFFBF7EC)        // lightest paper
val GranthParchment = Color(0xFFF2E8D2)    // page background (light)
val GranthSand = Color(0xFFE6D7B8)         // warm sand, depth
val GranthSaffron = Color(0xFFE8941A)      // primary accent / energy
val GranthSaffronDeep = Color(0xFFD17C0A)  // pressed / gradient end
val GranthCopper = Color(0xFFA65C34)       // secondary accent, seals, dividers
val GranthGold = Color(0xFFC9A227)         // tertiary / highlights / PRO
val GranthForest = Color(0xFF2F5D4A)       // success, sync-complete, checkboxes
val GranthIndigo = Color(0xFF33407C)       // links, info, work category
val GranthInk = Color(0xFF2A2118)          // darkest text / dark-mode base

// Signature gradient (spec §2.1): 135° saffron → copper
val granthAccentGradient = listOf(GranthSaffron, GranthCopper)

// Semantic tokens — LIGHT mode (spec §2.2)
val GranthBgLight = Color(0xFFEDE2C8)
val GranthSurfaceLight = Color(0xFFFBF7EC)
val GranthSurface2Light = Color(0xFFF6EEDB)
val GranthSurfaceRaiseLight = Color(0xFFFFFFFF)
val GranthTextLight = Color(0xFF2A2118)
val GranthText2Light = Color(0xFF6B5B45)
val GranthText3Light = Color(0xFF9C8A6E)
val GranthBorderLight = Color(0x1A2A2118)   // rgba(42,33,24,.10)
val GranthAccentLight = Color(0xFFA65C34)
val GranthLinkLight = Color(0xFF33407C)

// Semantic tokens — DARK mode (spec §2.2) — warm ink-brown, never cool gray
val GranthBgDark = Color(0xFF16120D)
val GranthSurfaceDark = Color(0xFF221B14)
val GranthSurface2Dark = Color(0xFF2A2219)
val GranthSurfaceRaiseDark = Color(0xFF2E261C)
val GranthTextDark = Color(0xFFEFE4CE)
val GranthText2Dark = Color(0xFFB7A684)
val GranthText3Dark = Color(0xFF7E7158)
val GranthBorderDark = Color(0x1AEFE4CE)    // rgba(239,228,206,.10)
val GranthAccentDark = Color(0xFFD98A53)
val GranthLinkDark = Color(0xFF8E9BD8)

// Semantic tokens — AMOLED mode (spec §2.2) — true black bg, warm near-black surfaces
val GranthBgAmoled = Color(0xFF000000)
val GranthSurfaceAmoled = Color(0xFF0C0A07)
val GranthSurface2Amoled = Color(0xFF13100B)
val GranthSurfaceRaiseAmoled = Color(0xFF171309)
val GranthTextAmoled = Color(0xFFF0E6D0)
val GranthText2Amoled = Color(0xFFAE9D7C)
val GranthText3Amoled = Color(0xFF6E6248)
val GranthBorderAmoled = Color(0x17F0E6D0)  // rgba(240,230,208,.09)
val GranthAccentAmoled = Color(0xFFE8941A)
val GranthLinkAmoled = Color(0xFF94A1E0)
// 🎨 20-Jul-2026 — rewired onto the Granth palette (spec §2). Material 3 roles mapped so the whole app
//   adopts saffron/copper/parchment: primary = saffron accent, secondary = copper, tertiary = gold,
//   surfaces = warm paper. Old blue tokens above are kept for any direct references.
val lightTheme = lightColorScheme(
    primary = GranthSaffron,                       // primary accent / energy
    primaryContainer = GranthSand,                 // soft warm container
    onPrimary = GranthIvory,                       // text on saffron
    onPrimaryContainer = GranthInk,                // text on sand
    inversePrimary = GranthSaffronDeep,            // pressed / gradient end
    tertiary = GranthGold,                         // highlights / PRO
    tertiaryContainer = Color(0xFFF0E4B9),         // light gold container
    onTertiaryContainer = Color(0xFF574406),
    onTertiary = GranthInk,
    secondary = GranthCopper,                      // seals, dividers, secondary accent
    secondaryContainer = Color(0xFFEBD6C6),        // light copper container
    onSecondaryContainer = Color(0xFF3A1E0E),
    onSecondary = GranthIvory,
    outline = GranthText3Light,                    // hairline / meta stroke
    outlineVariant = GranthBorderLight,
    background = GranthBgLight,                     // parchment page bg
    onBackground = GranthTextLight,
    surface = GranthSurfaceLight,                  // ivory card surface
    inverseSurface = GranthInk,
    onSurfaceVariant = GranthText2Light,           // secondary text
    onSurface = GranthTextLight,
    surfaceContainer = GranthSurface2Light,
    surfaceDim = GranthSand,
    surfaceBright = GranthSurfaceRaiseLight,        // nav / sheets raise
    inverseOnSurface = GranthIvory,
    scrim = Black,
    onError = onErrorLight,
    onErrorContainer = onErrorContainerLight,
    errorContainer = errorContainerLight,
    error = errorLight,
)

var darkTheme = darkColorScheme(
    primary = GranthAccentDark,                    // warm accent (dark)
    primaryContainer = GranthSurface2Dark,
    onPrimary = GranthInk,
    onPrimaryContainer = GranthTextDark,
    secondary = GranthCopper,
    secondaryContainer = Color(0xFF3A1E0E),
    onSecondary = GranthIvory,
    onSecondaryContainer = Color(0xFFEBD6C6),
    onTertiaryContainer = Color(0xFFF0E4B9),
    tertiary = GranthGold,
    onTertiary = GranthInk,
    tertiaryContainer = Color(0xFF574406),
    inversePrimary = GranthSaffron,
    onSurface = GranthTextDark,                     // warm off-white
    surface = GranthSurfaceDark,                    // warm near-black
    onSurfaceVariant = GranthText2Dark,
    surfaceContainer = GranthSurface2Dark,
    surfaceDim = GranthBgDark,
    outline = GranthText3Dark,
    outlineVariant = GranthBorderDark,
    surfaceBright = GranthSurfaceRaiseDark,
    scrim = Black,
    background = GranthBgDark,                       // warm ink-brown page bg
    onBackground = GranthTextDark,
    inverseSurface = GranthIvory,
    inverseOnSurface = GranthInk,
    error = errorDark,
    errorContainer = errorContainerDark,
    onError = onErrorDark,
    onErrorContainer = onErrorContainerDark,
)

// 🎨 20-Jul-2026 — AMOLED scheme (spec §2.2): true-black bg for OLED battery savings, warm near-black
//   surfaces. Additive: opt-in from the theme wrapper when the user picks the AMOLED tile (settings §6).
val amoledTheme = darkColorScheme(
    primary = GranthAccentAmoled,
    primaryContainer = GranthSurface2Amoled,
    onPrimary = Black,
    onPrimaryContainer = GranthTextAmoled,
    secondary = GranthCopper,
    secondaryContainer = Color(0xFF2A1409),
    onSecondary = GranthIvory,
    onSecondaryContainer = Color(0xFFEBD6C6),
    tertiary = GranthGold,
    onTertiary = Black,
    tertiaryContainer = Color(0xFF3E2F04),
    onTertiaryContainer = Color(0xFFF0E4B9),
    inversePrimary = GranthSaffron,
    onSurface = GranthTextAmoled,
    surface = GranthSurfaceAmoled,
    onSurfaceVariant = GranthText2Amoled,
    surfaceContainer = GranthSurface2Amoled,
    surfaceDim = GranthBgAmoled,
    outline = GranthText3Amoled,
    outlineVariant = GranthBorderAmoled,
    surfaceBright = GranthSurfaceRaiseAmoled,
    scrim = Black,
    background = GranthBgAmoled,
    onBackground = GranthTextAmoled,
    inverseSurface = GranthIvory,
    inverseOnSurface = GranthInk,
    error = errorDark,
    errorContainer = errorContainerDark,
    onError = onErrorDark,
    onErrorContainer = onErrorContainerDark,
)

fun Color.toHexString(): String {
    val alpha = (this.alpha * 255).roundToInt()
    val red = (this.red * 255).roundToInt()
    val green = (this.green * 255).roundToInt()
    val blue = (this.blue * 255).roundToInt()
    return if (alpha == 255) String.format("#%02X%02X%02X", red, green, blue)
    else String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
}
fun String.toColor() : Color {
    return Color(this.toColorInt())
}