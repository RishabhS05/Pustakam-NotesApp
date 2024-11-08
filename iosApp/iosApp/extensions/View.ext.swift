//
//  View.ext.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 08/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
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
