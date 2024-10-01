//
//  LoginView.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 01/10/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct LoginView: View {
    @State private var password : String = ""
    @State private var email : String = ""
    var body: some View {
    
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
                // api call
        }.font(.title2).frame(width: 120)
        .padding(8).background(.brown)
        .foregroundColor(.white)
        .cornerRadius(12)
    }
}

#Preview {
    LoginView()
}
