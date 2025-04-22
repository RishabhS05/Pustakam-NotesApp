//
//  AudioPlayingWidget.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 23/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import shared
struct AudioPlayingWidget : View {
    var content : NoteContentModel.MediaContent
    var actionEdit : () -> Void = {}
    var actionClick : () -> Void
    var body: some View {
        Text("Hello, World!")
    }
}
