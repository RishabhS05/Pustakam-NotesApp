//
//  CaneraView.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 21/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import SwiftUI

struct CameraFeedView : View {
    
    @Binding var image: CGImage?
    
    var body: some View {
        
        GeometryReader { geometry in
            if let image = image {
                Image(decorative: image ,scale: 1)
                    .resizable()
                    .scaledToFill()
                    .ignoresSafeArea()
                    .frame(
                        width : geometry.size.width,
                        height: geometry.size.height
                    )
            } else {
                ContentUnavailableView("No camera feed", systemImage: "xmark.circle.fill")
                                    .frame(width: geometry.size.width,
                                           height: geometry.size.height)

            }
        }
    }
}
