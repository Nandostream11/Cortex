package com.cortex.app.domain.graph

import com.cortex.app.domain.model.RelationType

/**
 * A candidate edge between two extracted entities, before either has been resolved to
 * an actual persisted [com.cortex.app.domain.model.Node]. [GraphBuilder] resolves
 * `fromKind`/`fromValue` and `toKind`/`toValue` to real node ids (creating or
 * deduplicating as needed) and persists the edge.
 */
data class InferredRelation(
    val fromKind: EntityKind,
    val fromValue: String,
    val toKind: EntityKind,
    val toValue: String,
    val relationType: RelationType,
    val confidence: Double,
    val rationale: String
)
