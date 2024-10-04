//
//  OutlineTextfieldStyle.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 02/10/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct OutlineTextfieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
            configuration
            .padding().frame(height: 40)
                .textInputAutocapitalization(.never)
                .disableAutocorrection(true)
                .overlay(){
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color(UIColor.brown), lineWidth: 1)
                }.submitLabel(.next)
        }
  
}


struct TextFieldStylePreview: PreviewProvider {
   
    static var previews: some View {
        @State var name : String = ""
        TextField("Sign up",   text: $name ).textFieldStyle(OutlineTextfieldStyle()).padding()
    }
}
