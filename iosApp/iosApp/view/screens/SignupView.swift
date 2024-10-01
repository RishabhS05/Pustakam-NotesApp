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
    
    var body: some View {
        VStack(spacing:20){
            AvatarImageView()
            Text("Signup").font(/*@START_MENU_TOKEN@*/.title/*@END_MENU_TOKEN@*/.bold()).foregroundColor(.brown)
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
            Button("Submit"){
                    // api call
            }.font(.title2).frame(width: 120)
            .padding(8).background(.brown)
            .foregroundColor(.white)
            .cornerRadius(12)
            
            Button("Login") {
                    // api call
            } .foregroundColor(.brown)
        }.frame(width: .infinity,alignment: .top).padding(20)
    
    }
}
#Preview {
    SignupView()
}
