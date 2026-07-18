// 🔧 18-Jul-2026: NEW FEATURE (book widget) — iOS home-screen widget styled as a small leather
//   book; tapping it deep-links into the page-curl reader for the last-read note.
//   ⚠️ This file belongs to a Widget Extension TARGET (File → New → Target → Widget Extension,
//   name: PustakamBookWidget). Add this file to that target and enable the App Group
//   "group.com.app.pustakam" on BOTH the app and the extension. See test notes in chat.

import WidgetKit
import SwiftUI

// 🔧 18-Jul-2026: keys mirror BookWidgetStore in the app target (shared via App Group)
private let appGroupId = "group.com.app.pustakam"
private let lastIdKey = "book.last.noteId"
private let lastTitleKey = "book.last.title"

struct BookEntry: TimelineEntry {
    let date: Date
    let noteId: String?
    let title: String
}

struct BookProvider: TimelineProvider {
    private func currentEntry() -> BookEntry {
        let defaults = UserDefaults(suiteName: appGroupId)
        return BookEntry(
            date: Date(),
            noteId: defaults?.string(forKey: lastIdKey),
            title: defaults?.string(forKey: lastTitleKey) ?? "Pustakam"
        )
    }

    func placeholder(in context: Context) -> BookEntry {
        BookEntry(date: Date(), noteId: nil, title: "Pustakam")
    }

    func getSnapshot(in context: Context, completion: @escaping (BookEntry) -> Void) {
        completion(currentEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<BookEntry>) -> Void) {
        // static timeline — the app pokes WidgetCenter whenever a book is opened
        completion(Timeline(entries: [currentEntry()], policy: .never))
    }
}

struct PustakamBookWidgetEntryView: View {
    var entry: BookEntry

    private var deepLink: URL? {
        entry.noteId.flatMap { URL(string: "pustakam://book/\($0)") }
    }

    var body: some View {
        VStack(spacing: 8) {
            Rectangle().fill(Color(red: 0.85, green: 0.70, blue: 0.42)).frame(width: 34, height: 3).cornerRadius(2)
            Text(entry.title)
                .font(.system(.subheadline, design: .serif).weight(.bold))
                .foregroundColor(Color(red: 0.98, green: 0.95, blue: 0.89))
                .multilineTextAlignment(.center)
                .lineLimit(2)
            Text("Tap to open your book")
                .font(.system(size: 10))
                .foregroundColor(Color(red: 0.85, green: 0.70, blue: 0.42))
            Rectangle().fill(Color(red: 0.85, green: 0.70, blue: 0.42)).frame(width: 34, height: 3).cornerRadius(2)
        }
        .padding(12)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        // 🔧 18-Jul-2026: containerBackground required on iOS 17+, leather-cover gradient
        .containerBackground(for: .widget) {
            LinearGradient(colors: [Color(red: 0.42, green: 0.29, blue: 0.22),
                                    Color(red: 0.24, green: 0.16, blue: 0.12)],
                           startPoint: .topLeading, endPoint: .bottomTrailing)
        }
        .widgetURL(deepLink)   // no last book yet → just opens the app
    }
}

struct PustakamBookWidget: Widget {
    let kind: String = "PustakamBookWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: BookProvider()) { entry in
            PustakamBookWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Pustakam Book")
        .description("Opens your last note as a real book.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

@main
struct PustakamBookWidgetBundle: WidgetBundle {
    var body: some Widget {
        PustakamBookWidget()
    }
}
