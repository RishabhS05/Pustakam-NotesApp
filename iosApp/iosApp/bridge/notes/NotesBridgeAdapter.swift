//
//  NotesBridgeAdapter.swift
//  iosApp
//
//  Created by Rishabh on 10/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//


import Foundation
import shared

/// Owns the Kotlin bridge and every Closeable it hands out.
/// ViewModels never see Closeable/dispose — deinit here cancels everything.
final class NotesBridgeAdapter {

    private let bridge = NotesBridge()
    private var closeables: [Closeable] = []

    deinit {
        closeables.forEach { $0.close() }
        bridge.dispose()
    }

    // MARK: - Reactive state (replaces NoteRepositoryHelper)

    func observeNotes(onChange: @escaping (Notes) -> Void) {
        closeables.append(bridge.observeNotes(onChange: onChange))
    }

    func observeTags(onChange: @escaping ([Tag]) -> Void) {
        closeables.append(bridge.observeTags(onChange: onChange))
    }

    // MARK: - One-shot calls (notes-list slice)

    func getNotes(page: Int, onState: @escaping (UiState<Notes>) -> Void) {
        closeables.append(bridge.getNotes(
            page: Int32(page),
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        ))
    }
    
    // MARK: - Editor slice

    func readNote(noteId: String?, onState: @escaping (UiState<Note>) -> Void) {
        closeables.append(bridge.readNote(
            noteId: noteId,
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        ))
    }

    // 🔧 REALTIME-FIX: write Closeables are NOT retained — deinit closed them,
    //   cancelling in-flight saves/deletes when the screen died (onDisappear-save).
    func createOrUpdateNote(note: Note, onState: @escaping (UiState<Note>) -> Void) {
        _ = bridge.createOrUpdateNote(
            note: note,
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        )
    }

    func deleteNote(noteId: String?, onState: @escaping (UiState<KotlinBoolean>) -> Void) {
        _ = bridge.deleteNote(
            noteId: noteId,
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        )
    }

    // 🔧 14-Jul-2026: NEW — delete a single content block (image/video/audio/text) of a note.
    //   The note itself stays. Wraps Kotlin NotesBridge.deleteNoteContent (existed, was never
    //   called from any UI path). Write call — not retained, survives screen death (see note above).
    //   Usage: adapter.deleteNoteContent(contentId: content.id) { state in ... }
    func deleteNoteContent(contentId: String?, onState: @escaping (UiState<KotlinBoolean>) -> Void) {
        _ = bridge.deleteNoteContent(
            contentId: contentId,
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        )
    }

    
    /***
     CRUD For TAGS
     */
    
    
    
    func createTag(tag: Tag, onState: @escaping (UiState<Tag>) -> Void) {
        _ = bridge.createTag(   // 🔧 write — not retained (see above)
            tag: tag,
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        )
    }
    
    // 🔧 F6: removed unused `tag` parameter — getTags takes no input
    func getTags(onState: @escaping (UiState<[Tag]>) -> Void) {
        closeables.append(bridge.getTags(
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        ))
    }
    
    func deleteTag(tag: Tag, onState: @escaping (UiState<KotlinBoolean>) -> Void) {
        _ = bridge.deleteTag(   // 🔧 write — not retained
            tagId: tag.id,
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError: { onState(.failure($0)) }
        )
    }
    func updateTag(tag: Tag, onState: @escaping (UiState<Tag>) -> Void) {
        _ = bridge.updateTag(   // 🔧 write — not retained
            tag: tag,
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        )
    }
    
}
