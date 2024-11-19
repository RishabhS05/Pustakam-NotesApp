//
//  BackButton.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 19/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import SwiftUI
struct BackButton: View {
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack{
                Image(systemName: "chevron.backward")
                Text("Back")
            }
        }
        .foregroundColor(.brown)
    }
}
