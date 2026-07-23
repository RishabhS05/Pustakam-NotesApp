import SwiftUI



struct BookLoadingAnimation: View {

    let pageCorner: CGFloat = 4

    let pageInset: CGFloat = 3

    let animationDuration: Double = 1.8

    var bookWidth: CGFloat = 80
    var bookHeight: CGFloat = 70
    let cornerRadius: CGFloat = 6
    @State
    private var progress: CGFloat = 0

    var body: some View {

        ZStack {

            //----------------------------------------
            // Book Cover
            //----------------------------------------

            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(Color.accentColor) // Replace with your theme secondary color
                .shadow(
                    color: .black.opacity(0.25),
                    radius: 12,
                    x: 0,
                    y: 8
                )

            //----------------------------------------
            // Left Page
            //----------------------------------------

            ZStack {

                BookPage(isLeft: true)

                WritingCanvas(progress: progress)
                    .padding(.horizontal, 2)
                    .padding(.vertical, 4)

            }
            .frame(
                width: bookWidth * 0.48,
                height: bookHeight * 0.92
            )
            .offset(x: -bookWidth * 0.24 + pageInset)

            //----------------------------------------
            // Right Page
            //----------------------------------------

            ZStack {

                BookPage(isLeft: false)

                PencilView(progress: progress)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 4)

            }
            .frame(
                width: bookWidth * 0.48,
                height: bookHeight * 0.92
            )
            .offset(x: bookWidth * 0.24 - pageInset)

            //----------------------------------------
            // Book Spine
            //----------------------------------------

            Rectangle()
                .fill(Color.accentColor)
                .frame(width: 2, height: bookHeight * 0.93)

            //----------------------------------------
            // Bottom Shadow
            //----------------------------------------

            BottomShadow()
                .frame(
                    width: bookWidth,
                    height: 10
                )
                .offset(y: bookHeight * 0.52)

        }
        .frame(
            width: bookWidth,
            height: bookHeight
        )
        .onAppear {

            progress = 0

            withAnimation(
                .linear(duration: animationDuration)
                    .repeatForever(autoreverses: false)
            ) {

                progress = 1
            }
        }
    }
}

#Preview {

    ZStack {

        Color(.systemGray6)
            .ignoresSafeArea()

        BookLoadingAnimation(
            bookWidth: 120,
            bookHeight: 100
        )
    }
}

struct BookPage: View {

    let isLeft: Bool
    let pageCorner: CGFloat = 4

    var body: some View {

        ZStack {

            // Paper
            RoundedRectangle(
                cornerRadius: pageCorner,
                style: .continuous
            )
                .fill(
                    LinearGradient(
                        colors: [
                            .white,
                            Color(red: 0.965, green: 0.965, blue: 0.965),
                            .white
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )

            // Ruled lines
            GeometryReader { geometry in

                Canvas { context, size in

                    let gap = size.height / 9

                    var y = gap

                    for _ in 0..<8 {

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
                            with: .color(
                                Color(
                                    red: 0.91,
                                    green: 0.91,
                                    blue: 0.91
                                )
                            ),
                            lineWidth: 1
                        )

                        y += gap
                    }
                }
            }
        }
        .clipShape(pageShape)
    }

    private var pageShape: some Shape {

        UnevenRoundedRectangle(

            topLeadingRadius: isLeft ? pageCorner : 0,

            bottomLeadingRadius: isLeft ? pageCorner : 0,

            bottomTrailingRadius: isLeft ? 0 : pageCorner,

            topTrailingRadius: isLeft ? 0 : pageCorner
        )
    }
}

#Preview {

    HStack(spacing: 6) {

        BookPage(isLeft: true)
            .frame(width: 80, height: 120)

        BookPage(isLeft: false)
            .frame(width: 80, height: 120)
    }
    .padding()
}

//----------- new WritingCanvas

struct WritingCanvas: View {

    /// 0.0 -> 1.0
    let progress: CGFloat

    var body: some View {
        Canvas { context, size in

            let totalLines = 3

            let left = size.width * 0.12
            let right = size.width * 0.88

            let lineSpacing = size.height * 0.22
            let top = size.height * 0.22

            let overall = progress * CGFloat(totalLines)

            let currentLine = min(Int(overall), totalLines - 1)
            let lineProgress = overall - CGFloat(currentLine)

            for i in 0..<totalLines {

                let y = top + CGFloat(i) * lineSpacing

                let endX: CGFloat

                if i < currentLine {
                    endX = right
                } else if i == currentLine {
                    endX = left + (right - left) * lineProgress
                } else {
                    endX = left
                }

                var path = Path()

                path.move(to: CGPoint(x: left, y: y))
                path.addLine(to: CGPoint(x: endX, y: y))

                context.stroke(
                    path,
                    with: .color(Theme.Colors.copper),
                    style: StrokeStyle(
                        lineWidth: 3,
                        lineCap: .round
                    )
                )
            }
        }
    }
}

struct BottomShadow: View {

    var body: some View {

        GeometryReader { geo in

            Ellipse()
                .fill(
                    Color.black.opacity(0.12)
                )
                .frame(
                    width: geo.size.width * 0.64,
                    height: geo.size.height * 0.8
                )
                .position(
                    x: geo.size.width * 0.50,
                    y: geo.size.height * 0.50
                )
        }
        .allowsHitTesting(false)
    }
}

#Preview {

    VStack {

        Spacer()

        BottomShadow()
            .frame(width: 120, height: 12)
    }
    .padding()
}

#Preview {

    VStack(spacing: 20) {

        WritingCanvas(progress: 0.15)
            .frame(width: 120, height: 140)

        WritingCanvas(progress: 0.55)
            .frame(width: 120, height: 140)

        WritingCanvas(progress: 1.0)
            .frame(width: 120, height: 140)
    }
    .padding()
}



//--------------new PencilView
struct PencilView: View {

    /// Progress between 0...1
    let progress: CGFloat

    private let totalLines = 3

    var body: some View {

        GeometryReader { geo in

            let overall = progress * CGFloat(totalLines)

            let currentLine = min(Int(overall), totalLines - 1)

            let lineProgress = overall - CGFloat(currentLine)

            let x = geo.size.width * 0.18 + lineProgress * geo.size.width * 0.52

            let y = geo.size.height * 0.22 + CGFloat(currentLine) * geo.size.height * 0.22

            Image("pencil")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 18, height: 18)
                .position(x: x, y: y)
        }
    }
}

#Preview {

    VStack(spacing: 20) {

        PencilView(progress: 0.1)
            .frame(width: 120, height: 120)

        PencilView(progress: 0.5)
            .frame(width: 120, height: 120)

        PencilView(progress: 0.9)
            .frame(width: 120, height: 120)
    }
    .padding()
}
