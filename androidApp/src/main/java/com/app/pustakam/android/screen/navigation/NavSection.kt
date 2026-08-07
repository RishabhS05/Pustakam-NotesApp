package com.app.pustakam.android.screen.navigation

import androidx.navigation.NavDestination

/** Which nav graph a destination belongs to, and therefore which Scaffold wraps it. */
enum class NavSection {
    AUTH,
    HOME,
    EDITOR
}

/**
 * Walks up the graph hierarchy to find the owning section.
 *
 * 🔧 07-Aug-2026 — reads the ancestry rather than matching individual routes, so adding a
 * destination to a graph automatically gives it that graph's chrome with nothing else to update.
 */
fun NavDestination?.navSection(): NavSection {
    var node: NavDestination? = this
    while (node != null) {
        when (node.route) {
            Route.Authentication -> return NavSection.AUTH
            Route.Home -> return NavSection.HOME
            Route.NotesEditorRouter -> return NavSection.EDITOR
        }
        node = node.parent
    }
    return NavSection.HOME
}
