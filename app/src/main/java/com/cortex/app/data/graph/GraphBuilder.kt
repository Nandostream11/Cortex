package com.cortex.app.data.graph

import com.cortex.app.domain.graph.EntityExtractor
import com.cortex.app.domain.graph.EntityKind
import com.cortex.app.domain.graph.EntityToNodeMapping
import com.cortex.app.domain.graph.RelationInferer
import com.cortex.app.domain.model.MemoryItem
import com.cortex.app.domain.model.Node
import com.cortex.app.domain.model.RelationType

data class GraphLinkResult(
    val memoryNode: Node,
    val entityNodes: List<Node>,
    val edgesCreated: Int
)

/**
 * MemoryEngine.md Step 7 / Phase 2 Step 7: normalize -> extract -> create nodes -> merge
 * duplicates -> create edges -> (scores are [GraphUpdater]'s job, run separately/batched
 * rather than per-capture — see its doc comment) -> persist. Normalization and
 * extraction happen before this is called ([memory] is already a saved MemoryItem);
 * this class owns everything from "entities are extracted" onward.
 */
class GraphBuilder(
    private val graphRepository: GraphRepository,
    private val entityExtractor: EntityExtractor = EntityExtractor(),
    private val relationInferer: RelationInferer = RelationInferer()
) {

    suspend fun linkMemory(memory: MemoryItem): GraphLinkResult {
        val memoryNode = graphRepository.findOrCreateMemoryNode(memory.id, memory.normalizedText)

        val allEntities = entityExtractor.extract(memory.normalizedText, memory.createdAt)
        val nodeEntities = allEntities.filter { EntityToNodeMapping.isPromotedToNode(it.kind) }

        // (kind, normalizedValue) -> resolved Node, so RelationInferer's output (which
        // speaks in terms of kind+value) can be turned into real edges between real ids
        // without re-running dedup for the same entity twice within one memory.
        val resolved = LinkedHashMap<Pair<EntityKind, String>, Node>()

        for (entity in nodeEntities) {
            val key = entity.kind to entity.normalizedValue
            val node = resolved.getOrPut(key) {
                // Safe: nodeEntities was already filtered to isPromotedToNode == true,
                // so nodeTypeFor is guaranteed non-null here by construction.
                val nodeType = EntityToNodeMapping.nodeTypeFor(entity.kind)!!
                graphRepository.findOrCreateNode(
                    rawName = entity.normalizedValue,
                    type = nodeType,
                    subtype = EntityToNodeMapping.subtypeFor(entity.kind)
                )
            }
            graphRepository.createOrReinforceEdge(
                fromNodeId = memoryNode.id,
                toNodeId = node.id,
                relationType = RelationType.MENTIONS,
                initialWeight = entity.confidence,
                confidence = entity.confidence
            )
        }

        val inferredRelations = relationInferer.infer(nodeEntities, memory.category)
        var relationEdgeCount = 0
        for (relation in inferredRelations) {
            val fromNode = resolved[relation.fromKind to relation.fromValue] ?: continue
            val toNode = resolved[relation.toKind to relation.toValue] ?: continue
            graphRepository.createOrReinforceEdge(
                fromNodeId = fromNode.id,
                toNodeId = toNode.id,
                relationType = relation.relationType,
                initialWeight = relation.confidence,
                confidence = relation.confidence
            )
            relationEdgeCount++
        }

        return GraphLinkResult(memoryNode, resolved.values.toList(), nodeEntities.size + relationEdgeCount)
    }
}
