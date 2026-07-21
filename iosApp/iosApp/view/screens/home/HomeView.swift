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
            NotesView().tabItem {
                Image(systemName: "note.text.badge.plus")
                Text("Notes")
            }
            .tag(0)
            SearchView().tabItem {
                Image(systemName: "magnifyingglass")
                Text("Search")
            }
            .tag(1)
            
            NotificationView().tabItem {
                Image(systemName: "bell.fill")
                Text("Notification")
            }
            .tag(2)
        }
        .background(Theme.Colors.background)
        .navigationBarBackButtonHidden(true)
        .navigationTitle(title.capitalized)
    }
}

#Preview {
    HomeView()
}
