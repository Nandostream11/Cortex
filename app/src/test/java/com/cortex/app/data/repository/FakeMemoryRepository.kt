package com.cortex.app.data.repository

import com.cortex.app.domain.model.MemoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeMemoryRepository : MemoryRepository {
    private val items = MutableStateFlow<List<MemoryItem>>(emptyList())

    /** Test-only inspection helper. */
    val saved: List<MemoryItem> get() = items.value

    override fun observeActive(): StateFlow<List<MemoryItem>> = items

    // Snapshot-at-call-time, not a live-updating stream — fine for current tests, which
    // only assert against `saved` after calling use cases. Revisit if a test needs to
    // observe these reactively.
    override fun observePinned() = MutableStateFlow(items.value.filter { it.isPinned })
    override fun observeActiveCount() = MutableStateFlow(items.value.count { !it.isArchived })

    override suspend fun getById(id: String): MemoryItem? = items.value.find { it.id == id }

    override suspend fun save(item: MemoryItem) {
        items.value = items.value.filterNot { it.id == item.id } + item
    }

    override suspend fun searchByKeyword(query: String, limit: Int): List<MemoryItem> =
        items.value.filter { it.normalizedText.contains(query, ignoreCase = true) }.take(limit)

    override suspend fun getRecent(limit: Int): List<MemoryItem> =
        items.value.sortedByDescending { it.createdAt }.take(limit)

    override suspend fun archive(id: String) {
        items.value = items.value.map { if (it.id == id) it.copy(isArchived = true) else it }
    }

    override suspend fun setPinned(id: String, pinned: Boolean) {
        items.value = items.value.map { if (it.id == id) it.copy(isPinned = pinned) else it }
    }
}
