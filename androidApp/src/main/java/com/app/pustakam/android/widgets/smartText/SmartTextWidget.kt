package com.app.pustakam.android.widgets.smartText

import android.content.ClipData
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.app.pustakam.core.model.models.RichTextMetadata
import com.app.pustakam.core.richtext.codec.RichTextCodec
import com.app.pustakam.core.richtext.engine.ListEngine
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.presentation.SmartTextCommands
import com.app.pustakam.core.richtext.presentation.SmartTextIntent
import com.app.pustakam.core.richtext.presentation.SmartTextReducer
import com.app.pustakam.core.richtext.presentation.SmartTextState
import com.app.pustakam.core.richtext.presentation.ToolbarAction
import java.util.UUID

/**
 * Rich text editor for a note's TextContent. All behaviour comes from the shared
 * [SmartTextReducer]; this file only renders state and forwards intents.
 */
@Composable
fun SmartTextWidget(
    document: RichDocument,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    readOnly: Boolean = false,
    showKeyboardToolbar: Boolean = true,
    onDocumentChange: (RichDocument) -> Unit
) {
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current

    var state by remember { mutableStateOf(SmartTextState.of(document)) }
    var lastEmitted by remember { mutableStateOf(document) }
    var sheet by rememberSaveable { mutableStateOf(SmartTextSheet.NONE) }
    var selectionToolbarExpanded by remember { mutableStateOf(false) }

    // only a document we did NOT produce replaces the editor state — otherwise typing would reset it
    LaunchedEffect(document) {
        if (document != lastEmitted) {
            state = SmartTextState.of(document)
            lastEmitted = document
        }
    }

    val firstBlockId = state.document.blocks.first().id
    val extraRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(blockId: String): FocusRequester =
        if (blockId == firstBlockId) focusRequester
        else extraRequesters.getOrPut(blockId) { FocusRequester() }

    fun dispatch(intent: SmartTextIntent) {
        val next = SmartTextReducer.reduce(state, intent)
        if (next.document != state.document) {
            lastEmitted = next.document
            onDocumentChange(next.document)
        }
        state = next
    }

    LaunchedEffect(state.focusedBlockId) {
        runCatching { requesterFor(state.focusedBlockId).requestFocus() }
    }

    val numbering = remember(state.document) { ListEngine.numbering(state.document.blocks) }
    val focusedTable = state.document.blockById(state.focusedBlockId) as? RichBlock.Table
        ?: state.document.blocks.filterIsInstance<RichBlock.Table>().lastOrNull()

    // the action-to-intent mapping is shared, so Compose and SwiftUI cannot drift apart
    fun handle(action: ToolbarAction) {
        if (SmartTextCommands.isDismiss(action)) {
            dispatch(SmartTextCommands.dismissToolbar())
            selectionToolbarExpanded = false
            focusManager.clearFocus()
            return
        }
        if (SmartTextCommands.isMore(action)) {
            selectionToolbarExpanded = !selectionToolbarExpanded
            dispatch(SmartTextCommands.setToolbarExpanded(!state.isToolbarExpanded))
            return
        }
        SmartTextCommands.forToolbar(action)?.let { intent ->
            dispatch(intent)
            return
        }
        val existing = state.toolbar.link
        if (SmartTextCommands.isLink(action) && existing != null && state.selection.isCollapsed) {
            runCatching { uriHandler.openUri(existing) }
            return
        }
        val opened = SmartTextSheet.fromIndex(SmartTextCommands.sheetIndex(action))
        // the colour picker is tall — drop the keyboard so the whole sheet is reachable
        if (opened == SmartTextSheet.TEXT_COLOR || opened == SmartTextSheet.BACKGROUND_COLOR) {
            focusManager.clearFocus()
        }
        sheet = opened
    }

    val toolbarHost = LocalSmartTextToolbar.current
    val widgetId = rememberSaveable { UUID.randomUUID().toString() }
    var isEditorFocused by remember { mutableStateOf(false) }

    // the focused widget owns the single bar pinned above the keyboard
    LaunchedEffect(isEditorFocused, state.toolbar, state.isToolbarExpanded, state.canUndo, state.canRedo) {
        if (toolbarHost == null) return@LaunchedEffect
        if (isEditorFocused && !readOnly && showKeyboardToolbar) {
            toolbarHost.publish(
                ownerId = widgetId,
                toolbar = state.toolbar,
                expanded = state.isToolbarExpanded,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                onAction = ::handle
            )
        } else {
            toolbarHost.release(widgetId)
        }
    }
    DisposableEffect(widgetId) {
        onDispose { toolbarHost?.release(widgetId) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isEditorFocused = it.hasFocus }
            ) {
                state.document.blocks.forEach { block ->
                    SmartTextBlockRow(
                        block = block,
                        isFocused = block.id == state.focusedBlockId,
                        listNumber = numbering[block.id],
                        selection = state.selection,
                        searchRanges = searchRangesFor(state, block.id),
                        activeSearchRange = activeSearchRangeFor(state, block.id),
                        focusRequester = requesterFor(block.id),
                        readOnly = readOnly,
                        onIntent = ::dispatch,
                        modifier = Modifier.padding(bottom = SmartTextTokens.blockSpacing)
                    )
                }
            }

            if (state.showSelectionToolbar && !readOnly) {
                SmartTextSelectionToolbar(
                    toolbar = state.toolbar,
                    expanded = selectionToolbarExpanded,
                    onAction = ::handle,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )
            }
        }

        // inline fallback only when no screen-level host is provided (previews, standalone use)
        if (showKeyboardToolbar && !readOnly && toolbarHost == null && isEditorFocused) {
            SmartTextKeyboardToolbar(
                toolbar = state.toolbar,
                expanded = state.isToolbarExpanded,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                onAction = ::handle
            )
        }
    }

    SmartTextSheetHost(
        sheet = sheet,
        currentStyle = state.toolbar.paragraphStyle,
        currentAlign = state.toolbar.align,
        currentFontSize = state.toolbar.fontSize,
        currentLink = state.toolbar.link,
        searchQuery = state.search.query,
        replacement = state.search.replacement,
        matchCount = state.search.matches.size,
        currentMatch = state.search.currentIndex,
        tableRowCount = focusedTable?.data?.rowCount ?: 0,
        tableColumnCount = focusedTable?.data?.columnCount ?: 0,
        onDismiss = { sheet = SmartTextSheet.NONE },
        onStyle = {
            dispatch(SmartTextIntent.SetParagraphStyle(it))
            sheet = SmartTextSheet.NONE
        },
        onAlign = {
            dispatch(SmartTextIntent.SetAlignment(it))
            sheet = SmartTextSheet.NONE
        },
        onColor = {
            if (sheet == SmartTextSheet.BACKGROUND_COLOR) {
                dispatch(SmartTextIntent.SetBackgroundColor(it))
            } else {
                dispatch(SmartTextIntent.SetTextColor(it))
            }
            sheet = SmartTextSheet.NONE
        },
        onFontSize = {
            dispatch(SmartTextIntent.SetFontSize(it))
            sheet = SmartTextSheet.NONE
        },
        onLink = {
            dispatch(if (it == null) SmartTextIntent.RemoveLink else SmartTextIntent.SetLink(it))
            sheet = SmartTextSheet.NONE
        },
        onTable = { command ->
            focusedTable?.let { dispatch(SmartTextIntent.TableAction(it.id, command)) }
        },
        onInsertTable = { rows, columns ->
            dispatch(SmartTextIntent.InsertTable(rows, columns))
            sheet = SmartTextSheet.NONE
        },
        onSearchQuery = { dispatch(SmartTextIntent.SetSearchQuery(it)) },
        onReplacement = { dispatch(SmartTextIntent.SetReplacement(it)) },
        onFindNext = { dispatch(SmartTextIntent.FindNext) },
        onFindPrevious = { dispatch(SmartTextIntent.FindPrevious) },
        onReplaceCurrent = { dispatch(SmartTextIntent.ReplaceCurrent) },
        onReplaceAll = { dispatch(SmartTextIntent.ReplaceAll) }
    )

    // mirrors Cut/Copy into the system clipboard without leaking Compose types into shared code
    LaunchedEffect(state.clipboard) {
        state.clipboard?.let { clipboard.setText(AnnotatedString(it.text)) }
    }
}

/** Convenience wrapper for callers that still hold plain text plus its stored metadata. */
@Composable
fun SmartTextWidget(
    text: String,
    metadata: RichTextMetadata?,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    readOnly: Boolean = false,
    showKeyboardToolbar: Boolean = true,
    onDocumentChange: (RichDocument) -> Unit
) {
    val document = remember(text, metadata) { RichTextCodec.documentFrom(text, metadata) }
    SmartTextWidget(
        document = document,
        modifier = modifier,
        focusRequester = focusRequester,
        readOnly = readOnly,
        showKeyboardToolbar = showKeyboardToolbar,
        onDocumentChange = onDocumentChange
    )
}

private fun searchRangesFor(state: SmartTextState, blockId: String): List<IntRange> =
    if (!state.search.isActive) emptyList()
    else state.search.matches.filter { it.blockId == blockId }.map { it.start until it.end }

private fun activeSearchRangeFor(state: SmartTextState, blockId: String): IntRange? =
    state.search.current?.takeIf { it.blockId == blockId }?.let { it.start until it.end }
suspend fun Clipboard.setText(annotatedString: AnnotatedString) {
    val clipData = ClipData.newPlainText("text", annotatedString.text)
    this.setClipEntry(clipData.toClipEntry())
}