import AVKit
import SwiftUI


struct SystemControlledPlayerView: UIViewControllerRepresentable {
    func makeCoordinator() -> Coordinator {
        Coordinator(player: self.player)
    }
   
    
    var player: MediaControlledPlayer?
    init(player: MediaControlledPlayer?) {
        self.player = player
    }
    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = player
        controller.showsPlaybackControls = true
        return controller
    }

    func updateUIViewController(_ uiViewController: AVPlayerViewController, context: Context) {
      
    }
    class Coordinator: NSObject, AVPlayerViewControllerDelegate {
           private let player: MediaControlledPlayer?
        init(player: MediaControlledPlayer?) {
            self.player = player
        }
    }
}
