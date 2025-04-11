//
//  ActionButtonWithoutBackground.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 28/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ActionButtonWithoutBackground: View {
    let iconName: String
    let action: () -> Void
    var tint: Color = .white
    var body: some View {
        Button(action: action) {
            Image(systemName: iconName)
                .resizable()
                .scaledToFit()
                .frame(width: 18, height: 18)
                .padding(12)
                .shadow(color: .gray.opacity(0.1),radius: 4)
                .foregroundColor(tint)
        }
    }
}
