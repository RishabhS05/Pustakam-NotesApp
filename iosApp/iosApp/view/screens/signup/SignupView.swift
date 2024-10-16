import SwiftUI
import shared
struct SignUpHandler : IBaseHandler{
    var base: BaseRepository = BaseRepository()
    func validateCred(req : RegisterReq) -> ErrorField {
        let valmag = FieldValidationKt.checkRegisterFieldsValidity(req: req)
        return ErrorField(showErrorAlert: valmag.first == false, errorMessage: valmag.second?.getError() ?? "")
    }
    func signUpCall(reqUser : RegisterReq) async -> BaseResult<User?>{
       return await apiHandler(apiCall: {
                try await base.registerUser(user: reqUser)
            })
    }
}
struct SignupView: View {
    @State private var name : String = ""
    @State private var email : String = ""
    @State private var phone : String = ""
    @State private var password : String = ""
    @State private var confirmPasword : String = ""
    @State private var errorField : ErrorField = ErrorField()
    private let signupHandle = SignUpHandler()
    @Environment(\.dismiss) var dismiss
    var body: some View {
        VStack(spacing:20){
            AvatarImageView()
            TextField(
                "Your name",
                text: $name
            ).textFieldStyle(OutlineTextfieldStyle())
            TextField(
                "Your email",
                text: $email
            ).textFieldStyle(OutlineTextfieldStyle())
            TextField(
                "Your Phone",
                text: $phone
            ).textFieldStyle(OutlineTextfieldStyle())
            SecureField(
                "Passeord",
                text: $password
            ).textFieldStyle(OutlineTextfieldStyle())
            SecureField(
                "Confirm Password",
                text: $confirmPasword
            ).textFieldStyle(OutlineTextfieldStyle())
            Button("Sign up"){
                signUpHandle()
            }.buttonStyle(SigninButtonStyle())
            
        }.padding(20)
        .alert(isPresented: $errorField.showErrorAlert, content: {
                return Alert(title: Text("Error!"), message: Text(errorField.errorMessage),
                             dismissButton: Alert.Button.default(Text("OK"), action: {
                    errorField.showErrorAlert = false
                })
            )
            })
    }
    func signUpHandle() {
        let user =  RegisterReq(
            name: name,
            email : email,
            password : password,
            passwordConfirm: confirmPasword,
            phone : phone, imageUrl: "")
        let validation = signupHandle.validateCred(req: user)
        if validation.showErrorAlert { errorField = validation; return }
        Task{
            let resp = await signupHandle.signUpCall(reqUser: user)
            if(resp.error != nil && !resp.isSuccessful){
                errorField.errorMessage = (resp.error as! NetworkError).getError()
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
