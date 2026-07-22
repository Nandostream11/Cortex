package com.cortex.app.domain.model

import java.time.Instant

data class Node(
    val id: String,
    val label: String,
    val type: NodeType,
    /**
     * Finer-grained classification within [type] (e.g. type=TOOL, subtype="FRAMEWORK"),
     * set from [com.cortex.app.domain.graph.EntityKind] during graph building. Null for
     * nodes that predate Phase 2 or don't need it. See ADR-0002 for why this exists
     * instead of expanding NodeType itself.
     */
    val subtype: String?,
    val canonicalName: String,
    val description: String?,
    /** Composite importance from [com.cortex.app.domain.graph.GraphRanking], refreshed by GraphUpdater. 0.0 until first computed. */
    val importanceScore: Double = 0.0,
    val createdAt: Instant,
    val updatedAt: Instant
)
