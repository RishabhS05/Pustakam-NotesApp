import SwiftUI

struct BookLoadingView: View {

    var width: CGFloat = 80
    var height: CGFloat = 70

    @State private var writeProgress: CGFloat = 0

    private let totalLines = 3

    var body: some View {

        ZStack {

            // Book Cover
            RoundedRectangle(cornerRadius: 6)
                .fill(Color.accentColor)

            //----------------------------------
            // Left Page
            //----------------------------------

            HStack(spacing: 0) {

                ZStack {

                    PageView(isLeft: true)

                    WritingCanvas(
                        progress: writeProgress,
                        totalLines: totalLines
                    )
                    .padding(.horizontal, 6)
                    .padding(.vertical, 6)
                }
                .frame(maxWidth: .infinity)

                //----------------------------------
                // Spine
                //----------------------------------

                Rectangle()
                    .fill(Color.accentColor)
                    .frame(width: 2)

                //----------------------------------
                // Right Page
                //----------------------------------

                ZStack {

                    PageView(isLeft: false)

                    PencilView(
                        progress: writeProgress,
                        totalLines: totalLines
                    )
                }
                .frame(maxWidth: .infinity)
            }
            .padding(3)
            
            Ellipse()
                .fill(Color.black.opacity(0.12))
                .frame(width: width * 0.64,
                       height: 8)
                .offset(y: height / 2 - 2)
        }
        .frame(width: width, height: height)
        .shadow(color: .black.opacity(0.20), radius: 12)
        .onAppear {

            withAnimation(
                .linear(duration: 1.8)
                .repeatForever(autoreverses: false)
            ) {
                writeProgress = 1
            }
        }
    }
}
struct PageView: View {
    
    let isLeft: Bool

    var body: some View {

        GeometryReader { geo in

            ZStack {

                LinearGradient(
                    colors: [
                        .white,
                        Color(.systemGray6),
                        .white
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )

                Canvas { context, size in

                    let gap = size.height / 9

                    for i in 1...8 {

                        let y = CGFloat(i) * gap

                        var path = Path()

                        path.move(
                            to: CGPoint(x: 6, y: y)
                        )

                        path.addLine(
                            to: CGPoint(
                                x: size.width - 6,
                                y: y
                            )
                        )

                        context.stroke(
                            path,
                            with: .color(Color.gray.opacity(0.25)),
                            lineWidth: 1
                        )
                    }
                }
            }
            .clipShape(
                UnevenRoundedRectangle(
                    topLeadingRadius: isLeft ? 4 : 0,
                    bottomLeadingRadius: isLeft ? 4 : 0,
                    bottomTrailingRadius: isLeft ? 0 : 4,
                    topTrailingRadius: isLeft ? 0 : 4
                )
            )
        }
    }
}
struct WritingCanvas: View {
    
    let progress: CGFloat
    let totalLines: Int

    var body: some View {

        Canvas { context, size in

            let left = size.width * 0.12
            let right = size.width * 0.88

            let spacing = size.height * 0.22
            let top = size.height * 0.22

            let overall = progress * CGFloat(totalLines)

            let current = min(
                Int(overall),
                totalLines - 1
            )

            let lineProgress = overall - CGFloat(current)

            for i in 0..<totalLines {

                let y = top + CGFloat(i) * spacing

                let endX: CGFloat

                if i < current {

                    endX = right

                } else if i == current {

                    endX = left + (right - left) * lineProgress

                } else {

                    endX = left
                }

                var path = Path()

                path.move(
                    to: CGPoint(x: left, y: y)
                )

                path.addLine(
                    to: CGPoint(x: endX, y: y)
                )

                context.stroke(
                    path,
                    with: .color(.green),
                    style: StrokeStyle(
                        lineWidth: 3,
                        lineCap: .round
                    )
                )
            }
        }
    }
}
struct PencilView: View {

    let progress: CGFloat
    let totalLines: Int

    var body: some View {

        GeometryReader { geo in

            let overall = progress * CGFloat(totalLines)

            let currentLine = min(
                Int(overall),
                totalLines - 1
            )

            let lineProgress = overall - CGFloat(currentLine)

            let left = geo.size.width * 0.12
            let right = geo.size.width * 0.88

            let x = left + (right - left) * lineProgress
            let y = CGFloat(currentLine) * 14

            Image(systemName: "pencil.tip")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(Color.green)
                .rotationEffect(.degrees(0))
                .offset(x: x, y: y)
                .frame(
                    maxWidth: .infinity,
                    maxHeight: .infinity,
                    alignment: .center
                )
        }
    }
}
