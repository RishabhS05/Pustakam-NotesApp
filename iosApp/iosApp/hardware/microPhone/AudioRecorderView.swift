
import SwiftUI
import AVFoundation

struct AudioRecorderView : View {
    @StateObject private var audioRecorder = AudioRecorder()
    @StateObject private var audioLevelsMonitor = AudioLevelsMonitor()
    @State private var elapsedTime: TimeInterval = 0.0
    @State private var timer: Timer? = nil
    
    let onDismiss: (() -> Void) = { }
    var onSave: (CapturedMedia) -> Void = { _ in  }
    
    init(onSave :  @escaping (_ media : CapturedMedia) -> Void){
        self.onSave = onSave
    }
    
    var body: some View {
            HStack(spacing: 4) {
                Image(systemName: "microphone.fill")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .padding(8)
                    .background(Color.red)
                    .clipShape(Circle())
                    .foregroundColor(.white)
                
                // Timer Display
                Text(String(format: "%02d : %02d . %02d",
                            Int(elapsedTime / 60),
                            Int(elapsedTime.truncatingRemainder(dividingBy: 60)),
                            Int((elapsedTime * 100).truncatingRemainder(dividingBy: 100))))
                .font(.headline.monospacedDigit())
                .foregroundColor(Theme.Colors.primary)
                    
                // Real-time Wave Animation
                AudioVisualizerView(audioLevelsMonitor: audioLevelsMonitor)
                .frame(height: 30).padding(4)
                    // Play Button
                    Button(action: {
                        if audioRecorder.isRecording {
                            pauseRecording()
                        } else {
                            resumeRecording()
                        }
                    }) {
                        Image(systemName:"record.circle")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                            .foregroundColor(audioRecorder.isRecording ? .red : Theme.Colors.secondary)
                    }
                    .disabled(audioRecorder.audioFileURL == nil)
                    .padding(8)
                   
                    // Disable if there's no recorded file
                    // Stop Playback Button
                    Button(action: {
                        stopRecording()
                        if ((audioRecorder.audioFileURL) != nil){
                            onSave(.audio(audioRecorder.audioFileURL!))
                        }
                        onDismiss()
                    }) {
                        Image(systemName: "stop.fill")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                            .foregroundColor(Theme.Colors.secondary)
                    }
                }
            .frame(height: 40)
            .padding(8)
            .background(Theme.Colors.surface)
            .cornerRadius(12)
            .shadow(radius: 12)
            .padding(8)
            .onAppear(){
            startRecording()
        }
        .onDisappear() {
            onDismiss()
        }
    }
        // Start Recording Function
    private func startRecording() {
        audioRecorder.startRecording()
        audioLevelsMonitor.startLevelsMonitoring()
        startTimer()
    }
    
    private func pauseRecording(){
        audioRecorder.pauseRecording()
        audioLevelsMonitor.stopLevelsMonitoring()
        
        stopTimer()
    }
    private func resumeRecording(){
        audioRecorder.resumeRecording()
        audioLevelsMonitor.startLevelsMonitoring()
        startTimer()
    }
    
        // Stop Recording Function
    private func stopRecording() {
        audioRecorder.stopRecording()
        audioLevelsMonitor.loadAudioFile(url:audioRecorder.audioFileURL ?? nil)
        audioLevelsMonitor.stopLevelsMonitoring()
        stopTimer()
    }
    
        // Timer start or pause Functions
    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 0.01, repeats: true) { _ in
            elapsedTime += 0.01
        }
    }
    // this will call only when recording is completed
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
}

#Preview{
    AudioRecorderView(){_ in }
}

struct BarView: View {
    let height: CGFloat
    
    var body: some View {
        RoundedRectangle(cornerRadius: 2)
            .fill(Color.red)
            .frame(width: 4, height: height * 100)
    }
}

// wave pattern
struct WaveShape: Shape {
    var amplitude: CGFloat
    var phase: CGFloat
    
    var animatableData: AnimatablePair<CGFloat, CGFloat> {
        get { AnimatablePair(amplitude, phase) }
        set {
            amplitude = newValue.first
            phase = newValue.second
        }
    }
    
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let midY = rect.midY
        
        for x in stride(from: 0, to: rect.width, by: 1) {
            let normalizedX = x / rect.width
            let angle = normalizedX * .pi * 2 + phase
            let y = midY + sin(angle) * amplitude * rect.height / 2
            if x == 0 {
                path.move(to: CGPoint(x: x, y: y))
            } else {
                path.addLine(to: CGPoint(x: x, y: y))
            }
        }
        return path
    }
}

struct WaveProgressView: Shape {
    var amplitude: CGFloat
    var phase: CGFloat
    var progress: CGFloat

    var animatableData: AnimatablePair<CGFloat, CGFloat> {
        get { AnimatablePair(phase, progress) }
        set {
            phase = newValue.first
            progress = newValue.second
        }
    }

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let midY = rect.midY

        for x in stride(from: 0, to: rect.width * progress, by: 1) {
            let normalizedX = x / rect.width
            let angle = normalizedX * .pi * 2 + phase
            let y = midY + sin(angle) * amplitude * rect.height / 2
            if x == 0 {
                path.move(to: CGPoint(x: x, y: y))
            } else {
                path.addLine(to: CGPoint(x: x, y: y))
            }
        }

        return path
    }
}


    // pulse pattern
struct PulseShape: Shape {
    var amplitude: CGFloat
    var phase: CGFloat
    
    var animatableData: AnimatablePair<CGFloat, CGFloat> {
        get { AnimatablePair(amplitude, phase) }
        set {
            amplitude = newValue.first
            phase = newValue.second
        }
    }
    
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let midY = rect.midY
        let pulseSpacing: CGFloat = 10
        // Space between pulses
        for x in stride(from: 0, to: rect.width, by: pulseSpacing) {
            let normalizedX = x / rect.width
            let angle = normalizedX * .pi * 2 + phase
            let pulseHeight = sin(angle) * amplitude * rect.height / 2
            
            // Draw a vertical line (pulse)
            let pulseStart = CGPoint(x: x, y: midY - pulseHeight / 2)
            let pulseEnd = CGPoint(x: x, y: midY + pulseHeight / 2)
            path.move(to: pulseStart)
            path.addLine(to: pulseEnd)
        }
        return path
    }
}
