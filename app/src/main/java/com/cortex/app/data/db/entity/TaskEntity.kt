package com.cortex.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = MemoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedMemoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("projectId"), Index("linkedMemoryId"), Index("status")]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val details: String?,
    val status: String,
    val priority: String,
    val dueAt: Long?,
    val projectId: String?,
    val linkedMemoryId: String?,
    val createdAt: Long,
    val updatedAt: Long
)
