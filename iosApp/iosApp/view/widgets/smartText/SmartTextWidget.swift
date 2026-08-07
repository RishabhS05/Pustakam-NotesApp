import SwiftUI
import shared

/**
 Rich text editor for a note's TextContent. All behaviour comes from the shared
 SmartTextReducer; this file only renders state and forwards intents.
 */
struct SmartTextWidget: View {

    let document: RichDocument
    var readOnly: Bool = false
    var showKeyboardToolbar: Bool = true
    let onDocumentChange: (RichDocument) -> Void

    @StateObject private var store: SmartTextStore
    @Environment(\.colorScheme) private var scheme
    @Environment(\.openURL) private var openURL

    init(
        document: RichDocument,
        readOnly: Bool = false,
        showKeyboardToolbar: Bool = true,
        onDocumentChange: @escaping (RichDocument) -> Void
    ) {
        self.document = document
        self.readOnly = readOnly
        self.showKeyboardToolbar = showKeyboardToolbar
        self.onDocumentChange = onDocumentChange
        _store = StateObject(
            wrappedValue: SmartTextStore(document: document, onDocumentChange: onDocumentChange)
        )
    }

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .top) {
                VStack(alignment: .leading, spacing: SmartTextMetrics.blockSpacing) {
                    ForEach(store.document.blocks, id: \.id) { block in
                        SmartTextBlockView(
                            block: block,
                            isFocused: block.id == store.focusedBlockId,
                            listNumber: store.listNumber(for: block.id),
                            searchRanges: store.searchRanges(for: block.id),
                            activeSearchRange: store.activeSearchRange(for: block.id),
                            readOnly: readOnly,
                            accessory: keyboardAccessory,
                            dismissToken: store.keyboardDismissToken,
                            onIntent: store.dispatch
                        )
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if store.state.showSelectionToolbar && !readOnly {
                    SmartTextSelectionToolbar(
                        toolbar: store.toolbar,
                        expanded: store.selectionToolbarExpanded,
                        onAction: handle
                    )
                    .padding(.top, 4)
                    .zIndex(10)
                }
            }

        }
        .onChange(of: document) { _, newValue in
            store.syncExternal(document: newValue)
        }
        // mirrors Cut/Copy into the system pasteboard without leaking UIKit types into shared code
        .onChange(of: store.state.clipboard) { _, payload in
            if let text = payload?.text { UIPasteboard.general.string = text }
        }
        .sheet(isPresented: Binding(
            get: { store.sheet.isPresented },
            set: { if !$0 { store.sheet = .none } }
        )) {
            SmartTextSheetHost(store: store)
        }
    }

    /// Built here and handed to every block so UIKit can pin it above the keyboard.
    private var keyboardAccessory: AnyView? {
        guard showKeyboardToolbar, !readOnly else { return nil }
        return AnyView(
            SmartTextKeyboardToolbar(
                toolbar: store.toolbar,
                expanded: store.state.isToolbarExpanded,
                canUndo: store.state.canUndo,
                canRedo: store.state.canRedo,
                onAction: handle
            )
            .environment(\.colorScheme, scheme)
        )
    }

    private func handle(_ action: ToolbarAction) {
        store.handle(action) { url in openURL(url) }
    }
}

/// Convenience wrapper for callers that still hold plain text plus its stored metadata.
struct SmartTextContentWidget: View {

    let text: String
    let metadata: RichTextMetadata?
    var readOnly: Bool = false
    var showKeyboardToolbar: Bool = true
    let onDocumentChange: (RichDocument) -> Void

    var body: some View {
        SmartTextWidget(
            document: RichTextCodec.shared.documentFrom(text: text, metadata: metadata),
            readOnly: readOnly,
            showKeyboardToolbar: showKeyboardToolbar,
            onDocumentChange: onDocumentChange
        )
    }
}
