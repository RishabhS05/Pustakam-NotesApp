    //
    //  HomeView.swift
    //  iosApp
    //
    //  Created by Rishabh Shrivastava on 15/10/24.
    //  Copyright © 2024 orgName. All rights reserved.
    //

import SwiftUI

struct HomeView : View {
    var body: some View {
        TabView{
            Notes().tabItem {
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
