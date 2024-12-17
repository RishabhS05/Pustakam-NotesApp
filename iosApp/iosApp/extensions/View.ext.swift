
import SwiftUI

extension View {
    
 func onFirstAppear(perform action: @escaping () -> Void) -> some View {
     self.modifier(OnFirstAppearModifier(perform: action))
    }
}

struct OnFirstAppearModifier : ViewModifier{
    let perform : () -> Void
    @State var isFirstAppear : Bool = true
    func body(content: Content) -> some View{
        content.onAppear{
            if isFirstAppear{
                isFirstAppear = false
                self.perform()
            }
        }
    }
}



extension View {
    func swipe(
        up: @escaping (() -> Void) = {},
        down: @escaping (() -> Void) = {},
        left: @escaping (() -> Void) = {},
        right: @escaping (() -> Void) = {}
    ) -> some View {
        return self.gesture(DragGesture(minimumDistance: 0, coordinateSpace: .local)
            .onEnded({ value in
                    print(value.translation)
                if value.translation.width < 0 {
                    print("left")
                    left() }
                if value.translation.width > 0 {
                    print("right")
                    right() }
                if value.translation.height < 0 { up() }
                if value.translation.height > 0 { down() }
            }))
    }
}
