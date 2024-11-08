    //
    //  noteView.swift
    //  iosApp
    //
    //  Created by Rishabh Shrivastava on 07/11/24.
    //  Copyright © 2024 orgName. All rights reserved.
    //

import SwiftUI
import shared
struct NoteView : View {
    let note: Note
    let onClick: () -> Void

    var body: some View {
        VStack{
            Text(note.title ?? "")
                .font(.system(size: 17, weight: .bold))
                .padding(4)
                .frame(maxWidth: .infinity)
                .multilineTextAlignment(.center)
            
            Text(note.description_ ?? "")
                .font(.system(size: 17, weight: .regular))
                .padding(4)
                .frame(maxWidth: .infinity)
                .multilineTextAlignment(.center)
        }
        .background(RoundedRectangle(cornerRadius: 10)
            .stroke(Color.gray, lineWidth: 0.5))
        .onTapGesture {
            onClick()
        }  .frame(maxHeight: 300)
    }
}
