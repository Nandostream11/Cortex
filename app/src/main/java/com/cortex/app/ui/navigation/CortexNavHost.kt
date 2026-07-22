package com.cortex.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cortex.app.di.AppContainer
import com.cortex.app.di.cortexViewModelFactory
import com.cortex.app.ui.screens.capture.CaptureScreen
import com.cortex.app.ui.screens.capture.CaptureViewModel
import com.cortex.app.ui.screens.graph.GraphScreen
import com.cortex.app.ui.screens.home.HomeScreen
import com.cortex.app.ui.screens.placeholder.PlaceholderScreen
import com.cortex.app.ui.screens.search.SearchScreen

@Composable
fun CortexApp(container: AppContainer) {
    val navController = rememberNavController()
    val factory = cortexViewModelFactory(container)

    Scaffold(
        bottomBar = { CortexBottomBar(navController) },
        floatingActionButton = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            if (backStackEntry?.destination?.route == CortexDestination.Home.route) {
                FloatingActionButton(onClick = { navController.navigate(CortexDestination.CAPTURE_ROUTE) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Capture")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CortexDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(CortexDestination.Home.route) {
                HomeScreen(viewModel(factory = factory))
            }
            composable(CortexDestination.CAPTURE_ROUTE) {
                val captureViewModel: CaptureViewModel = viewModel(factory = factory)
                CaptureScreen(captureViewModel)
            }
            composable(CortexDestination.Search.route) {
                SearchScreen(viewModel(factory = factory))
            }
            composable(CortexDestination.Graph.route) {
                // TODO(Phase 3): once a memory-detail screen exists, navigate to it here
                // instead of doing nothing — tracked in docs/PHASE2_STATUS.md rather than
                // half-building a detail screen just to wire this one callback.
                GraphScreen(viewModel(factory = factory), onOpenRelatedMemories = {})
            }
            composable(CortexDestination.Guidance.route) {
                PlaceholderScreen("Guidance", "Proactive suggestions and daily briefs land with GuidanceEngine.")
            }
            composable(CortexDestination.Connectors.route) {
                PlaceholderScreen("Connectors", "Connect Gmail, Calendar, GitHub and more here once the Connector SDK exists.")
            }
            composable(CortexDestination.Settings.route) {
                PlaceholderScreen("Settings", "Autonomy mode, OpenRouter key, and export/import will live here.")
            }
        }
    }
}

@Composable
private fun CortexBottomBar(navController: androidx.navigation.NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        CortexDestination.bottomBarDestinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}
