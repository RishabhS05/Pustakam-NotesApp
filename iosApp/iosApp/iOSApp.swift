import shared
import SwiftUI
import Firebase
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
                            case .Camera(let onDone):
                                CameraPreview(onCapture: onDone)
                            default: LoginView()
                        }
                    }
            }.environment(router)
                .environment(themeManager)
                .preferredColorScheme(themeManager.getTheme())
        }
	}
}

class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization
        FirebaseApp.configure()
        KoinKt.doInitKoin(appDeclaration: {_ in})
        return true
    }

}
