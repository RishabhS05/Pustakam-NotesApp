    //
    //  SignupView.swift
    //  iosApp
    //
    //  Created by Rishabh Shrivastava on 01/10/24.
    //  Copyright © 2024 orgName. All rights reserved.
    //

import SwiftUI

struct SignupView: View {
    @State private var name : String = ""
    @State private var email : String = ""
    @State private var phone : String = ""
    @State private var password : String = ""
    @State private var confirmPasword : String = ""
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
                text: $email
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
                    // api call
            }.buttonStyle(SigninButtonStyle())
            
            Button("Login") {
                   dismiss()
            } .foregroundColor(.brown)
        }.navigationBarBackButtonHidden().padding(20)
    }
}
#Preview {
    SignupView()
}
