import shared
import Combine
import SwiftUI
enum NotesViewUIState {
   case showTagSheet
   case showColorSelector
   case none
}

struct NotesUIState {
    // 🔧 15-Jul-2026 iOS parity (summary query): the list renders light summaries
    //   (title/snippet/counts/thumbnail) — full Note contents never load for the list.
    var summaries: [NoteSummary] = []
    var tags: [Tag] = []
    var isLoading = false
    var page = 1
    // 🔧 15-Jul-2026 iOS parity (paging): false once a page returns fewer than pageSize items
    var isNextPage = true
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

           // 🔧 15-Jul-2026 iOS parity (summary query): observe the summaries stream — the
           //   repository keeps it in sync on every save/delete (Android parity).
           // Bridge callbacks arrive on the main thread — direct assignment is safe.
           adapter.observeNoteSummaries { [weak self] summaries in
               self?.state.summaries = summaries
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

       // 🔧 15-Jul-2026 iOS parity (paging): Android NOTES_PAGE_SIZE parity.
       private let pageSize = 20

       /// 🔄 28-Aug-2026 — PULL TO REFRESH: push what is waiting, pull what is new, and say why
       ///   when it fails. The summaries stream updates the list on its own once the pull lands.
       @MainActor
       func syncNow() async {
           state.errorMessage = nil
           if let message = await SyncController.shared.syncNowAsync() {
               state.errorMessage = message
           }
       }

       /// Called from NotesView.onAppear — same name, new internals.
       func getNotesCall() {
           // 🔧 REALTIME-FIX: one-time initial load (see hasLoaded above).
           //   Live updates arrive via observeNoteSummaries; re-fetching here raced saves.
           guard !hasLoaded else { return }
           hasLoaded = true
           fetchPage()
       }

       // 🔧 15-Jul-2026 iOS parity (paging): called when the LAST card appears (see NotesView).
       //   Guards mirror Android: no duplicate in-flight fetches, stop when a page came back short.
       func loadNextPage() {
           guard hasLoaded, state.isNextPage, !state.isLoading else { return }
           fetchPage()
       }

       // 🔧 15-Jul-2026 iOS parity (summary query): one page of summaries instead of full notes.
       private func fetchPage() {
           adapter.getNoteSummaries(page: state.page, limit: pageSize) { [weak self] result in
               guard let self else { return }
               switch result {
               case .loading:
                   self.state.isLoading = true
               case .success(let list):
                   self.state.isLoading = false
                   self.state.page += 1
                   // a short page means the DB is exhausted — stop asking
                   self.state.isNextPage = (list?.count ?? 0) >= self.pageSize
                   // list content arrives via observeNoteSummaries — single source of truth
               case .failure(let error):
                   self.state.isLoading = false
                   self.state.errorMessage = error.message
                   print("getNoteSummaries failed [\(error.code)] \(error.message)")
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
