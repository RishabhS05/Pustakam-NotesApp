//
//  ActionButtonWithoutBackground.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 28/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import UIKit

struct ActionButtonWithoutBackground: View {
    let iconName: String
    let enabled: Bool
    let action: () -> Void
    var tint: Color
    var fallbackSystemName: String

    init(
        iconName: String,
        enabled: Bool = true,
        action: @escaping () -> Void,
        tint: Color = .white,
        fallbackSystemName: String = "square.grid.2x2"
    ) {
        self.iconName = iconName
        self.enabled = enabled
        self.action = action
        self.tint = tint
        self.fallbackSystemName = fallbackSystemName
    }

    private var isSystemIcon: Bool { UIImage(systemName: iconName) != nil }

    private var hasAssetIcon: Bool { UIImage(named: iconName) != nil }

    var body: some View {
        Button(action: action) {
            label
        }
        .disabled(!enabled)
    }

    @ViewBuilder
    private var label: some View {
        if isSystemIcon {
            symbol(iconName)
        } else if hasAssetIcon {
            asset(iconName)
        } else {
            // 🔧 09-Aug-2026: neither an SF Symbol nor a catalog image — never render nothing
            symbol(fallbackSystemName)
        }
    }

    private func symbol(_ name: String) -> some View {
        Image(systemName: name)
            .resizable()
            .scaledToFit()
            .frame(width: 18, height: 18)
            .padding(12)
            .shadow(color: .gray.opacity(0.1), radius: 4)
            .foregroundColor(enabled ? tint : .gray)
    }

    private func asset(_ name: String) -> some View {
        Image(name)
            .renderingMode(.original)
            .resizable()
            .scaledToFit()
            .frame(width: 20, height: 20)
            .padding(11)
            .opacity(enabled ? 1 : 0.4)
    }
}
