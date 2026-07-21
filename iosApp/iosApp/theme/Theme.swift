
import SwiftUI
struct Theme {
    //Colors
    struct Colors {
        static let primary = Color("primary")
        static let secondary = Color("secondary")
        static let error = Color("error")
        static let onError = Color("onError")
        static let onSurface = Color("onSurface")
        static let outline = Color("outline")
        static let surface = Color("surface")
        static let background = Color("background")

        // 🎨 20-Jul-2026 — Granth spec §2 palette. All resolve from Assets.xcassets/colors/* colorsets
        //   (light+dark defined per set). Additive: the eight tokens above are kept and now also carry
        //   the warm values. Brand primitives (spec §2.1):
        static let ivory = Color("ivory")
        static let parchment = Color("parchment")
        static let sand = Color("sand")
        static let saffron = Color("saffron")
        static let saffronDeep = Color("saffronDeep")
        static let copper = Color("copper")
        static let gold = Color("gold")
        static let forest = Color("forest")          // success / sync-complete
        static let indigo = Color("indigo")          // links / info
        static let ink = Color("ink")                // darkest text
        // Semantic tokens (spec §2.2):
        static let surface2 = Color("surface2")
        static let surfaceRaise = Color("surfaceRaise")  // nav / sheets
        static let text2 = Color("text2")            // secondary text
        static let text = Color("text")            // secondary text
        static let text3 = Color("text3")            // tertiary / meta
        static let border = Color("border")
        static let accent = Color("accent")
        static let link = Color("link")

        // Signature gradient (spec §2.1): 135° saffron → copper. Used on FAB, primary button, hero, seal.
        static let accentGradient = LinearGradient(
            colors: [saffron, copper],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )

//        static let baseWhite = Color(hex: "#FFFFFF")
//        static let primaryLight = Color(hex: "#97D2FF")
//        static let inversePrimaryLight = Color(hex: "#92CDFA")
//        static let secondaryLight = Color(hex: "#246CBC")
//        static let onSecondaryLight = baseWhite
//        static let tertiaryLight = Color(hex: "#835422")
//        static let onTertiaryLight = Color(hex: "#FFFFFF")
//        static let errorLight = Color(hex: "error")
//        static let onErrorLight = Color(hex: "#FFFFFF")
//        static let onPrimaryLight = Color(hex: "#FFFFFF")
//        static let primaryContainerLight = Color(hex: "#97D2FF")
//        static let onPrimaryContainerLight = Color(hex: "#125B82")
//        static let secondaryContainerLight = Color(hex: "#007CB2")
//        static let onSecondaryContainerLight = Color(hex: "#FCFCFF")
//        static let tertiaryContainerLight = Color(hex: "#FFBF83")
//        static let onTertiaryContainerLight = Color(hex: "#794B1A")
//        static let errorContainerLight = Color(hex: "#FFDAD6")
//        static let onErrorContainerLight = Color(hex: "#93000A")
//        static let inverseSurface = Color(hex: "#2E3134")
//        static let onInverseSurface = Color(hex: "#EFF1F4")
//        static let surface = Color(hex: "#F8F9FD")
//        static let onSurface = Color(hex: "#191C1F")
//        static let onSurfaceVar = Color(hex: "#41484E")
//        static let surfaceDim = Color(hex: "#D8DADE")
//        static let surfaceBright = surface
//        static let outline = Color(hex: "#71787F")
//        static let outlineVarient = Color(hex: "#C0C7CF")
//
//        // MARK: - Dark Theme
//        static let primary = Color(hex: "primary")
//        static let onPrimaryDark = Color(hex: "#00344E")
//        static let primaryContainerDark = Color(hex: "#97D2FF")
//        static let onPrimaryContainerDark = Color(hex: "#125B82")
//        static let inversePrimaryDark = Color(hex: "#21648B")
//        static let secondaryDark = Color(hex: "#8ACEFF")
//        static let onSecondaryDark = Color(hex: "#00344E")
//        static let tertiaryDark = Color(hex: "#FFE3CC")
//        static let onTertiaryDark = Color(hex: "#4B2800")
//        static let errorDark = Color(hex: "#FFB4AB")
//        static let onErrorDark = Color(hex: "#690005")
//        static let secondaryContainerDark = Color(hex: "#039ADC")
//        static let onSecondaryContainerDark = Color(hex: "#002C43")
//        static let tertiaryContainerDark = Color(hex: "#FFBF83")
//        static let onTertiaryContainerDark = Color(hex: "#794B1A")
//        static let errorContainerDark = Color(hex: "#93000A")
//        static let onErrorContainerDark = Color(hex: "#FFDAD6")
//        static let inverseSurfaceDark = Color(hex: "#E1E2E6")
//        static let onInverseSurfaceDark = Color(hex: "#2E3134")
//        static let surfaceDark = Color(hex: "#111416")
//        static let onSurfaceDark = Color(hex: "#191C1F")
//        static let onSurfaceVarDark = Color(hex: "#41484E")
//        static let surfaceDimDark = Color(hex: "#111416")
//        static let surfaceBrightDark = Color(hex: "#37393C")
//        static let outlineDark = Color(hex: "#8A9199")
//        static let outlineVarientDark = Color(hex: "#41484E")
    }
    
    // Fonts
    struct Fonts {
        static let gtWpRegular = "GTWalsheimPro-Regular"
        static let gtWpMedium = "GTWalsheimPro-Medium"
        static let gtWpLight = "GTWalsheimPro-Light"
        static let gtWpThin = "GTWalsheimPro-Thin"

        static let largeTitle = Font.custom(gtWpMedium, size: 24)
        static let topbarTitle = Font.custom(gtWpMedium, size: 22)

        static let title = Font.custom(gtWpMedium, size: 18)
        static let selectTitle = Font.custom(gtWpMedium, size: 18)
        static let body = Font.custom(gtWpMedium,size: 18)
        static let body2 = Font.custom(gtWpRegular, size: 16)
        static let caption = Font.custom(gtWpLight,size: 14)

        // 🎨 20-Jul-2026 — Granth spec §3 type scale. Display = serif (system serif design, HIG-native,
        //   no font binary needed; swap for Noto Serif later); Body/UI = SF Pro (system). iOS titles keep
        //   serif with -0.02em tracking (apply via .tracking(-0.4) at the call site). Additive to the gtWp* set.
        static let displayXL = Font.system(size: 30, weight: .semibold, design: .serif)   // page title 30/33
        static let displayL  = Font.system(size: 26, weight: .semibold, design: .serif)   // note-title editor 26/30
        static let sectionTitle = Font.system(size: 18, weight: .semibold, design: .serif) // 17–20/24
        static let cardTitle = Font.system(size: 15, weight: .semibold, design: .serif)   // 14.5/18
        static let readerBody = Font.system(size: 17, weight: .regular, design: .serif)   // 17/31
        static let bodyText = Font.system(size: 15, weight: .regular, design: .default)   // 15/26 sans
        static let metaCaption = Font.system(size: 12, weight: .medium, design: .default) // 11–12/16
        static let codeText = Font.system(size: 13, weight: .regular, design: .monospaced) // 12–13/20
     }

     // Define Spacing
     // 🎨 20-Jul-2026 — Granth spec §4 spacing (4pt base). small/medium/large kept for back-compat;
     //   named steps + screen gutter added. Additive.
     struct Spacing {
         static let small: CGFloat = 8
         static let medium: CGFloat = 16
         static let large: CGFloat = 24
         static let xs: CGFloat = 4
         static let sm: CGFloat = 8
         static let md: CGFloat = 12
         static let lg: CGFloat = 16
         static let xl: CGFloat = 20
         static let xxl: CGFloat = 24
         static let xxxl: CGFloat = 32
         static let gutter: CGFloat = 20      // screen gutter
     }

     // 🎨 20-Jul-2026 — Granth spec §4 radius. FAB is a full circle on iOS (spec §8), so use .infinity/Circle().
     struct Radius {
         static let sm: CGFloat = 8    // chips / code
         static let md: CGFloat = 14   // cards / inputs
         static let lg: CGFloat = 22   // search / hero
         static let xl: CGFloat = 30   // sheets
     }

     // 🎨 20-Jul-2026 — Granth spec §4 elevation. Use as .shadow(color:radius:x:y:).
     struct Elevation {
         // shadow-1 (cards)
         static let card = Color.black.opacity(0.06)
         static let cardRadius: CGFloat = 6
         static let cardY: CGFloat = 2
         // shadow-2 (hero / dialog)
         static let hero = Color.black.opacity(0.12)
         static let heroRadius: CGFloat = 20
         static let heroY: CGFloat = 6
         // shadow-fab
         static let fab = Color(red: 166/255, green: 92/255, blue: 52/255).opacity(0.40)
         static let fabRadius: CGFloat = 24
         static let fabY: CGFloat = 8
     }
}
struct Images {
   static let left = "chevron.backward.circle.fill"
   static let right = "chevron.forward.circle.fill"
   static let arrowRight = "chevron.forward"
   static let arrowLeft = "chevron.backward"
   static let avatarFemale = "avatar-female"
   static let home = "home"
   static let other = "other"
   static let gym = "gym"
   static let checkCircleFilled =  "checkmark.circle.fill"
   static let checkCircle =  "circle"
   static let diamond =  "diamond"
   static let workoutImage =  "workout-image"
                          
}

