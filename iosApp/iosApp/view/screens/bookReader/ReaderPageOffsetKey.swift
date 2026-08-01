import SwiftUI

// 📖 01-Aug-2026: collects each page's live offset so the reader can report the current page from
//   MEASURED positions. Replaces the fixed pageStride arithmetic, which assumed uniform sheets.
struct ReaderPageOffsetKey: PreferenceKey {
    static var defaultValue: [Int: CGFloat] = [:]

    static func reduce(value: inout [Int: CGFloat], nextValue: () -> [Int: CGFloat]) {
        value.merge(nextValue()) { _, new in new }
    }
}
