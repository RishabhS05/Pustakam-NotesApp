import SwiftUI
import shared

class LoginHandler : IBaseHandler {
    var base: BaseRepository = KoinHelper().getBaseRepository()
    func checkLoginCredValidity(req: Login) -> ErrorField? {
        let valmsg  = FieldValidationKt.checkLoginEmailPasswordValidity(req: req)
        return   valmsg != ValidationError.none ?
        ErrorField(showErrorAlert: true, errorMessage : valmsg.getError()) :  nil
    }
    func loginWithEmail(login :Login) async ->
    BaseResult<BaseResponse<User>?>  {
        return await apiHandler(apiCall: {
            try await base.loginUser(login: login)
        })
    }
}


struct LoginView: View {
    @State private var password : String = ""
    @State private var email : String = ""
    @State private var errorField : ErrorField = ErrorField()
    @State private var isLoading : Bool = false
    private let loginHandler = LoginHandler()
    @EnvironmentObject var router: Router
    var body: some View {
        ZStack(alignment: .center){
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
                
            }.alert(isPresented: $errorField.showErrorAlert, content: {
                return Alert(title: Text("Error!").font(.headline.weight(.heavy)).foregroundColor(.red),
                             message: Text(errorField.errorMessage),
                             dismissButton: Alert.Button.default(Text("OK"), action: {errorField.showErrorAlert = false}))
            })
            if isLoading {
                LoadingUI().frame(alignment: .center)
                Color.black.opacity(0.4).edgesIgnoringSafeArea(.all)
            }
        }

    }
    
    func login(email: String, password: String) {
        let login = Login(email: email, password: password, phone: nil)
        let validation = loginHandler.checkLoginCredValidity(req: login)
        if validation != nil && validation!.showErrorAlert {
            errorField = validation!
            return
        }
        Task {
            isLoading = true
            let resp =  await loginHandler.loginWithEmail(login: login)
            isLoading = false
            if( resp.error != nil && !resp.isSuccessful) {
                errorField.errorMessage = (resp.error as! NetworkError).getError()
                errorField.showErrorAlert = !resp.isSuccessful
            }else {
                router.navigate(to: .Home)
            }
        }
        
    }
}

#Preview {
    LoginView()
}
