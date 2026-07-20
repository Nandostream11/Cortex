package com.cortex.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_items")
data class MemoryItemEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val rawText: String,
    val normalizedText: String,
    val sourceType: String,
    val sourceRef: String?,
    val category: String,
    val importanceScore: Double,
    val confidenceScore: Double,
    val isPinned: Boolean,
    val isArchived: Boolean,
    /** Comma-free JSON array string; see [com.cortex.app.data.db.Converters]. */
    val tagIdsJson: String,
    val linkedNodeIdsJson: String
)
