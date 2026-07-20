package com.cortex.app.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cortex.app.domain.usecase.CaptureTextMemoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CaptureUiState(
    val inputText: String = "",
    val isSaving: Boolean = false,
    val lastSavedConfirmation: String? = null,
    val error: String? = null
)

class CaptureViewModel(private val captureTextMemory: CaptureTextMemoryUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun onSaveClicked() {
        val text = _uiState.value.inputText
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "Nothing to capture yet.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runCatching { captureTextMemory(text) }
                .onSuccess { saved ->
                    _uiState.update {
                        CaptureUiState(
                            inputText = "",
                            isSaving = false,
                            lastSavedConfirmation = "Saved as ${saved.category.name.lowercase().replace('_', ' ')}"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSaving = false, error = throwable.message ?: "Couldn't save that.")
                    }
                }
        }
    }

    fun dismissConfirmation() {
        _uiState.update { it.copy(lastSavedConfirmation = null) }
    }
}
