package com.cortex.app.domain.model

import java.time.Instant

data class Node(
    val id: String,
    val label: String,
    val type: NodeType,
    val canonicalName: String,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
