package com.cortex.app.di

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cortex.app.ui.screens.capture.CaptureViewModel
import com.cortex.app.ui.screens.graph.GraphViewModel
import com.cortex.app.ui.screens.home.HomeViewModel
import com.cortex.app.ui.screens.search.SearchViewModel

/** One factory, wired from [AppContainer]. Add an `initializer { ... }` per new ViewModel. */
fun cortexViewModelFactory(container: AppContainer) = viewModelFactory {
    initializer { HomeViewModel(container.memoryRepository) }
    initializer { CaptureViewModel(container.memoryLinkingPipeline) }
    initializer { SearchViewModel(container.searchEngine) }
    initializer { GraphViewModel(container.graphRepository, container.graphQueryEngine, container.graphStatistics) }
}
