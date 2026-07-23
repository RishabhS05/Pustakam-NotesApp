//
//  ContinueReadingCard.swift
//  iosApp
//
//  Created by Rishabh on 22/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//


import SwiftUI

struct ContinueReadingCard: View {

    let title: String
    let description: String
    let progress: Double        // 0.0 ... 1.0
    let timeLeft: String
    let lastEdited: String

    var onClick: () -> Void

    private var progressText: String {
        "\(Int(progress * 100))%"
    }

    var body: some View {

        Button(action: onClick) {

            VStack(alignment: .leading, spacing: Theme.Spacing.md) {

                Text("Continue writing")
                    .font(Theme.Fonts.caption)
                    .foregroundStyle(Theme.Colors.primary)

                Text(title)
                    .font(Theme.Fonts.largeTitle)
                    .foregroundStyle(Theme.Colors.text)
                    .lineLimit(2)

                Text(description)
                    .font(Theme.Fonts.bodyText)
                    .foregroundStyle(Theme.Colors.text2)
                    .lineLimit(2)

                GeometryReader { geometry in

                    ZStack(alignment: .leading) {

                        RoundedRectangle(cornerRadius: 4)
                            .fill(Theme.Colors.surfaceSecondary)

                        RoundedRectangle(cornerRadius: 4)
                            .fill(Theme.Colors.primary)
                            .frame(width: geometry.size.width * progress)
                    }
                }
                .frame(height: 6)

                Text("\(progressText) • \(timeLeft) • edited \(lastEdited)")
                    .font(Theme.Fonts.caption)
                    .foregroundStyle(Theme.Colors.text3)
            }
            .padding(Theme.Spacing.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
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

    VStack {

        ContinueReadingCard(
            title: "On the Origins of Nalanda",
            description: "The great mahavihara drew scholars from across Asia, its nine-story library said to hold…",
            progress: 0.62,
            timeLeft: "4 min left",
            lastEdited: "12m ago"
        ) {

        }

    }
    .padding()
    .background(Theme.Colors.background)
}
