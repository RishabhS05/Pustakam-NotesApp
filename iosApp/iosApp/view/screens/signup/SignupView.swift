import SwiftUI
import SwiftUICore
import shared

class SignUpHandler: BaseHandler {
    func validateCred(req: RegisterReq) -> ErrorField? {
        let valmsg =
            FieldValidationKt.checkRegisterFieldsValidity(
                req: req)
        return valmsg != ValidationError.none
            ? ErrorField(
                showErrorAlert: true,
                errorMessage: valmsg.getError()) : nil
    }
    func signUpCall(reqUser: RegisterReq) async -> BaseResult<User?> {
        return await apiHandler(apiCall: {
            try await base.registerUser(user: reqUser)
        })
    }
}
struct SignupView: View {
    @State private var name: String = ""
    @State private var email: String = ""
    @State private var phone: String = ""
    @State private var password: String = ""
    @State private var confirmPasword: String = ""
    @State private var errorField: ErrorField = ErrorField()
    @State private var imageUrl: String = ""
    @State private var isLoading: Bool = false
    @FocusState private var focusedField: Fields?
    private enum Fields {
        case name, email, phone, password, confirmPasword
    }
    private let signupHandle = SignUpHandler()
    @Environment(\.dismiss) var dismiss
    var body: some View {
        ZStack {
            VStack(spacing: 20) {
            AvatarImageView(imageUrl: imageUrl) {}
            TextField(
                "Your name",
                text: $name
            ).textFieldStyle(OutlineTextfieldStyle())
                    .focused($focusedField, equals: .name)
                    .submitLabel(.next)
                            .onSubmit {
                                focusedField = .email
                            }
            TextField(
                "Your email",
                text: $email
            ).textFieldStyle(OutlineTextfieldStyle())
                    .focused($focusedField, equals: .email)
                    .submitLabel(.next)
                            .onSubmit {
                                focusedField = .phone
                            }
            TextField(
                "Your Phone",
                text: $phone
            ).textFieldStyle(OutlineTextfieldStyle())
                    .focused($focusedField, equals: .phone)
                    .submitLabel(.next)
                            .onSubmit {
                                focusedField = .password
                            }
            SecureField(
                "Passeord",
                text: $password
            ).textFieldStyle(OutlineTextfieldStyle())
                    .focused($focusedField, equals: .password)
                    .submitLabel(.next)
                            .onSubmit {
                                focusedField = .confirmPasword
                            }
            SecureField(
                "Confirm Password",
                text: $confirmPasword
            ).textFieldStyle(OutlineTextfieldStyle())
                    .focused($focusedField, equals: .phone)
                    .submitLabel(.done)
                            .onSubmit {
                                focusedField = nil
                                signUpHandle()
                            }
                
            Button("Sign up") {
                signUpHandle()
            }.buttonStyle(SigninButtonStyle())
            
        }.padding(20)
                .alert(
                    isPresented: $errorField.showErrorAlert,
                    content: {
                        return Alert(
                            title: Text("Error!"),
                            message: Text(errorField.errorMessage),
                            dismissButton: Alert.Button.default(
                                Text("OK"),
                                action: {
                                    errorField.showErrorAlert = false
                                })
                        )
                    })
            if isLoading {
                LoadingUI().frame(alignment: .center)
                Color.black.opacity(0.4).edgesIgnoringSafeArea(.all)
            }
        } .navigationBarBackButtonHidden(true)
        .toolbar{
                ToolbarItem(placement: .topBarLeading){
                    BackButton(action: {
                        dismiss()
                    })
                }
            }
        .onAppear{
            focusedField = .name
        }
    }
    func signUpHandle() {
        let user = RegisterReq(
            name: name,
            email: email,
            password: password,
            passwordConfirm: confirmPasword,
            phone: phone, imageUrl: "")
        let validation = signupHandle.validateCred(req: user)
        if validation != nil && validation!.showErrorAlert {
            errorField = validation!
            return
        }
        Task {
           isLoading = true
            let resp = await signupHandle.signUpCall(
                reqUser: user)
            isLoading = false
            if resp.error != nil && !resp.isSuccessful {
                errorField.errorMessage =
                    (resp.error as! NetworkError).getError()
                errorField.showErrorAlert = !resp.isSuccessful
                return
            }
        }
        dismiss()
    }
}
#Preview {
    SignupView()
}
