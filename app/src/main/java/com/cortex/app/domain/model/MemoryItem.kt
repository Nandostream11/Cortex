package com.cortex.app.domain.model

import java.time.Instant

/**
 * A single structured unit of memory — the core object in Cortex.
 * Every capture becomes one of these, per Database.md's MemoryItem entity.
 */
data class MemoryItem(
    val id: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val rawText: String,
    val normalizedText: String,
    val sourceType: MemorySourceType,
    val sourceRef: String?,
    val category: MemoryCategory,
    val importanceScore: Double,
    val confidenceScore: Double,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val tagIds: List<String> = emptyList(),
    val linkedNodeIds: List<String> = emptyList()
)
