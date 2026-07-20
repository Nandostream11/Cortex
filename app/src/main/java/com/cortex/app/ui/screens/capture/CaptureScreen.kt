package com.cortex.app.ui.screens.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CaptureScreen(viewModel: CaptureViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Capture", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = state.inputText,
            onValueChange = viewModel::onInputChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("What's on your mind?") },
            minLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            isError = state.error != null
        )

        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        state.lastSavedConfirmation?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Push-to-talk is in IssueBacklog's "Capture" section, but the transcription
            // pipeline doesn't exist yet — disabled rather than faked.
            IconButton(onClick = { /* TODO: push-to-talk, needs a transcription pipeline */ }, enabled = false) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice capture (coming soon)")
            }

            Button(
                onClick = viewModel::onSaveClicked,
                enabled = !state.isSaving && state.inputText.isNotBlank(),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
        }
    }
}
