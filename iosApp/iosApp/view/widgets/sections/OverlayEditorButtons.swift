//
//  OverlayEditorButtons.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 19/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import SwiftUI

struct OverlayEditorButtons : View {
    @State private var showArrow: Bool = false
    @State private var offset: CGFloat = 0 // Animatable offset
    var showDelete: Bool = false
    var onMediaCapture: () -> Void = {}
    var onShare: () -> Void = {}
    var onRecordMic: () -> Void = {}
    var onAddTextField : () -> Void = {}
    var onArrowButton: () -> Void = {}
    var body: some View {
        VStack(alignment: .center) {
              if showArrow {
                  VStack(spacing: 8) {
                      ActionButton(iconName: "mic.fill", action: {
                          onArrowButton()
                          onRecordMic()
                      })
                      ActionButton(iconName: "camera.circle", action: {
                          onArrowButton()
                          onMediaCapture()
                      })

                      ActionButton(iconName: "note.text.badge.plus", action: {
                          onArrowButton()
                          onAddTextField()
                      })
                      ActionButton(iconName: "square.and.arrow.up", action: {
                          onArrowButton()
                          onShare()
                      })

                  }
                  .padding(8)
                  .background(Color.gray.opacity(0.4))
                  .cornerRadius(12)
                  .offset(x: offset)
                  .animation(.spring(), value: offset) // Smooth transition
              }
                // Arrow Button
            ActionButton(iconName: showArrow ? "arrow.left" : "arrow.right",action: {
                    onArrowButton()
                    withAnimation {
                        showArrow.toggle()
                        offset = showArrow ? 0 : -100 // Adjust offset if needed
                    }
                }) 
            
        }.frame(maxWidth: .infinity,maxHeight: .infinity,alignment:.bottomTrailing)
            .swipe(left : {
                withAnimation {
                    showArrow.toggle()
                    offset = showArrow ? 0 : -100 // Adjust offset if needed
                }
            }, right: {
                withAnimation {
                    showArrow.toggle()
                    offset = showArrow ? 0 : -100 // Adjust offset if needed
                }
            })
            .padding(.leading, 8)
      }
}
