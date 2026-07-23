//
//  FolderItem.swift
//  iosApp
//
//  Created by Rishabh on 22/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//


import SwiftUI

struct FolderItem: View {

    let title: String
    let noteCount: Int

    let iconColor: Color
    let iconBackground: Color

    var onClick: () -> Void

    var body: some View {

        Button(action: onClick) {

            HStack(spacing: Theme.Spacing.md) {

                RoundedRectangle(cornerRadius: Theme.Radius.md)
                    .fill(iconBackground)
                    .frame(width: 44, height: 44)
                    .overlay {
                        Image(systemName: "folder.fill")
                            .font(.system(size: 20))
                            .foregroundStyle(iconColor)
                    }

                VStack(alignment: .leading, spacing: 4) {

                    Text(title)
                        .font(Theme.Fonts.body)
                        .foregroundStyle(Theme.Colors.text)

                    Text("\(noteCount) notes")
                        .font(Theme.Fonts.caption)
                        .foregroundStyle(Theme.Colors.text2)
                }

                Spacer()
            }
            .padding(Theme.Spacing.md)
            .frame(maxWidth: .infinity)
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
        }
        .buttonStyle(.plain)
    }
}

#Preview {

    VStack(spacing: 12) {

        FolderItem(
            title: "Studies",
            noteCount: 48,
            iconColor: .orange,
            iconBackground: .orange.opacity(0.15)
        ) {}

        FolderItem(
            title: "Work",
            noteCount: 31,
            iconColor: .green,
            iconBackground: .green.opacity(0.15)
        ) {}

        FolderItem(
            title: "Journal",
            noteCount: 120,
            iconColor: .indigo,
            iconBackground: .indigo.opacity(0.15)
        ) {}

        FolderItem(
            title: "Recipes",
            noteCount: 17,
            iconColor: .yellow,
            iconBackground: .yellow.opacity(0.20)
        ) {}
    }
    .padding()
    .background(Theme.Colors.background)
}
