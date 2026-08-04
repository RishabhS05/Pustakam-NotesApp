import shared
import SwiftUI
import Firebase
import FirebaseCrashlytics
@main
struct iOSApp: App {
    init(){
        if AppEnvironment.isRunningForPreviews {
            return
        }

        FirebaseApp.configure()
        KoinKt.doInitKoin(appDeclaration: {_ in})
    }
    var body: some Scene {
        WindowGroup {
            if AppEnvironment.isRunningForPreviews {
                EmptyView()
            } else {
                AppRootView()
            }
        }
	}
}

private struct AppRootView: View {
    @State var themeManager = ThemeManager()
    @State var router = Router()

    var body: some View {
        NavigationStack(path: $router.navPath){
            AppView()

                .navigationDestination(for: Router.Destination.self){
                    destination in
                    switch destination {
                        case .Signup : SignupView()
                        case.Notes : NotesView()
                        case .NoteEditor(let noteId) : NoteEditorView(noteId: noteId)
                        case.Login : LoginView()
                        case .Notification : NotificationView()
                        case .Search : SearchView()
                        case .Home : HomeView()
                        case .Settings : SettingsView()
                        case .Camera(let onDone):
                            CameraPreview(onCapture: onDone)
                        // 📖 01-Aug-2026: document reader vs whole-note reader
                        case .BookReader(let bookId):
                            BookReaderView(bookId: bookId)
                        case .NoteBookReader(let noteId, let startContentId):
                            NoteBookReaderView(noteId: noteId, startContentId: startContentId)
                        default: LoginView()
                    }
                }
        }
        // 🔧 18-Jul-2026: widget deep link pustakam://book/<noteId> → open the book reader
        .onOpenURL { url in
            guard url.scheme == "pustakam", url.host == "book" else { return }
            let noteId = url.lastPathComponent
            if !noteId.isEmpty && noteId != "book" {
                router.navigate(to: .NoteBookReader(noteId: noteId))
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
