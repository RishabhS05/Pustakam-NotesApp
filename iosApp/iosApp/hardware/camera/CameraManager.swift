//
//  CameraManager.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 21/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import AVFoundation
import CoreImage

class CameraManager  : NSObject {
    
    private var captureSession: AVCaptureSession = AVCaptureSession()
    private var deviceInput : AVCaptureDeviceInput?
    private var videoOutput: AVCaptureVideoDataOutput?
    private let systemPreferredCamera = AVCaptureDevice.default(for: .video)
    private var sessionQueue = DispatchQueue(label: "video.preview.session")
    private var cameraPermission = CameraPermission()
    
    override init() {
           super.init()
           Task {
               await configureSession()
               await startSession()
           }
       }
    private var addToPreviewStream: ((CGImage) -> Void)?
    
    lazy var previewStream: AsyncStream<CGImage> = {
           AsyncStream { continuation in
               addToPreviewStream = { cgImage in
                   continuation.yield(cgImage)
               }
           }
       }()
    
    private func configureSession() async {
       
        guard await cameraPermission.isCameraPermissionGranted,
                 let systemPreferredCamera,
                 let deviceInput = try? AVCaptureDeviceInput(device: systemPreferredCamera)
           else { return }
        
        
        captureSession.beginConfiguration()
         
        
        defer {
               self.captureSession.commitConfiguration()
           }
        
        let videoOutput = AVCaptureVideoDataOutput()
           videoOutput.setSampleBufferDelegate(self, queue: sessionQueue)
        
        guard captureSession.canAddInput(deviceInput) else {
                print("Unable to add device input to capture session.")
                return
            }
        guard captureSession.canAddOutput(videoOutput) else {
                print("Unable to add video output to capture session.")
                return
            }
        captureSession.addInput(deviceInput)
           captureSession.addOutput(videoOutput)
     }
     

     private func startSession() async {
         guard await cameraPermission.isCameraPermissionGranted else { return }
         captureSession.startRunning()
     }
}

extension CMSampleBuffer {
    
    var cgImage: CGImage? {
        let pixelBuffer: CVPixelBuffer? = CMSampleBufferGetImageBuffer(self)
        
        guard let imagePixelBuffer = pixelBuffer else {
            return nil
        }
        
        return CIImage(cvPixelBuffer: imagePixelBuffer).cgImage
    }
    
}



extension CIImage {
    
    var cgImage: CGImage? {
        let ciContext = CIContext()
        
        guard let cgImage = ciContext.createCGImage(self, from: self.extent) else {
            return nil
        }
        
        return cgImage
    }
    
}

extension CameraManager: AVCaptureVideoDataOutputSampleBufferDelegate {
    
    func captureOutput(_ output: AVCaptureOutput,
                       didOutput sampleBuffer: CMSampleBuffer,
                       from connection: AVCaptureConnection) {
        guard let currentFrame = sampleBuffer.cgImage else { return }
        addToPreviewStream?(currentFrame)
    }
    
}


