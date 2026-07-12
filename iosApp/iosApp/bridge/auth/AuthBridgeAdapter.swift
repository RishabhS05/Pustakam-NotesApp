//
//  AuthBridgeAdapter.swift
//  iosApp
//
//  🔧 AUTH-FIX: login/signup through use cases (AuthBridge) — replaces the
//  BaseViewModel + apiHandler + `as! NetworkError` path that crashed on
//  non-NetworkError failures.
//

import Foundation
import shared

/// Owns the Kotlin AuthBridge + its Closeables. Same lifecycle rules as NotesBridgeAdapter.
final class AuthBridgeAdapter {

    private let bridge = AuthBridge()
    private var closeables: [Closeable] = []

    deinit {
        closeables.forEach { $0.close() }
        bridge.dispose()
    }

    // 🔧 REALTIME-FIX: writes not retained — deinit was cancelling in-flight auth calls.
    func login(login: Login, onState: @escaping (UiState<User>) -> Void) {
        _ = bridge.login(
            login: login,
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        )
    }

    func signup(request: RegisterReq, onState: @escaping (UiState<User>) -> Void) {
        _ = bridge.signup(
            request: request,
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        )
    }

    func logout(onDone: @escaping () -> Void) {
        bridge.logout(onDone: onDone)
    }

    func observeAuthState(onChange: @escaping (UserPreference) -> Void) {
        closeables.append(bridge.observeAuthState(onChange: onChange))
    }
}
