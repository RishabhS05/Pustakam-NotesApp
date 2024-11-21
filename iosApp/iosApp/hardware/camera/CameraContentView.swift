//
//  CameraContentView.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 21/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import SwiftUI

struct CameraContentView : View {
    @State private var cameraHandler = CameraHandler()
    
    var body: some View {
        CameraFeedView(image : $cameraHandler.currentFrame)
            .ignoresSafeArea()
    }
}
