// 🔧 20-Jul-2026: NEW — full-screen image preview for the note editor (was missing on iOS: tapping
//   an image card did nothing). Whole image FITS (never fills/crops) and is pinch/double-tap
//   zoomable via the shared ZoomableView (DRY). Tap outside / Close dismisses.

import SwiftUI
import shared

struct ImagePreviewView: View {
    let path: String
    var onClose: () -> Void = {}

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            // 🔧 20-Jul-2026: scaledToFit — full image visible; ZoomableView adds pinch/pan/double-tap
            ZoomableView {
                if FileManager.default.fileExists(atPath: path), let ui = UIImage(contentsOfFile: path) {
                    Image(uiImage: ui).resizable().scaledToFit()
                } else {
                    AsyncImage(url: URL(string: path)) { img in img.resizable().scaledToFit() }
                        placeholder: { ProgressView().tint(.white) }
                }
            }
            .ignoresSafeArea()

            Button(action: onClose) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 30))
                    .foregroundStyle(.white.opacity(0.9), .black.opacity(0.4))
                    .padding(16)
            }
        }
    }
}
