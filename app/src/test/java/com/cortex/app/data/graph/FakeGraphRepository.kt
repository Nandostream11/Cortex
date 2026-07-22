package com.cortex.app.data.graph

import com.cortex.app.domain.graph.ExistingNodeRef
import com.cortex.app.domain.graph.NodeDeduplicator
import com.cortex.app.domain.model.Edge
import com.cortex.app.domain.model.Node
import com.cortex.app.domain.model.NodeType
import com.cortex.app.domain.model.RelationType
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.util.UUID

/**
 * In-memory stand-in for [GraphRepositoryImpl], used by tests for [GraphBuilder],
 * [GraphQueryEngine], [GraphStatistics], and [GraphUpdater] so they don't need Room.
 * Mirrors the real implementation's dedup-on-write and reinforce-on-repeat behavior,
 * since those semantics are exactly what those tests need to exercise.
 */
class FakeGraphRepository(private val deduplicator: NodeDeduplicator = NodeDeduplicator()) : GraphRepository {

    private val nodesFlow = MutableStateFlow<List<Node>>(emptyList())
    private val edges = mutableListOf<Edge>()

    val allNodesSnapshot: List<Node> get() = nodesFlow.value
    val allEdgesSnapshot: List<Edge> get() = edges.toList()

    override suspend fun findOrCreateNode(rawName: String, type: NodeType, subtype: String?, description: String?): Node {
        val canonical = rawName.trim().lowercase()
        nodesFlow.value.firstOrNull { it.type == type && it.canonicalName == canonical }?.let { return it }

        val existingRefs = nodesFlow.value.filter { it.type == type }.map { ExistingNodeRef(it.id, it.type, it.canonicalName) }
        val result = deduplicator.resolve(rawName, type, existingRefs)
        if (result.matchedNodeId != null) {
            nodesFlow.value.firstOrNull { it.id == result.matchedNodeId }?.let { return it }
        }

        val now = Instant.now()
        val node = Node(
            id = UUID.randomUUID().toString(),
            label = rawName.trim(),
            type = type,
            subtype = subtype,
            canonicalName = result.canonicalName,
            description = description,
            importanceScore = 0.0,
            createdAt = now,
            updatedAt = now
        )
        nodesFlow.value = nodesFlow.value + node
        return node
    }

    override suspend fun findOrCreateMemoryNode(memoryId: String, previewLabel: String): Node {
        nodesFlow.value.firstOrNull { it.type == NodeType.MEMORY && it.canonicalName == memoryId }?.let { return it }
        val now = Instant.now()
        val node = Node(
            id = UUID.randomUUID().toString(),
            label = previewLabel.take(80),
            type = NodeType.MEMORY,
            subtype = null,
            canonicalName = memoryId,
            description = null,
            importanceScore = 0.0,
            createdAt = now,
            updatedAt = now
        )
        nodesFlow.value = nodesFlow.value + node
        return node
    }

    override suspend fun findNodeByExactName(canonicalName: String, type: NodeType): Node? =
        nodesFlow.value.firstOrNull { it.type == type && it.canonicalName == canonicalName }

    override suspend fun getNode(id: String): Node? = nodesFlow.value.firstOrNull { it.id == id }

    override suspend fun getAllNodes(): List<Node> = nodesFlow.value

    override fun observeAllNodes() = nodesFlow

    override suspend fun createOrReinforceEdge(
        fromNodeId: String,
        toNodeId: String,
        relationType: RelationType,
        initialWeight: Double,
        confidence: Double
    ): Edge {
        val existingIndex = edges.indexOfFirst {
            it.fromNodeId == fromNodeId && it.toNodeId == toNodeId && it.relationType == relationType
        }
        if (existingIndex >= 0) {
            val existing = edges[existingIndex]
            val reinforced = existing.copy(weight = existing.weight + 0.15 * confidence)
            edges[existingIndex] = reinforced
            return reinforced
        }
        val edge = Edge(
            id = UUID.randomUUID().toString(),
            fromNodeId = fromNodeId,
            toNodeId = toNodeId,
            relationType = relationType,
            weight = initialWeight,
            confidence = confidence,
            createdAt = Instant.now()
        )
        edges.add(edge)
        return edge
    }

    override suspend fun getAllEdges(): List<Edge> = edges.toList()

    override suspend fun edgesTouching(nodeId: String): List<Edge> =
        edges.filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }

    override suspend fun updateNodeImportance(nodeId: String, score: Double) {
        nodesFlow.value = nodesFlow.value.map { if (it.id == nodeId) it.copy(importanceScore = score) else it }
    }
}
