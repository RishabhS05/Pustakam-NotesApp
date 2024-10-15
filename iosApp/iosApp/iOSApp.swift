import SwiftUI

@main
struct iOSApp: App {
    @ObservedObject var router = Router()
	var body: some Scene {
        WindowGroup {
            NavigationStack(path: $router.navPath){
                AppView()
                    .navigationDestination(for: Router.Destination.self){
                        destination in
                        switch destination {
                            case .Signup : SignupView()
                            case.Notes : Notes()
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
