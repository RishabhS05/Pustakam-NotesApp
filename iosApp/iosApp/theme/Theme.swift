
import SwiftUI
struct Theme {
    //Colors
    struct Colors {
        static let primary = Color("primary")
        static let secondary = Color("secondary")
        static let tertiary = Color("tertiary")
        static let background = Color("background")
        static let onSurface = Color("onSurface")
        static let placeholderText = Color("placeholderText")
        static let textPrimary = Color("textPrimary")
        static let textSecondary = Color("textSecondary")
        static let dashTitleColor = Color("dashTitleColor")
        static let selectedTabColor = Color("selectedTabColor")
        static let checkboxColor = Color("checkboxColor")
        static let progressIndicatorBG = Color("progressIndicatiorBG")
        static let sliderThumbColor = Color("sliderThumbColor")
        static let sliderTextColor = Color("sliderTextColor")
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
