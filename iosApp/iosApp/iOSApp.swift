import SwiftUI

@main
struct iOSApp: App {
    @ObservedObject var router = Router()
	var body: some Scene {
        WindowGroup {
            NavigationStack(path: $router.navPath){
                HomeView()
                    .navigationDestination(for: Router.Destination.self){
                        destination in
                        switch destination {
                            case .Signup : SignupView()
                            case.Notes : Notes()
                            case.Login : LoginView()
                            default: LoginView()
                        }
                    }
            }.environmentObject(router)
        }
	}
}
