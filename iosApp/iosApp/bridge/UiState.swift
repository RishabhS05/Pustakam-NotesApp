//
//  UiState.swift
//  iosApp
//
//  Created by Rishabh on 10/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//


import Foundation
import shared

/// Single UI-state surface for any bridge call.
enum UiState<T> {
    case idle
    case loading
    case success(T?)
    case failure(BridgeError)
}