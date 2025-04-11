
import SwiftUI
import shared
@Observable final class Router{
    
    var navPath = NavigationPath()
    public enum Destination : Hashable {
        case Login
        case Signup
        case Notes
        case NoteEditor (note : Note? = nil )
        case Profile
        case Notification
        case Search
        case Home
        case Camera (onCapture : (CapturedMedia?) -> Void)
        
        func hash(into hasher: inout Hasher) {
            switch self {
                case .NoteEditor(let note):
                    hasher.combine(note?.id)
                default:
                    hasher.combine(String(describing: self))
            }
        }
        
        static func == (lhs: Router.Destination, rhs: Router.Destination) -> Bool {
            switch (lhs, rhs) {
                case (.NoteEditor(let lhsNote), .NoteEditor(let rhsNote)):
                    return lhsNote?.id == rhsNote?.id
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
