import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 🎨 20-Jul-2026 — radius scale from Granth spec §4: sm 8 (chips/code), md 14 (cards/inputs),
//   lg 22 (search/hero), xl 30 (sheets). Material's Shapes exposes small/medium/large + extras.
val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // sm — chips / code
    small = RoundedCornerShape(8.dp),        // sm — chips / code
    medium = RoundedCornerShape(14.dp),      // md — cards / inputs
    large = RoundedCornerShape(22.dp),       // lg — search / hero
    extraLarge = RoundedCornerShape(30.dp)   // xl — sheets
)

// 🎨 20-Jul-2026 — named radius tokens (spec §4) for direct use where a Shapes role doesn't map 1:1
//   (e.g. FAB = 20 on Android). Additive.
val radiusSm = 8.dp
val radiusMd = 14.dp
val radiusLg = 22.dp
val radiusXl = 30.dp
val radiusFabAndroid = 20.dp
