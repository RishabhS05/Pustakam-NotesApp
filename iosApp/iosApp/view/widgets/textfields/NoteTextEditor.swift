//
//  NoteTextEditor.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 16/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct NoteTextEditor: View {
    @Binding var text: String
    var placeholder: String = ""
    @State var leftpadding: CGFloat = 10
    @State var fontSize: CGFloat = 16
    @State var isRulledEnabled: Bool = false
    let lineColor = Color.gray.opacity(0.5)
    let marginColor = Color.red
    let lineSpacing: CGFloat = 28
    var body: some View {
        ZStack(alignment: .topLeading){
            if isRulledEnabled {
                RulledPage(
                    lineColor: lineColor,
                    marginColor:
                        marginColor, fontSize: fontSize,
                    lineSpacing: lineSpacing,
                    leftpadding: leftpadding
                )
                .background(Color.white)
                .ignoresSafeArea()
            }
            if text.isEmpty {
                Text(placeholder)
                    .padding(.leading, leftpadding)
                    .frame(minHeight: 20)
                    .lineSpacing(10)
                    .font(.system(size: fontSize, weight: .light))
                    .foregroundColor(.gray)
            }
            TextEditor(text: $text)
                .padding(.leading, leftpadding)
                .accentColor(.brown)
                .font(.system(size: fontSize))
                .scrollContentBackground(.hidden)
            }
        
    }
}
