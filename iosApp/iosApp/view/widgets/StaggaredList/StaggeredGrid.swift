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
        ScrollView {
        GeometryReader { geometry in
            let columnWidth = (geometry.size.width - CGFloat(columns - 1) * spacing) / CGFloat(columns)
                       let groupedItems: [[T]] = groupItemsIntoColumns(items: items, columns: columns)
                       let enumeratedGroupedItems = Array(groupedItems.enumerated())
                HStack{
                    ForEach(enumeratedGroupedItems, id: \.0) { _, columnItems in
                        StaggeredColumn(items: columnItems, spacing: 0, columnWidth: columnWidth, content: content)
                    }
                }
            }
        .frame(height: calculatedHeight()) // Important to fix layout!
        }
    }
   private func calculatedHeight() -> CGFloat {
        // Rough estimate of height (you can improve this based on your content)
        let rows = ceil(Double(items.count) / Double(columns))
        return CGFloat(rows) * 120 // Assuming average 100pt height per card
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
        VStack{
            ForEach(items) { item in
                content(item)
                    .frame(width: columnWidth)
            }
        }
    }
}
struct StaggeredItem: Identifiable {
    let id = UUID()
    let height: CGFloat
    let color: Color
}
struct StaggView: View {
    let items = (0..<20).map { _ in
        StaggeredItem(height: CGFloat.random(in: 10...20), color: Color.red)
    }
    var body: some View {
        StaggeredGrid(columns: 2,items: items ,spacing: 6){ item in
            RoundedRectangle(cornerRadius: 12)
                .fill(item.color)
                            .frame(height: item.height)
                            .overlay(Text("Height: \(Int(item.height))").foregroundColor(.white))
        }
    }
}
