import SwiftUI
import shared
 
class  UserPreferenceWrapper : ObservableObject {
    private let userPreferenceViewModel: UserPreferenceViewModel = UserPreferenceViewModel()
    @Published var userPreference: UserPreference? = nil
    
    init() {
        userPreferenceViewModel.observeUserPreference { [weak self]  newPreference in DispatchQueue.main.async {
                      self?.userPreference = newPreference
                    }
                }
    }
}


struct AppView : View {
    @Environment(Router.self) var router : Router
    @Environment(\.dismiss) var dismiss
    @StateObject var userPreferenceWrapper : UserPreferenceWrapper = UserPreferenceWrapper()

    private var isSignedIn: Bool {
        AuthConfig.shared.bypassAuth() || userPreferenceWrapper.userPreference?.isAuthenticated == true
    }

    var body: some View {
        Group {
            if isSignedIn {
                HomeView()
            } else {
                LoginView()
            }
        }
        .onChange(of: userPreferenceWrapper.userPreference?.isAuthenticated){ _ , isAuthenticated in
            if isAuthenticated == true && !AuthConfig.shared.bypassAuth() {
                dismiss()
                router.navigate(to: .Home)
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
        AppView()
	}
}
