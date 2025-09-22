import SwiftUI

struct HomeView : View {

    var body: some View {
        TabView{
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
        }.navigationBarBackButtonHidden()
        
    }
}

#Preview {
    HomeView()
}
