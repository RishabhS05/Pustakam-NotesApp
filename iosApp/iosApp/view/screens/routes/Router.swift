
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
        // 🔧 18-Jul-2026: NEW — page-curl book reader (noteId so the widget deep link works too)
        // 🔧 19-Jul-2026: single=true → book contains ONLY the tapped file's pages
        case BookReader (noteId : String, startContentId : String? = nil, single : Bool = false)

        func hash(into hasher: inout Hasher) {
            switch self {
                case .NoteEditor(let note):
                    hasher.combine(note?.id)
                case .BookReader(let noteId, let contentId, let single):   // 🔧 19-Jul-2026
                    hasher.combine(noteId); hasher.combine(contentId); hasher.combine(single)
                default:
                    hasher.combine(String(describing: self))
            }
        }

        static func == (lhs: Router.Destination, rhs: Router.Destination) -> Bool {
            switch (lhs, rhs) {
                case (.NoteEditor(let lhsNote), .NoteEditor(let rhsNote)):
                    return lhsNote?.id == rhsNote?.id
                case (.BookReader(let lId, let lC, let lS), .BookReader(let rId, let rC, let rS)):   // 🔧 19-Jul-2026
                    return lId == rId && lC == rC && lS == rS
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
