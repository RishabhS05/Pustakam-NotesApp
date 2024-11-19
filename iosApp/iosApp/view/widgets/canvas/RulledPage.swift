//
//  RulledPage.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 19/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
struct RulledPage: View {
    
    let lineColor : Color
    let marginColor : Color
    let fontSize: CGFloat
    let lineSpacing: CGFloat
    let leftpadding : CGFloat
    
    init (lineColor: Color = Color.gray.opacity(0.5), marginColor: Color = Color.red, fontSize: CGFloat = 18, lineSpacing : CGFloat = 28, leftpadding: CGFloat = 0) {
        self.lineColor = lineColor
        self.marginColor = marginColor
        self.fontSize = fontSize
        self.lineSpacing = lineSpacing
        self.leftpadding = leftpadding
    }
    
    var body: some View {
            Canvas { context, size in
                let startX: CGFloat = 80
                var y: CGFloat = lineSpacing
                
                    // Draw horizontal lines across the page
                while y < size.height {
                    context.stroke(
                        Path { path in
                            path.move(to: CGPoint(x: 0, y: y))
                            path.addLine(to: CGPoint(x: size.width, y: y))
                        },
                        with: .color(lineColor),
                        lineWidth: 1
                    )
                    y += lineSpacing
                }
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: startX + 6, y: 0))
                        path.addLine(to: CGPoint(x: startX + 6, y: size.height))
                    },
                    with: .color(marginColor),
                    lineWidth: 2
                )
                
                    // Draw vertical margin line
                context.stroke(
                    Path { path in
                        path.move(to: CGPoint(x: startX, y: 0))
                        path.addLine(to: CGPoint(x: startX, y: size.height))
                    },
                    with: .color(marginColor),
                    lineWidth: 2
                )
            }
            .background(Color.white)
            .ignoresSafeArea()
        }
}
