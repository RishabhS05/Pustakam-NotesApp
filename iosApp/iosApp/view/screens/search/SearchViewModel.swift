import Foundation
import shared

// 🔧 15-Jul-2026 iOS parity (Phase 2.2): state + intents for the search screen (Android
//   SearchViewModel parity). MVI: query intent -> debounced FTS5 search -> results state.
//   Results are NoteSummary items — content matches carry a snippet.
struct SearchUIState {
    var query: String = ""
    var results: [NoteSummary] = []
    var isSearching = false
    var errorMessage: String? = nil
}

// 🔧 15-Jul-2026 iOS parity (Phase 2.2): NEW — drives the (previously stub) SearchView.
//   How to use: bind `state.query` to the search field via onQueryChange(text); each change is
//   debounced 300ms (the pending task is cancelled), then adapter.searchNotes hits the shared
//   FTS5 index and results land in state on the main thread.
final class SearchViewModel: ObservableObject {
    @Published var state = SearchUIState()

    private let adapter: NotesBridgeAdapter
    private var debounceTask: Task<Void, Never>? = nil

    init(adapter: NotesBridgeAdapter = NotesBridgeAdapter()) {
        self.adapter = adapter
    }

    func onQueryChange(_ query: String) {
        state.query = query
        debounceTask?.cancel()
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else {
            state.results = []
            state.isSearching = false
            return
        }
        debounceTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 300_000_000)   // debounce: wait for a typing pause
            guard !Task.isCancelled else { return }
            await MainActor.run { self?.search(query) }
        }
    }

    private func search(_ query: String) {
        adapter.searchNotes(query: query) { [weak self] result in
            guard let self else { return }
            switch result {
            case .loading:
                self.state.isSearching = true
            case .success(let list):
                self.state.isSearching = false
                self.state.results = list as? [NoteSummary] ?? []
            case .failure(let error):
                self.state.isSearching = false
                self.state.errorMessage = error.message
                print("searchNotes failed [\(error.code)] \(error.message)")
            case .idle:
                break
            }
        }
    }
}
