//
//  ActionButton.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 19/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//


import SwiftUI

struct ActionButton: View {
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
                .background(.brown)
                .clipShape(Rectangle())
                .cornerRadius(12)
                .shadow(color: .gray.opacity(0.1),radius: 4)
                .foregroundColor(tint)
        }
    }
}
