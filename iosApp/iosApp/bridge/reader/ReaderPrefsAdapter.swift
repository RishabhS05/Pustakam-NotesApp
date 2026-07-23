//
//  ReaderPrefsAdapter.swift
//  iosApp
//
//  Created by Rishabh on 23/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//

import Foundation
import shared

// 📖 23-Jul-2026: NEW — owns the reader-preferences Kotlin bridge and every Closeable it hands out,
//   exactly like NotesBridgeAdapter. Views never see Closeable/dispose; deinit cancels everything.
//   Reading mode and per-book resume live in the SAME shared DataStore Android writes, so the two
//   platforms stay in sync.
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

    // 📖 23-Jul-2026: reading progress moved OFF preferences onto the document's own MediaContent
    //   row (progressPage/totalPages), so it travels with the content and survives sync. There is no
    //   "load" call anymore — the page comes back with the note, on the media item itself.
    func saveProgress(contentId: String, page: Int, totalPages: Int) {
        bridge.saveReadingProgress(contentId: contentId, page: Int32(page), totalPages: Int32(totalPages))
    }
}
