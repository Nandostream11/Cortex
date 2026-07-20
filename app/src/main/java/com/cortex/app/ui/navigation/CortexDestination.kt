package com.cortex.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The seven screens from Architecture.md's UI layer. Only Home and Capture are fully
 * wired in Phase 1; the rest render as placeholders via [PlaceholderScreen] until their
 * owning subsystem (graph engine, guidance engine, connector SDK) exists.
 */
sealed class CortexDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Home : CortexDestination("home", "Home", Icons.Filled.Home)
    data object Search : CortexDestination("search", "Search", Icons.Filled.Search)
    data object Graph : CortexDestination("graph", "Graph", Icons.Filled.AccountTree)
    data object Guidance : CortexDestination("guidance", "Guidance", Icons.Filled.Lightbulb)
    data object Connectors : CortexDestination("connectors", "Connectors", Icons.Filled.Cable)
    data object Settings : CortexDestination("settings", "Settings", Icons.Filled.Settings)

    companion object {
        // Capture is reached from Home (a prominent action), not a bottom-nav tab —
        // it's the primary action of the app, not a peer destination to browse to.
        const val CAPTURE_ROUTE = "capture"

        val bottomBarDestinations = listOf(Home, Search, Graph, Guidance, Connectors, Settings)
    }
}
