//
//  QuickActionCard.swift
//  iosApp
//
//  Created by Rishabh on 22/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//


import SwiftUI

struct QuickActionCard: View {

    let title: String
    let icon: String
    let iconColor: Color
    let iconBackground: Color

    var onClick: () -> Void

    var body: some View {

        Button(action: onClick) {

            VStack(spacing: Theme.Spacing.md) {

                ZStack {

                    RoundedRectangle(cornerRadius: Theme.Radius.md)
                        .fill(iconBackground)
                        .frame(width: 48, height: 48)

                    Image(systemName: icon)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(iconColor)
                }

                Text(title)
                    .font(Theme.Fonts.displayL)
                    .foregroundStyle(Theme.Colors.text)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, Theme.Spacing.lg)
            .background(Theme.Colors.surface)
            .clipShape(
                RoundedRectangle(
                    cornerRadius: Theme.Radius.lg
                )
            )
            .overlay(
                RoundedRectangle(
                    cornerRadius: Theme.Radius.lg
                )
                .stroke(
                    Theme.Colors.border,
                    lineWidth: 1
                )
            )
            .shadow(
                color: Theme.Elevation.card,
                radius: Theme.Elevation.cardRadius,
                x: 0,
                y: Theme.Elevation.cardY
            )
        }
        .buttonStyle(.plain)
    }
}

#Preview {

    LazyVGrid(
        columns: [
            GridItem(.flexible()),
            GridItem(.flexible())
        ],
        spacing: 16
    ) {

        QuickActionCard(
            title: "New Note",
            icon: "plus",
            iconColor: .orange,
            iconBackground: Color.orange.opacity(0.15)
        ) {}

        QuickActionCard(
            title: "Voice Note",
            icon: "mic.fill",
            iconColor: .red,
            iconBackground: Color.red.opacity(0.15)
        ) {}

        QuickActionCard(
            title: "Scan",
            icon: "doc.viewfinder",
            iconColor: .green,
            iconBackground: Color.green.opacity(0.15)
        ) {}

        QuickActionCard(
            title: "AI Assistant",
            icon: "sparkles",
            iconColor: .purple,
            iconBackground: Color.purple.opacity(0.15)
        ) {}
    }
    .padding()
    .background(Theme.Colors.background)
}
