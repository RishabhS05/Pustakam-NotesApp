//
//  GenericGrid.swift
//  iosApp
//
//  Created by Rishabh on 01/08/26.
//  Copyright © 2026 orgName. All rights reserved.
//

import SwiftUI

struct GenericGrid<Item: Identifiable, Content: View>: View {

    let items: [Item]
    let columns: Int
    let spacing: CGFloat
    let content: (Item) -> Content

    init(
        items: [Item],
        columns: Int = 2,
        spacing: CGFloat = 12,
        @ViewBuilder content: @escaping (Item) -> Content
    ) {
        self.items = items
        self.columns = columns
        self.spacing = spacing
        self.content = content
    }

    private var gridColumns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: spacing),
            count: columns
        )
    }

    var body: some View {
        LazyVGrid(columns: gridColumns, spacing: spacing) {
            ForEach(items) { item in
                content(item).padding(spacing)
            }
        }
    }
}

private struct GenericGridPreviewItem: Identifiable {
    let id = UUID()
    let title: String
    let color: Color
}

struct GenericGrid_Previews: PreviewProvider {
    private static let items = [
        GenericGridPreviewItem(title: "Video", color: .blue),
        GenericGridPreviewItem(title: "Image", color: .green),
        GenericGridPreviewItem(title: "Doc", color: .orange),
        GenericGridPreviewItem(title: "Audio", color: .purple)
    ]

    static var previews: some View {
        GenericGrid(items: items, columns: 2, spacing: 8) { item in
            RoundedRectangle(cornerRadius: 12)
                .fill(item.color.opacity(0.85))
                .frame(height: 120)
                .overlay {
                    Text(item.title)
                        .font(.headline)
                        .foregroundColor(.white)
                }
        }
        .padding()
        .previewLayout(.sizeThatFits)
    }
}
