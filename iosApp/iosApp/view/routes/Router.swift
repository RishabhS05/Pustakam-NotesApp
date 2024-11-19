    //
    //  Router.swift
    //  iosApp
    //
    //  Created by Rishabh Shrivastava on 04/10/24.
    //  Copyright © 2024 orgName. All rights reserved.
    //

import SwiftUI
import shared
final class Router: ObservableObject {
    @Published var navPath = NavigationPath()
    public enum Destination : Hashable {
        case Login
        case Signup
        case Notes
        case NoteEditor (note : Note? = nil )
        case Profile
        case Notification
        case Search
        case Home
        
        func hash(into hasher: inout Hasher) {
            switch self {
                case .NoteEditor(let note):
                    hasher.combine(note?._id)
                default:
                    hasher.combine(String(describing: self))
            }
        }
        
        static func == (lhs: Router.Destination, rhs: Router.Destination) -> Bool {
            switch (lhs, rhs) {
                case (.NoteEditor(let lhsNote), .NoteEditor(let rhsNote)):
                    return lhsNote?._id == rhsNote?._id
                default:
                    return String(describing: lhs) == String(describing: rhs)
            }
        }
        
    }
    func navigate(to destination : Destination){
        navPath.append(destination)
    }
    func navigateBack() {
        navPath.removeLast()
    }
    
    func navigateToRoot() {
        navPath.removeLast(navPath.count)
    }
}
