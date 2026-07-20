package com.cortex.app.di

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cortex.app.ui.screens.capture.CaptureViewModel
import com.cortex.app.ui.screens.home.HomeViewModel

/** One factory, wired from [AppContainer]. Add an `initializer { ... }` per new ViewModel. */
fun cortexViewModelFactory(container: AppContainer) = viewModelFactory {
    initializer { HomeViewModel(container.memoryRepository) }
    initializer { CaptureViewModel(container.captureTextMemoryUseCase) }
}
