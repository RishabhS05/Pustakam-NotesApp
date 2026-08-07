import SwiftUI
import UIKit
import shared

// the only place iOS mutates the editor — every change goes through the shared reducer
final class SmartTextStore: ObservableObject {

    @Published private(set) var state: SmartTextState
    @Published var sheet: SmartTextSheetKind = .none
    @Published var selectionToolbarExpanded: Bool = false
    @Published private(set) var keyboardDismissToken: Int = 0

    func requestKeyboardDismiss() {
        keyboardDismissToken &+= 1
    }

    private var lastEmitted: RichDocument
    private let onDocumentChange: (RichDocument) -> Void

    init(document: RichDocument, onDocumentChange: @escaping (RichDocument) -> Void) {
        self.state = SmartTextState.companion.of(document: document)
        self.lastEmitted = document
        self.onDocumentChange = onDocumentChange
    }

    var document: RichDocument { state.document }

    var toolbar: ToolbarState { state.toolbar }

    var focusedBlockId: String { state.focusedBlockId }

    func dispatch(_ intent: SmartTextIntent) {
        let next = SmartTextReducer.shared.reduce(state: state, intent: intent)
        if next.document != state.document {
            lastEmitted = next.document
            onDocumentChange(next.document)
        }
        state = next
    }

    // only a document we did not produce replaces the editor state
    func syncExternal(document: RichDocument) {
        guard document != lastEmitted else { return }
        lastEmitted = document
        state = SmartTextState.companion.of(document: document)
    }

    func listNumber(for blockId: String) -> Int? {
        guard let value = ListEngine.shared.numbering(blocks: state.document.blocks)[blockId] else {
            return nil
        }
        return Int(truncating: value)
    }

    func searchRanges(for blockId: String) -> [NSRange] {
        guard state.search.isActive else { return [] }
        return state.search.matches
            .filter { $0.blockId == blockId }
            .map { NSRange(location: Int($0.start), length: Int($0.end - $0.start)) }
    }

    func activeSearchRange(for blockId: String) -> NSRange? {
        guard let match = state.search.current, match.blockId == blockId else { return nil }
        return NSRange(location: Int(match.start), length: Int(match.end - match.start))
    }

    var focusedTable: RichBlock.Table? {
        if let table = state.document.blockById(blockId: focusedBlockId) as? RichBlock.Table {
            return table
        }
        return state.document.blocks.compactMap { $0 as? RichBlock.Table }.last
    }

    // toolbar handling comes straight from shared, so iOS never spells an enum entry
    func handle(_ action: ToolbarAction, openURL: (URL) -> Void) {
        let commands = SmartTextCommands.shared
        if commands.isDismiss(action: action) {
            dispatch(commands.dismissToolbar())
            selectionToolbarExpanded = false
            requestKeyboardDismiss()
            return
        }
        if commands.isMore(action: action) {
            selectionToolbarExpanded.toggle()
            dispatch(commands.setToolbarExpanded(expanded: !state.isToolbarExpanded))
            return
        }
        if let intent = commands.forToolbar(action: action) {
            dispatch(intent)
            return
        }
        let index = Int(commands.sheetIndex(action: action))
        guard index != Int(SmartTextCommands.shared.SHEET_NONE) else { return }
        if commands.isLink(action: action),
           let existing = state.toolbar.link,
           state.selection.isCollapsed,
           let url = URL(string: existing) {
            openURL(url)
            return
        }
        let opened = SmartTextSheetKind(index: index)
        // the colour picker is tall — drop the keyboard so the whole sheet is reachable
        if opened == .textColor || opened == .backgroundColor {
            requestKeyboardDismiss()
        }
        sheet = opened
    }
}

enum SmartTextSheetKind: Identifiable, Equatable {
    case none
    case textStyle
    case textColor
    case backgroundColor
    case fontSize
    case align
    case table
    case link
    case findReplace

    // ordinals come from SmartTextCommands.SHEET_* so both platforms open the same sheet
    init(index: Int) {
        switch index {
        case 1: self = .textStyle
        case 2: self = .textColor
        case 3: self = .backgroundColor
        case 4: self = .fontSize
        case 5: self = .align
        case 6: self = .table
        case 7: self = .link
        case 8: self = .findReplace
        default: self = .none
        }
    }

    var id: Int {
        switch self {
        case .none: return 0
        case .textStyle: return 1
        case .textColor: return 2
        case .backgroundColor: return 3
        case .fontSize: return 4
        case .align: return 5
        case .table: return 6
        case .link: return 7
        case .findReplace: return 8
        }
    }

    var isPresented: Bool { id != 0 }
}
