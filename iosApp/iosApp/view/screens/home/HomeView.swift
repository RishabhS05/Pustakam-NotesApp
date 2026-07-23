import SwiftUI

struct HomeView : View {
    @State var selectedTab = 0
    // 🎨 22-Jul-2026 — palette drives the tab bar tint so the accent follows the chosen theme
    @Environment(\.palette) private var palette
    var title : String {
        switch(selectedTab) {
            case 0 :  return "Notes"
            case 1 :  return "Search"
            case 2 :  return "Notification"
            case 3 :  return "Settings"
            default : return "Notes"
        }
    }
    var body: some View {
        TabView(selection: $selectedTab){
            NotesView()
                .background(Theme.Colors.background)
                .tabItem {
                Image(systemName: "note.text.badge.plus")
                Text("Notes")
            }
            .tag(0)
            SearchView()
                .background(Theme.Colors.background)
                .tabItem {
                Image(systemName: "magnifyingglass")
                Text("Search")
            }
            .tag(1)
            
            NotificationView()
                .background(Theme.Colors.background)
                .tabItem {
                Image(systemName: "bell.fill")
                Text("Notification")
            }
            .tag(2)
            
            SettingsView()
                .background(Theme.Colors.background)
                .tabItem {
                Image(systemName: "gearshape.fill")
                Text("Settings")
            }
            .tag(3)
        }
        .navigationBarBackButtonHidden(true)
        .navigationTitle(title.capitalized)
        // 🎨 22-Jul-2026 — saffron/copper accent on the selected tab instead of the system blue
        .tint(palette.accent)

    }
}

// 🎨 22-Jul-2026 — previews per theme so the tab bar tint can be checked without running the app
#Preview("Home — Light") {
    HomeView().environment(\.palette, ThemePalette(mode: .light, scheme: .light))
        .preferredColorScheme(.light)
}

#Preview("Home — Dark") {
    HomeView().environment(\.palette, ThemePalette(mode: .dark, scheme: .dark))
        .preferredColorScheme(.dark)
}

#Preview("Home — AMOLED") {
    HomeView().environment(\.palette, ThemePalette(mode: .amoled, scheme: .dark))
        .preferredColorScheme(.dark)
}


