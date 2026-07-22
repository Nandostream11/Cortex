package com.cortex.app.data.graph

import com.cortex.app.data.db.dao.EdgeDao
import com.cortex.app.data.db.dao.NodeDao
import com.cortex.app.data.db.entity.EdgeEntity
import com.cortex.app.data.db.entity.NodeEntity
import com.cortex.app.data.mapper.toDomain
import com.cortex.app.domain.graph.ExistingNodeRef
import com.cortex.app.domain.graph.NodeDeduplicator
import com.cortex.app.domain.model.Edge
import com.cortex.app.domain.model.Node
import com.cortex.app.domain.model.NodeType
import com.cortex.app.domain.model.RelationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class GraphRepositoryImpl(
    private val nodeDao: NodeDao,
    private val edgeDao: EdgeDao,
    private val deduplicator: NodeDeduplicator = NodeDeduplicator()
) : GraphRepository {

    override suspend fun findOrCreateNode(
        rawName: String,
        type: NodeType,
        subtype: String?,
        description: String?
    ): Node {
        // Cheap path first: exact canonical-name+type match, same as Phase 1's
        // NodeDao.findByCanonicalName. Only falls through to the fuzzy path (loading
        // every same-type node for similarity scoring) when that misses.
        val cheapCanonical = rawName.trim().lowercase()
        nodeDao.findByCanonicalName(cheapCanonical, type.name)?.let { return it.toDomain() }

        val sameTypeExisting = nodeDao.getAllForType(type.name)

        val existingRefs = sameTypeExisting.map { ExistingNodeRef(it.id, type, it.canonicalName) }
        val result = deduplicator.resolve(rawName, type, existingRefs)

        if (result.matchedNodeId != null) {
            val existing = nodeDao.getById(result.matchedNodeId)
            if (existing != null) return existing.toDomain()
        }

        val now = Instant.now().toEpochMilli()
        val entity = NodeEntity(
            id = UUID.randomUUID().toString(),
            label = rawName.trim(),
            type = type.name,
            subtype = subtype,
            canonicalName = result.canonicalName,
            description = description,
            importanceScore = 0.0,
            createdAt = now,
            updatedAt = now
        )
        nodeDao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun findOrCreateMemoryNode(memoryId: String, previewLabel: String): Node {
        nodeDao.findByCanonicalName(memoryId, NodeType.MEMORY.name)?.let { return it.toDomain() }

        val now = Instant.now().toEpochMilli()
        val entity = NodeEntity(
            id = UUID.randomUUID().toString(),
            label = previewLabel.take(80),
            type = NodeType.MEMORY.name,
            subtype = null,
            canonicalName = memoryId,
            description = null,
            importanceScore = 0.0,
            createdAt = now,
            updatedAt = now
        )
        nodeDao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun findNodeByExactName(canonicalName: String, type: NodeType): Node? =
        nodeDao.findByCanonicalName(canonicalName, type.name)?.toDomain()

    override suspend fun getNode(id: String): Node? = nodeDao.getById(id)?.toDomain()

    override suspend fun getAllNodes(): List<Node> = nodeDao.getAll().map { it.toDomain() }

    override fun observeAllNodes(): Flow<List<Node>> = nodeDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun createOrReinforceEdge(
        fromNodeId: String,
        toNodeId: String,
        relationType: RelationType,
        initialWeight: Double,
        confidence: Double
    ): Edge {
        val existing = edgeDao.findExisting(fromNodeId, toNodeId, relationType.name)
        if (existing != null) {
            // Reinforcement amount scales with confidence: a high-confidence repeat
            // observation should move the weight more than a shaky one.
            edgeDao.reinforce(existing.id, increment = 0.15 * confidence)
            return (edgeDao.getById(existing.id) ?: existing).toDomain()
        }

        val entity = EdgeEntity(
            id = UUID.randomUUID().toString(),
            fromNodeId = fromNodeId,
            toNodeId = toNodeId,
            relationType = relationType.name,
            weight = initialWeight,
            confidence = confidence,
            createdAt = Instant.now().toEpochMilli()
        )
        edgeDao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun getAllEdges(): List<Edge> = edgeDao.getAllOrderedByWeight().map { it.toDomain() }

    override suspend fun edgesTouching(nodeId: String): List<Edge> = edgeDao.edgesTouching(nodeId).map { it.toDomain() }

    override suspend fun updateNodeImportance(nodeId: String, score: Double) {
        nodeDao.updateImportanceScore(nodeId, score)
    }
}
