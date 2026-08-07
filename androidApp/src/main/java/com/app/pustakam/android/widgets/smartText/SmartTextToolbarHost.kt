package com.app.pustakam.android.widgets.smartText

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.pustakam.core.richtext.presentation.ToolbarAction
import com.app.pustakam.core.richtext.presentation.ToolbarState

/**
 * A note holds several text contents, each with its own [SmartTextWidget]. The keyboard toolbar
 * must be a single bar pinned above the IME, so the focused widget publishes its state here and
 * the screen renders one [SmartTextKeyboardToolbar] from it.
 */
@Stable
class SmartTextToolbarController {

    var toolbar by mutableStateOf(ToolbarState())
        private set

    var expanded by mutableStateOf(false)
        private set

    var canUndo by mutableStateOf(false)
        private set

    var canRedo by mutableStateOf(false)
        private set

    var isVisible by mutableStateOf(false)
        private set

    private var owner: String? = null
    private var action: ((ToolbarAction) -> Unit)? = null

    fun publish(
        ownerId: String,
        toolbar: ToolbarState,
        expanded: Boolean,
        canUndo: Boolean,
        canRedo: Boolean,
        onAction: (ToolbarAction) -> Unit
    ) {
        owner = ownerId
        this.toolbar = toolbar
        this.expanded = expanded
        this.canUndo = canUndo
        this.canRedo = canRedo
        this.action = onAction
        isVisible = true
    }

    /** Ignored when another widget already took focus, so blur/focus races cannot hide the bar. */
    fun release(ownerId: String) {
        if (owner != ownerId) return
        owner = null
        action = null
        isVisible = false
    }

    fun dispatch(toolbarAction: ToolbarAction) {
        action?.invoke(toolbarAction)
    }
}

val LocalSmartTextToolbar = compositionLocalOf<SmartTextToolbarController?> { null }

@Composable
fun rememberSmartTextToolbarController(): SmartTextToolbarController =
    remember { SmartTextToolbarController() }

/**
 * Renders the keyboard toolbar pinned above the IME. Place it as the last child of a
 * full-size Box so it floats over the content instead of scrolling with it.
 */
@Composable
fun SmartTextKeyboardToolbarHost(
    controller: SmartTextToolbarController,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = controller.isVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SmartTextKeyboardToolbar(
                toolbar = controller.toolbar,
                expanded = controller.expanded,
                canUndo = controller.canUndo,
                canRedo = controller.canRedo,
                onAction = controller::dispatch,
                // union, not both: the IME inset already spans the navigation bar when it is up
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.ime.union(WindowInsets.navigationBars)
                )
            )
        }
    }
}

/** Space the scrolling content must leave so the last line is not hidden by the bar. */
val SmartTextToolbarReservedHeight: Dp = 56.dp
