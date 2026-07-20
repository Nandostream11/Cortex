package com.cortex.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guidance_events")
data class GuidanceEventEntity(
    @PrimaryKey val id: String,
    val type: String,
    val content: String,
    val createdAt: Long,
    val sourceMemoryIdsJson: String
)
