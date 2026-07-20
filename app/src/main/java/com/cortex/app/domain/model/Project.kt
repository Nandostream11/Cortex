package com.cortex.app.domain.model

import java.time.Instant

data class Project(
    val id: String,
    val name: String,
    val description: String?,
    val status: ProjectStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
