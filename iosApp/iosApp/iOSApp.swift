import shared
import SwiftUI

@main
struct iOSApp: App {
    
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    @ObservedObject var router = Router()
    var body: some Scene {
       
        WindowGroup {
            NavigationStack(path: $router.navPath){
                AppView()
                    .navigationDestination(for: Router.Destination.self){
                        destination in
                        switch destination {
                            case .Signup : SignupView()
                            case.Notes : NotesView()
                            case.NoteEditor : NoteEditorView()
                            case.Login : LoginView()
                            case .Notification : NotificationView()
                            case .Search : SearchView()
                            case .Home : HomeView()
                            default: LoginView()
                        }
                    }
            }.environmentObject(router)
        }
	}
}

class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization
        KoinKt.doInitKoin(appDeclaration: {_ in})
        return true
    }
}
