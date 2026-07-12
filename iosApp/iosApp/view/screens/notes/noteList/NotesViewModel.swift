import shared
import Combine
import SwiftUI
enum NotesViewUIState {
   case showTagSheet
   case showColorSelector
   case none
}

struct NotesUIState {
    var notes: [Note] = []
    var tags: [Tag] = []
    var isLoading = false
    var page = 1
    var showSheet = false
    var showColorPalette = false
    var tagName = ""
    var color = Color.red
    var errorMessage: String? = nil
}
class NotesViewModel : ObservableObject {
    @Published var state = NotesUIState()

    private let adapter: NotesBridgeAdapter
    // 🔧 REALTIME-FIX: fetch from DB only ONCE (Android parity — hasLoaded guard).
    //   onAppear fires BEFORE the editor's onDisappear-save, so re-fetching on every
    //   appear raced the save: the stale DB read landed last and REPLACED repo state,
    //   wiping the just-saved note. After first load, repo state is the source of truth.
    private var hasLoaded = false

    /// DI: default arg keeps existing `NotesViewModel()` call sites compiling;
       /// tests/previews pass a fake. AppDI composition root takes over later.
       init(adapter: NotesBridgeAdapter = NotesBridgeAdapter()) { // 🔧 removed trailing comma (compile error on most Swift versions)
           self.adapter = adapter

           // Live notes list (replaces NoteRepositoryHelper.noteListStateHelper).
           // Bridge callbacks arrive on the main thread — direct assignment is safe.
           adapter.observeNotes { [weak self] notesState in
               self?.state.notes = notesState.notes as? [Note] ?? []
           }

           // Live tags (replaces getTagsHelper). The old clear()-all-notes quirk
           // on tag emissions is intentionally dropped (rev 1 §6.2).
           adapter.observeTags { [weak self] tags in
               self?.state.tags = tags
           }

           // 🔧 REALTIME-FIX: load tags from DB once — previously NOTHING on iOS ever
           //   called getTags, so after app relaunch the tag chips stayed empty until
           //   a tag was created. NOT_FOUND (empty DB) is a valid quiet outcome here.
           adapter.getTags { _ in }
       }
    // MARK: - API triggers

       /// Called from NotesView.onAppear — same name, new internals.
       func getNotesCall() {
           // 🔧 REALTIME-FIX: one-time initial load (see hasLoaded above).
           //   Live updates arrive via observeNotes; re-fetching here raced saves.
           guard !hasLoaded else { return }
           hasLoaded = true
           adapter.getNotes(page: state.page) { [weak self] result in
               guard let self else { return }
               switch result {
               case .loading:
                   self.state.isLoading = true
               case .success:
                   self.state.isLoading = false
                   // list content arrives via observeNotes — single source of truth
               case .failure(let error):
                   self.state.isLoading = false
                   self.state.errorMessage = error.message
                   print("getNotes failed [\(error.code)] \(error.message)")
               case .idle:
                   break
               }
           }
       }

    
    func createTag() {
            guard !state.tagName.isEmpty else { return }
            let tag = Tag(label: state.tagName, color: state.color.tohexColor())
            adapter.createTag(tag: tag) { [weak self] result in
                guard let self else { return }
                switch result {
                case .success:
                    self.state.tagName = ""            // tag list refreshes via observeTags
                case .failure(let error):
                    self.state.errorMessage = error.message
                    print("createTag failed [\(error.code)] \(error.message)")
                case .loading, .idle:
                    break
                }
            }
        }
}
