import SwiftUI
import shared

// 🔧 15-Jul-2026 iOS parity (Phase 2.2): IMPLEMENTED — this was a "Hello, World!" stub. It is now
//   the FTS5-backed note search (Android SearchView parity): type to search all note text and
//   titles; results are summary cards (snippet from the matching block); tapping one opens the
//   note in the editor via a contents-less stub (the editor re-reads by id).
struct SearchView: View {
    @StateObject private var viewModel = SearchViewModel()
    @Environment(Router.self) private var router: Router

    var body: some View {
        VStack(spacing: 8) {
           SearchBar(
            text :  Binding(
                get: { viewModel.state.query },
                set: { viewModel.onQueryChange($0) }
            ),
            placeholder: "Search notes, tags, titles…", showMicrophone: true, onMicrophoneClick: {
                
            }
           )
            .padding(10)
            if viewModel.state.query.trimmingCharacters(in: .whitespaces).isEmpty {
                centerHint("Search across every note — text, titles.")
            } else if viewModel.state.results.isEmpty && !viewModel.state.isSearching {
                centerHint("No notes match \"\(viewModel.state.query)\".")
            } else {
                ScrollView {
                    StaggeredGrid(columns: 2, items: viewModel.state.results, spacing: 12) { summary in
                        NoteBookView(summary: summary) {
                            router.navigate(to: .NoteEditor(noteId: summary.id))
                        }
                    }
                }
            }
        }
        .padding(.top, 8)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Text("Search")
            }
        }
    }

    private func centerHint(_ text: String) -> some View {
        VStack {
            Spacer()
            Text(text)
                .font(.system(size: 14))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(24)
            Spacer()
        }
    }
}

#Preview {
    SearchView().environment(Router())
}
