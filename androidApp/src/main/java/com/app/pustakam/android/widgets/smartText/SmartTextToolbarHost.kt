package com.app.pustakam.android.widgets.smartText

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.pustakam.core.richtext.presentation.ToolbarAction
import com.app.pustakam.core.richtext.presentation.ToolbarState

/**
 * A note holds several text contents, each with its own [SmartTextWidget]. The keyboard toolbar
 * must be a single bar sitting on the keyboard, so the focused widget publishes its state here
 * and the screen renders one [SmartTextKeyboardToolbar] from it.
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
 * Renders the keyboard toolbar with its bottom edge on top of the keyboard.
 *
 * 🔧 07-Aug-2026 — this places the bar at the window bottom and *translates* it up by the raw
 * inset instead of padding it. windowInsetsPadding was resolving to zero because an ancestor
 * (Scaffold) had already consumed the insets, which is what left the bar floating over content.
 * A layout-time translation reads the inset directly and cannot be consumed away.
 *
 * Put it as the last child of a full-size Box so it floats over everything.
 */
@Composable
fun SmartTextKeyboardToolbarHost(
    controller: SmartTextToolbarController,
    modifier: Modifier = Modifier
) {
    if (!controller.isVisible) return

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navigationBottom = WindowInsets.navigationBars.getBottom(density)
    val lift = maxOf(imeBottom, navigationBottom)

    Box(modifier = modifier.fillMaxWidth()) {
        SmartTextKeyboardToolbar(
            toolbar = controller.toolbar,
            expanded = controller.expanded,
            canUndo = controller.canUndo,
            canRedo = controller.canRedo,
            onAction = controller::dispatch,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, -lift)
                    }
                }
        )
    }
}

/** Space the scrolling content must leave so the last line is not hidden by the bar. */
val SmartTextToolbarReservedHeight: Dp = 56.dp
