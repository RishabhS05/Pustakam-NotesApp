package com.app.pustakam.android.widgets.dynamicWidgets

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

class CustomTextToolbar(
    private val onShowMenu: (Rect) -> Unit,
    private val onHideMenu: () -> Unit
) : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Shown

    override fun showMenu(
        rect: Rect, // This rect contains the selection coordinates
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        onShowMenu(rect)
    }

    override fun hide() {
        onHideMenu()
    }
}