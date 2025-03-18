
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
val brown3= Color(0XFFc57a2f)
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
val baseWhite = Color(0xFFFFFFFF)


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
    primary = earthyLight, // dark
    primaryContainer = brown10, //light
    onPrimary =  earthyLight2, // light or white
    onPrimaryContainer = DarkBrown3, // darker
    inversePrimary = brown0,// dark

    secondary = earthBrown, //another dark
    secondaryContainer = brown11,// light
    onSecondaryContainer = earthGreenDark10,
    onSecondary = earthGreenDark11,
    outline = earthBrown1,
    outlineVariant = brown13,
    background = OffWhite2,
    onBackground = Gray,

    surface = earthyLight,
    onSurface = earthtyDarker,
    surfaceContainer = earthyLight,
    surfaceTint = DarkBrown4,
    surfaceVariant =  earthyGrayLight3,
    surfaceDim = OffWhite,
    surfaceBright = OffWhite2,
    scrim = Black,
    onError = orange20,
    onErrorContainer = orange80,
    errorContainer = orange10,
    error = orange30,
)

var darkTheme = darkColorScheme(
    primary = brown8 , //light
    primaryContainer = DarkBrown4, //darker then primary color
    onPrimary = brown13,// darker or black
    onPrimaryContainer = brown11, // lighter then primary

    secondary = DarkBrown0, // other then primary same shade
    secondaryContainer = DarkBrown3, //lighter then onSecondary
    onSecondary = DarkBrown4 ,// darker
    onSecondaryContainer = brown13, // same as onPrimaryContainer shade might different

    inversePrimary = brown12, // darker then primary or opposite

    onSurface = brown13, //light
    surface = DarkBrown4, //darker

    onSurfaceVariant = brown12,
    surfaceTint = brown13,
    surfaceVariant =  DarkBrown3, //  mild light then surface

    surfaceDim = Gray3,
    outline = Gray2,
    surfaceBright = Gray4,
    scrim = Black,
    background = Gray3,

    onBackground = OffWhite,
    error = orange30,
    errorContainer = orange90,
    onError = orange20,
    onErrorContainer = orange80,
)

