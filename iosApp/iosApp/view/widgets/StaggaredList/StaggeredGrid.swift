//
//  StaggeredGrid.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 16/10/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import SwiftUI

struct StaggeredGrid<Content: View, T: Identifiable>: View {
    let columns: Int
    let items: [T]
    let spacing: CGFloat
    let content: (T) -> Content

    init(
        columns: Int,
        items: [T],
        spacing: CGFloat = 8,
        @ViewBuilder content: @escaping (T) -> Content
    ) {
        self.columns = columns
        self.items = items
        self.spacing = spacing
        self.content = content
    }

    var body: some View {
        GeometryReader { geometry in
            let columnWidth = (geometry.size.width - CGFloat(columns - 1) * spacing) / CGFloat(columns)
                       let groupedItems: [[T]] = groupItemsIntoColumns(items: items, columns: columns)
                       let enumeratedGroupedItems = Array(groupedItems.enumerated())
            ScrollView {
                HStack(alignment: .top) {
                    ForEach(enumeratedGroupedItems, id: \.0) { _, columnItems in
                        StaggeredColumn(items: columnItems, spacing: spacing, columnWidth: columnWidth, content: content)
                    }
                }
                .padding(.horizontal, spacing)
            }
        }
    }

    private func groupItemsIntoColumns(items: [T], columns: Int) -> [[T]] {
        var grouped: [[T]] = Array(repeating: [], count: columns)
        for (index, item) in items.enumerated() {
            grouped[index % columns].append(item)
        }
        return grouped
    }
}


struct StaggeredColumn<Content: View, T: Identifiable>: View {
    let items: [T]
    let spacing: CGFloat
    let columnWidth: CGFloat
    let content: (T) -> Content

    var body: some View {
        VStack {
            ForEach(items) { item in
                content(item)
                    .frame(width: columnWidth)
            }
        }
    }
}
