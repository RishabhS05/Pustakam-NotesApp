//
//  SigninButton.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 03/10/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct SigninButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.title2).frame(width: 120)
        .padding(8).background(.brown)
        .foregroundColor(.white)
        .cornerRadius(12)
    }
}

//#Preview {
//    SigninButton()
//}

struct ButtonStyle_Preview: PreviewProvider {
    static var previews: some View {
        Button("Sign up"){}.buttonStyle(SigninButtonStyle())
    }
}
