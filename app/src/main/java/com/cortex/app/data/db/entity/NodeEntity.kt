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
    val canonicalName: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long
)
