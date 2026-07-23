import shared
import SwiftUI
import Firebase
import FirebaseCrashlytics
@main
struct iOSApp: App {
   @State var themeManager = ThemeManager()
   @State var router = Router()
    
    init(){
        FirebaseApp.configure()
        KoinKt.doInitKoin(appDeclaration: {_ in})
    }
    var body: some Scene {
     
        WindowGroup {

            NavigationStack(path: $router.navPath){
                AppView()
                    
                    .navigationDestination(for: Router.Destination.self){
                        destination in
                        switch destination {
                            case .Signup : SignupView()
                            case.Notes : NotesView()
                            case.NoteEditor(let note) : NoteEditorView(note: note)
                            case.Login : LoginView()
                            case .Notification : NotificationView()
                            case .Search : SearchView()
                            case .Home : HomeView()
                            case .Settings : SettingsView()
                            case .Camera(let onDone):
                                CameraPreview(onCapture: onDone)
                            // 🔧 18-Jul-2026: page-curl book reader destination
                            case .BookReader(let noteId, let startContentId, let single):
                                BookReaderView(noteId: noteId, startContentId: startContentId, singleContent: single)
                            default: LoginView()
                        }
                    }
            }
            // 🔧 18-Jul-2026: widget deep link pustakam://book/<noteId> → open the book reader
            .onOpenURL { url in
                guard url.scheme == "pustakam", url.host == "book" else { return }
                let noteId = url.lastPathComponent
                if !noteId.isEmpty && noteId != "book" {
                    router.navigate(to: .BookReader(noteId: noteId))
                }
            }
            .environment(router)
                .environment(themeManager)
                // 🎨 22-Jul-2026 — nil for .system so the app follows the OS appearance.
                .preferredColorScheme(themeManager.getTheme())
                // 🎨 22-Jul-2026 — inject \.palette *after* preferredColorScheme so the AMOLED
                //   override sees the scheme iOS actually resolved.
                .themedRoot(mode: themeManager.mode)
        }
	}
}


final class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        return true
    }
}

// 🐛 23-Jul-2026: breadcrumbs for the PDF path. A CoreGraphics abort gives a stack with no clue
//   WHICH file killed it; these keys ride along with the next crash report so the offending
//   document is identifiable.
enum CrashBreadcrumb {
    static func openingDocument(contentId: String, path: String?, pageCount: Int) {
        let crashlytics = Crashlytics.crashlytics()
        crashlytics.setCustomValue(contentId, forKey: "doc.contentId")
        crashlytics.setCustomValue(pageCount, forKey: "doc.pageCount")
        crashlytics.setCustomValue((path as NSString?)?.lastPathComponent ?? "nil", forKey: "doc.file")
        crashlytics.log("opening document \(contentId) pages=\(pageCount)")
    }

    static func rejectedUnreadablePdf(path: String?) {
        Crashlytics.crashlytics().log("rejected unreadable pdf: \((path as NSString?)?.lastPathComponent ?? "nil")")
    }
}
