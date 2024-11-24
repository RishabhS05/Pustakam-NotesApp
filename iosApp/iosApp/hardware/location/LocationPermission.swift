//
//  LocationPermission.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 21/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import CoreLocation
import SwiftUI
class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    private var locationManager = CLLocationManager()
    @Published var isAuthorized = false

    override init() {
        super.init()
        locationManager.delegate = self
    }

    func requestLocationPermission() {
        if CLLocationManager.locationServicesEnabled() {
            locationManager.requestWhenInUseAuthorization()
        }
    }

    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        DispatchQueue.main.async {
            self.isAuthorized = status == .authorizedWhenInUse || status == .authorizedAlways
        }
    }
    
    func showAlert(onDismiss: @escaping () -> Void) -> Alert {
        return Alert(
            title: Text("Location Access Required"),
            message: Text("Please enable location access in Settings to use this feature."),
            primaryButton: .default(Text("Allow"), action: {
               openAppSettings()
               onDismiss()
            }),secondaryButton: .default(Text("Dont Allow"), action: {
                onDismiss()
            })
        )
    }
}
