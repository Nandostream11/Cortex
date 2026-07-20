package com.cortex.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cortex.app.domain.model.MemoryItem
import com.cortex.app.ui.screens.placeholder.PlaceholderScreen
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val memories by viewModel.activeMemories.collectAsStateWithLifecycle()

    if (memories.isEmpty()) {
        PlaceholderScreen(
            title = "Nothing captured yet",
            subtitle = "Tap the capture button to turn your first thought into memory.",
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(memories, key = { it.id }) { memory ->
            MemoryCard(memory)
        }
    }
}

@Composable
private fun MemoryCard(memory: MemoryItem, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = memory.category.name.replace('_', ' '),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = memory.normalizedText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = formatTimestamp(memory),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")

private fun formatTimestamp(memory: MemoryItem): String =
    timeFormatter.format(memory.createdAt.atZone(ZoneId.systemDefault()))
