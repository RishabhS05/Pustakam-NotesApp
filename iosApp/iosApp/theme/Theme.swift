
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
     }
     
     // Define Spacing
     struct Spacing {
         static let small: CGFloat = 8
         static let medium: CGFloat = 16
         static let large: CGFloat = 24
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
