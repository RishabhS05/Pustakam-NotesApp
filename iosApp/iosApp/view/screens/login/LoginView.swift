import SwiftUI
import shared

// 🔧 AUTH-FIX: no BaseViewModel, no apiHandler casts — auth goes through AuthBridge use cases
class LoginHandler {

    private let adapter = AuthBridgeAdapter()

    func checkLoginCredValidity(req: Login) -> ErrorField? {
        let valmsg  = FieldValidationKt.checkLoginEmailPasswordValidity(req: req)
        return   valmsg != ValidationError.none ?
        ErrorField(showErrorAlert: true, errorMessage : valmsg.getError()) :  nil
    }

    func loginWithEmail(login: Login, onState: @escaping (UiState<User>) -> Void) {
        adapter.login(login: login, onState: onState)
    }
}


struct LoginView: View {
    
    @State private var password : String = ""
    @State private var email : String = ""
    @State private var errorField : ErrorField = ErrorField()
    @State private var isLoading : Bool = false
    // 🔧 REALTIME-FIX: @State keeps ONE handler instance across View-struct re-renders —
    //   a plain `let` was recreated on every render, deallocating the old adapter and
    //   cancelling any in-flight login.
    @State private var loginHandler = LoginHandler()
    
    private enum Fields {
        case email, password
    }
    @FocusState private var focusedField: Fields?
    @Environment(Router.self) private var router: Router
    var body: some View {
        ZStack(alignment: .center){
            VStack{
                TextField(
                    "Your Phone or Email",
                    text: $email
                ).textFieldStyle(OutlineTextfieldStyle())
                    .focused($focusedField, equals: .email)
                    .submitLabel(.next)
                    .onSubmit{
                        focusedField = .password
                    }
                    .padding(.horizontal, 24)
                
                SecureField(
                    "Passeord",
                    text: $password
                ).textFieldStyle(OutlineTextfieldStyle())
                    .focused($focusedField, equals: .password)
                    .padding(.horizontal, 24)
                    .padding(.vertical)
                    .submitLabel(.done)
                    .onSubmit{
                        focusedField = nil
                        login(email: email, password: password)
                    }
                
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
        }.onAppear{
            focusedField = .email
        }
    }
    
    func login(email: String, password: String) {
        // 🔧 19-Aug-2026 — field carries either an email or a phone number; route it accordingly
        let login = email.contains("@")
            ? Login(email: email, password: password, phone: nil)
            : Login(email: nil, password: password, phone: email)
        let validation = loginHandler.checkLoginCredValidity(req: login)
        if validation != nil && validation!.showErrorAlert {
            errorField = validation!
            return
        }
        // 🔧 AUTH-FIX: callback state machine on main thread — no Task, no `as!` crash cast
        loginHandler.loginWithEmail(login: login) { state in
            switch state {
            case .loading:
                isLoading = true
            case .success:
                isLoading = false
                router.navigate(to: .Home)
            case .failure(let error):
                isLoading = false
                errorField.errorMessage = error.message   // typed BridgeError — never crashes
                errorField.showErrorAlert = true
            case .idle:
                break
            }
        }
    }
}

#Preview {
    LoginView()
}
