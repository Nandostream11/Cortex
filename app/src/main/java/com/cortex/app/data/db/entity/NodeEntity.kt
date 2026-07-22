package com.cortex.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nodes",
    indices = [Index(value = ["canonicalName", "type"])]
)
data class NodeEntity(
    @PrimaryKey val id: String,
    val label: String,
    val type: String,
    /** See [com.cortex.app.domain.model.Node.subtype]. Nullable — added in schema v2. */
    val subtype: String?,
    val canonicalName: String,
    val description: String?,
    val importanceScore: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long
)
