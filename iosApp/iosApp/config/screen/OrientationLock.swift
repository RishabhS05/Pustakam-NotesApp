//
//  OrientationLock.swift
//  iosApp
//
//  Created by Rishabh on 30/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//

import UIKit


class OrientationLock {
    static let shared = OrientationLock()

    var orientation: UIInterfaceOrientationMask = .all
}

func application(
    _ application: UIApplication,
    supportedInterfaceOrientationsFor window: UIWindow?
) -> UIInterfaceOrientationMask {
    OrientationLock.shared.orientation
}
