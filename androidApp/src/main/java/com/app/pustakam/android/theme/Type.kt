package com.app.pustakam.android.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.app.pustakam.android.R

private val Montserrat = FontFamily(
    Font(R.font.montserrat_light, FontWeight.Light),
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold)
)

private val Karla = FontFamily(
    Font(R.font.karla_regular, FontWeight.Normal),
    Font(R.font.karla_bold, FontWeight.Bold)
)

// 🎨 20-Jul-2026 — Granth spec §3 typography: two families. Display = serif (titles, note headings,
//   reader body); Body/UI = sans (platform default, Roboto). We use the OS-provided serif family so no
//   new font binary is needed (spec's recommended Noto Serif can be dropped in later as an R.font).
//   Android nuance (spec §8): page titles switch to Roboto 700 sans; note/reader content stays serif.
val GranthSerif = FontFamily.Serif
val GranthSans = FontFamily.SansSerif
// 🎨 20-Jul-2026 — retuned to Granth spec §3 type scale. Roles are unchanged (every
//   MaterialTheme.typography.* reference keeps resolving); only family/size/weight now follow the spec.
//   Serif = display/titles/reader body; Sans (Roboto default) = UI body/meta; per §8 the top page
//   title (headlineLarge) uses Roboto 700 while note titles stay serif.
val typography = Typography(
    // Display XL — page title: 30/33, 600, serif (note-title editor uses this too)
    displayLarge = TextStyle(
        fontFamily = GranthSerif,
        fontSize = 30.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 33.sp,
        letterSpacing = (-0.3).sp
    ),
    // Display L — note title editor: 26/30, 600, serif
    displayMedium = TextStyle(
        fontFamily = GranthSerif,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 30.sp
    ),
    displaySmall = TextStyle(
        fontFamily = GranthSerif,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp
    ),
    // Android page title — Roboto 700 sans (spec §8 platform nuance)
    headlineLarge = TextStyle(
        fontFamily = GranthSans,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 33.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GranthSerif,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    ),
    // Section title: 17–20/24, 600, serif
    headlineSmall = TextStyle(
        fontFamily = GranthSerif,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GranthSerif,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GranthSerif,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    ),
    // Card title: 14.5/18, 600, serif
    titleSmall = TextStyle(
        fontFamily = GranthSerif,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp
    ),
    // Reader body: 17/31, 400, serif
    bodyLarge = TextStyle(
        fontFamily = GranthSerif,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 31.sp
    ),
    // Body: 15/26, 400, sans
    bodyMedium = TextStyle(
        fontFamily = GranthSans,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GranthSans,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),
    // Meta / caption: 11–12/16, 500, sans
    bodySmall = TextStyle(
        fontFamily = GranthSans,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GranthSans,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GranthSans,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)