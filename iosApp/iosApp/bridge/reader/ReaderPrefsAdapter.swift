//
//  ReaderPrefsAdapter.swift
//  iosApp
//
//  Created by Rishabh on 23/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//

import Foundation
import shared

@Observable final class ReaderPrefsAdapter {

    private let bridge = ReaderPrefsBridge()
    private var closeables: [Closeable] = []

    deinit {
        closeables.forEach { $0.close() }
        bridge.dispose()
    }

    /// Live reading mode — fires again if it's changed from Settings while the reader is open.
    func observeReadingMode(onChange: @escaping (ReadingMode) -> Void) {
        closeables.append(bridge.observeReadingMode { raw in
            onChange(ReadingMode.from(raw))
        })
    }

    func setReadingMode(_ mode: ReadingMode) {
        bridge.setReadingMode(mode: mode.rawValue)
    }

    func saveProgress(contentId: String, page: Int, totalPages: Int) {
        bridge.saveReadingProgress(contentId: contentId, page: Int32(page), totalPages: Int32(totalPages))
    }
}
