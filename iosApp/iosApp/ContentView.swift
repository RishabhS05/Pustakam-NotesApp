import SwiftUI
import shared
struct  HomeView : View {
    @EnvironmentObject var router : Router
	var body: some View {
            LoginView()
            .onAppear{
            //auth and move to Notes List
            
        }
	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		HomeView()
	}
}
