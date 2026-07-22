package com.cortex.app.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cortex.app.domain.usecase.MemoryLinkingPipeline
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

/**
 * Drives capture through [MemoryLinkingPipeline] (Phase 2) rather than the bare
 * CaptureTextMemoryUseCase (Phase 1) — every capture now also extracts entities and
 * links them into the graph, not just saves text to Room.
 */
class CaptureViewModel(private val memoryLinkingPipeline: MemoryLinkingPipeline) : ViewModel() {

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
            runCatching { memoryLinkingPipeline(text) }
                .onSuccess { result ->
                    val entityCount = result.graphLink.entityNodes.size
                    val category = result.memory.category.name.lowercase().replace('_', ' ')
                    val confirmation = if (entityCount > 0) {
                        "Saved as $category — linked $entityCount entit${if (entityCount == 1) "y" else "ies"}"
                    } else {
                        "Saved as $category"
                    }
                    _uiState.update {
                        CaptureUiState(inputText = "", isSaving = false, lastSavedConfirmation = confirmation)
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
