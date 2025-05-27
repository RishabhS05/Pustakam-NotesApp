
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
// app new theme color


val baseWhite = Color(0xFFFFFFFF)
val primaryLight = Color(0xff97d2ff)
val inversePrimaryLight = Color(0xff92cdfa)
val secondaryLight = Color(0xff246cbc)
val onSecondaryLight = baseWhite
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

val earthtyDarker = Color(0xFF523D35)
val earthyLight = Color(0xFFBBA58F)
val earthyLight2 = Color(0xFFE7DACD)
val earthyGrayLight3= Color(0xFFEFEFE9)
val earthBrown1 = Color(0xFFA68A64)
val earthBrown = Color(0xFF936639)
val earthGreenDark10 = Color(0xFF7F4F24)
val earthGreenDark11 = Color(0xFF582E0E)
val green1 = Color(0xffbcf825)
val green2 = Color(0xff61800b)
val green3 = Color(0xff3a5102)
val blue1 = Color(0xff7295f3)
val blue2 = Color(0xff2d3e75)
val blue3 = Color(0xff223c98)
val blue4 = Color(0xff0f1e4a)

val notegradient = listOf(orange90,orange50)

val bgradient = listOf(earthGreenDark11,earthGreenDark10, earthtyDarker)
val sideGreenGradient = listOf(green1,green2,green3)
val sideblueGradient = listOf(blue1,blue2,blue3,blue4)
val sideBrownGradient = listOf(DarkBrown1.copy(alpha = .5f),DarkBrown2.copy(alpha = .5f))
val lightTheme = lightColorScheme(
    primary = primaryLight, // dark
    primaryContainer = primaryContainerLight, //light
    onPrimary =  onPrimaryLight, // light or white
    onPrimaryContainer = onPrimaryContainerLight, // darker
    inversePrimary =inversePrimaryLight ,// dark
    tertiary =  tertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    onTertiary = onTertiaryLight,
    secondary = secondaryLight, //another dark
    secondaryContainer = secondaryContainerLight,// light
    onSecondaryContainer = onSecondaryContainerLight,
    onSecondary = onSecondaryLight,
    outline = outline,
    outlineVariant = outlineVarient,
    background = OffWhite2,
    onBackground = Gray,
    surface = surface,
    inverseSurface = inverseSurface,
    onSurfaceVariant = onSurfaceVar,
    onSurface = onSurface,
    surfaceContainer = earthyLight,
    surfaceDim = surfaceDim,
    surfaceBright = surfaceBright,
    inverseOnSurface = onInverseSurface,
    scrim = Black,
    onError = onErrorLight,
    onErrorContainer = onErrorContainerLight,
    errorContainer = errorContainerLight,
    error = errorLight,
)

var darkTheme = darkColorScheme(
    primary = primaryDark , //light
    primaryContainer = primaryContainerDark, //darker then primary color
    onPrimary = baseWhite,// darker or black
    onPrimaryContainer = baseWhite, // lighter then primary

    secondary = secondaryDark, // other then primary same shade
    secondaryContainer = secondaryContainerDark, //lighter then onSecondary
    onSecondary = onSecondaryDark ,// darker
    onSecondaryContainer = onSecondaryContainerDark, // same as onPrimaryContainer shade might different
    onTertiaryContainer = onTertiaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    inversePrimary = inversePrimaryDark, // darker then primary or opposite
    onSurface = baseWhite, //light
    surface = surfaceDark, //darker
    onSurfaceVariant = onSurfaceVarDark,
    surfaceDim = surfaceDimDark,
    outline = outlineDark,
    surfaceBright = surfaceBrightDark,
    scrim = Black,
    background = Gray3,
    onBackground = OffWhite,
    error = errorDark,
    errorContainer = errorContainerDark,
    onError = onErrorDark,
    onErrorContainer = onErrorContainerDark,
)

