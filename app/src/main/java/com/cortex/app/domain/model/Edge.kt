package com.cortex.app.domain.model

import java.time.Instant

data class Edge(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val relationType: RelationType,
    val weight: Double,
    val confidence: Double,
    val createdAt: Instant
)
