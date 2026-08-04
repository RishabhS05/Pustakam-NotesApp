
import SwiftUI
@Observable final class Router{
    
    var navPath = NavigationPath()
    public enum Destination : Hashable {
        case Login
        case Signup
        case Notes
        case NoteEditor (noteId : String? = nil )
        case Profile
        case Notification
        case Search
        case Home
        case Settings
        case Camera (onCapture : (CapturedMedia?) -> Void)
        // 📖 01-Aug-2026: two readers — one document (bookId), or the whole note (widget deep link)
        case BookReader (noteId : String, bookId : String)
        case NoteBookReader (noteId : String, startContentId : String? = nil)

        func hash(into hasher: inout Hasher) {
            switch self {
                case .NoteEditor(let noteId):
                    hasher.combine(noteId)
                case .BookReader(let noteId, let bookId):
                    hasher.combine(noteId); hasher.combine(bookId)
                case .NoteBookReader(let noteId, let contentId):
                    hasher.combine(noteId); hasher.combine(contentId)
                default:
                    hasher.combine(String(describing: self))
            }
        }

        static func == (lhs: Router.Destination, rhs: Router.Destination) -> Bool {
            switch (lhs, rhs) {
                case (.NoteEditor(let lhsId), .NoteEditor(let rhsId)):
                    return lhsId == rhsId
                case (.BookReader(let lId, let lB), .BookReader(let rId, let rB)):
                    return lId == rId && lB == rB
                case (.NoteBookReader(let lId, let lC), .NoteBookReader(let rId, let rC)):
                    return lId == rId && lC == rC
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
