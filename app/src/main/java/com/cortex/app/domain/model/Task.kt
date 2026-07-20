package com.cortex.app.domain.model

import java.time.Instant

data class Task(
    val id: String,
    val title: String,
    val details: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueAt: Instant?,
    val projectId: String?,
    val linkedMemoryId: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
