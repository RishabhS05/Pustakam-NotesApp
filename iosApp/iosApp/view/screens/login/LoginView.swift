import SwiftUI
import shared

struct LoginHandler : IBaseHandler {
    var base: BaseRepository = BaseRepository()
    func loginWithEmail(login :Login) async -> BaseResult<BaseResponse<User>?>  {
        return await apiHandler(apiCall: {
           try await base.loginUser(login: login)
        })
    }
}


struct LoginView: View {
    @State private var password : String = ""
    @State private var email : String = ""
    @State private var error : ErrorField = ErrorField()
    private let loginHandler = LoginHandler()
    @EnvironmentObject var router: Router
    var body: some View {
        VStack{
            TextField(
                "Your Phone or Email",
                text: $email
            ).textFieldStyle(OutlineTextfieldStyle()) .padding(.horizontal, 24)
            
            SecureField(
                "Passeord",
                text: $password
            ).textFieldStyle(OutlineTextfieldStyle()) .padding(.horizontal, 24)
                .padding(.vertical)
            
            Button("Login"){
               login(email: email, password: password)
            }.buttonStyle(SigninButtonStyle())
            
            Button("Sign up") {
                router.navigate(to: .Signup)
            } .foregroundColor(.brown)
        }.alert(isPresented: $error.showErrorAlert, content: {
            return Alert(title: Text("Error!"), message: Text(error.errorMessage),
                         dismissButton: Alert.Button.default(Text("OK"), action: {
                error.showErrorAlert = false
            })
        )
        })
    }
    
    func login(email: String, password: String) {
        let login = Login(email: email, password: password, phone: nil)
        Task {
           let resp =  await loginHandler.loginWithEmail(login: login)
            if( resp.error != nil && !resp.isSuccessful) {
                error.errorMessage = resp.error!.getError()
                error.showErrorAlert = !resp.isSuccessful
            }else {
                router.navigate(to: .Notes)
            }
        }
    }
}

#Preview {
    LoginView()
}
