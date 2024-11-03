import SwiftUI
import shared
@main
struct iOSApp: App {
    @ObservedObject var router = Router()
    
//    init() {
//        KoinIOS.shared.startApp()
//     }
	var body: some Scene {
       
        WindowGroup {
            NavigationStack(path: $router.navPath){
                AppView()
                    .navigationDestination(for: Router.Destination.self){
                        destination in
                        switch destination {
                            case .Signup : SignupView()
                            case.Notes : NotesView()
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
