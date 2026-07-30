//
//  AppOrientation.swift
//  iosApp
//
//  Created by Rishabh on 30/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//


import UIKit

enum AppOrientation {
    case portrait
    case landscape
}

final class OrientationManager {

    static let shared = OrientationManager()

    private init() {}

    var isLandscape: Bool {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene else {
            return false
        }
        return scene.interfaceOrientation.isLandscape
    }

    func toggle() {
        set(isLandscape ? .portrait : .landscape)
    }

    func set(_ orientation: AppOrientation) {

        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene else {
            return
        }

        let mask: UIInterfaceOrientationMask =
            orientation == .portrait ? .portrait : .landscapeRight

        OrientationLock.shared.orientation = mask

        let preferences = UIWindowScene.GeometryPreferences.iOS(
            interfaceOrientations: mask
        )

        scene.requestGeometryUpdate(preferences)

        scene.keyWindow?.rootViewController?
            .setNeedsUpdateOfSupportedInterfaceOrientations()
    }
}