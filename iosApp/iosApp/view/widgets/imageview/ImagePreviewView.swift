// 🔧 20-Jul-2026: NEW — full-screen image preview for the note editor (was missing on iOS: tapping
//   an image card did nothing). Whole image FITS (never fills/crops) and is pinch/double-tap
//   zoomable via the shared ZoomableView (DRY). Tap outside / Close dismisses.

import SwiftUI
import shared

struct ImagePreviewView : View {
    let path: String
    var onClose: () -> Void = {}
    @State private var isLandscape: Bool
    
    init(path: String, onClose: @escaping () -> Void = {}) {
        self.path = path
        self.onClose = onClose
        self.isLandscape = (UIApplication.shared.connectedScenes.first as? UIWindowScene)?
            .interfaceOrientation
            .isLandscape ?? false
            
    }
    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            // 🔧 20-Jul-2026: scaledToFit — full image visible; ZoomableView adds pinch/pan/double-tap
            ZoomableView {
                if FileManager.default.fileExists(atPath: path), let ui = UIImage(contentsOfFile: path) {
                    Image(uiImage: ui)
                        .renderingMode(.original)
                        .resizable()
                        .scaledToFit()
                } else {
                    AsyncImage(url: URL(string: path)) { img in img.resizable()
                        .renderingMode(.original).scaledToFit() }
                        placeholder: { ProgressView().tint(.white) }
                }
            }
            .ignoresSafeArea()
            HStack(){
                Button(action: {
                    isLandscape = !isLandscape
                    if(isLandscape){ OrientationManager.shared.set(.landscape) } else{ OrientationManager.shared.set(.portrait)
                    }
                }) {
                    Image(systemName: isLandscape ?  "rectangle.landscape.rotate" : "rectangle.portrait.rotate" )
                        .font(.system(size: 20))
                        .foregroundStyle(.white.opacity(0.9))
                        .padding(16)
                }
                Spacer()
                Button(action: onClose) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 30))
                        .foregroundStyle(.white.opacity(0.9), .black.opacity(0.4))
                        .padding(16)
                }
            }
        }
        .onDisappear(){
            //Todo change orientation according to prefs
            OrientationManager.shared.set(.portrait)
        }
    }
}
