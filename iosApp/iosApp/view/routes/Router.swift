//
//  Router.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 04/10/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

final class Router: ObservableObject {
    @Published var navPath = NavigationPath()
    public enum Destination : Codable , Hashable{
        case Login
        case Signup
        case Notes
        case NoteEditor
        case Profile
        case Notification
        case Search
        case Home

    }
    func navigate(to destination : Destination ){
        navPath.append(destination)
    }
    func navigateBack() {
           navPath.removeLast()
       }
       
       func navigateToRoot() {
           navPath.removeLast(navPath.count)
       }
}
