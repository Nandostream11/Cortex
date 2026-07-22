package com.cortex.app.data.graph

import com.cortex.app.domain.model.Edge
import com.cortex.app.domain.model.Node
import com.cortex.app.domain.model.NodeType
import com.cortex.app.domain.model.RelationType
import kotlinx.coroutines.flow.Flow

interface GraphRepository {

    /**
     * Finds an existing node to merge into (via [com.cortex.app.domain.graph.NodeDeduplicator])
     * or creates a new one. Always returns a persisted node either way.
     */
    suspend fun findOrCreateNode(
        rawName: String,
        type: NodeType,
        subtype: String?,
        description: String? = null
    ): Node

    /**
     * Memory nodes are exact-by-id, never fuzzy-deduplicated — unlike [findOrCreateNode],
     * this must never merge two different memories just because their preview text looks
     * similar. [memoryId] is used as the canonical name directly.
     */
    suspend fun findOrCreateMemoryNode(memoryId: String, previewLabel: String): Node

    /** Exact canonical-name + type lookup, no fuzzy matching — the read-only counterpart to findOrCreate. */
    suspend fun findNodeByExactName(canonicalName: String, type: NodeType): Node?

    suspend fun getNode(id: String): Node?
    suspend fun getAllNodes(): List<Node>
    fun observeAllNodes(): Flow<List<Node>>

    /**
     * Creates a new edge, or — if one already exists between the same two nodes with
     * the same [relationType] — reinforces it (GraphEngine.md: repeated relations
     * strengthen the existing edge rather than duplicating it).
     */
    suspend fun createOrReinforceEdge(
        fromNodeId: String,
        toNodeId: String,
        relationType: RelationType,
        initialWeight: Double,
        confidence: Double
    ): Edge

    suspend fun getAllEdges(): List<Edge>
    suspend fun edgesTouching(nodeId: String): List<Edge>
    suspend fun updateNodeImportance(nodeId: String, score: Double)
}
