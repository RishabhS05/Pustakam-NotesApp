import SwiftUI
import shared
 
class  UserPreferenceWrapper : ObservableObject {
    private let userPreferenceViewModel: UserPreferenceViewModel = UserPreferenceViewModel()
    @Published var userPreference: UserPreference? = nil
    init() {
        userPreferenceViewModel.observeUserPreference { [weak self]  newPreference in
                    DispatchQueue.main.async {
                      self?.userPreference = newPreference
                    }
                }
    }
     
}


struct AppView : View {
    @EnvironmentObject var router : Router
    @StateObject var userPreferenceWrapper : UserPreferenceWrapper = UserPreferenceWrapper()
    var body: some View {
        LoginView()
            .onChange(of: userPreferenceWrapper.userPreference?.isAuthenticated) { isAuthenticated in
                        if isAuthenticated == true {
                            print("User is authenticated")
                            router.navigate(to: .Home)
                        }
                    }
//            .onAppear{
//                print("UserPrefs \(userPreferenceWrapper.userPreference)")
//                if (userPreferenceWrapper.userPreference?.isAuthenticated) == true  {
//                    print("User is authenticated")
//                    router.navigate(to: .Home)
//                }
//            }
    }
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		AppView()
	}
}
