import SwiftUI

struct HomeView : View {
  
    @State var selectedTab = 0
    var title : String {
        switch(selectedTab) {
            case 0 :  return "Notes"
            case 1 :  return "Search"
            case 2 :  return "Notification"
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
     
    }
}

#Preview {
    HomeView()
}


