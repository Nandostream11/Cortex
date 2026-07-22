package com.cortex.app.data.graph

import com.cortex.app.domain.model.Edge
import com.cortex.app.domain.model.Node
import com.cortex.app.domain.model.NodeType

data class NeighborEdge(val neighbor: Node, val edge: Edge)

class GraphQueryEngine(private val graphRepository: GraphRepository) {

    /** Every node directly connected to [nodeId], regardless of edge direction. */
    suspend fun neighbors(nodeId: String): List<Node> = neighborsWithEdges(nodeId).map { it.neighbor }

    suspend fun neighborsWithEdges(nodeId: String): List<NeighborEdge> {
        val edges = graphRepository.edgesTouching(nodeId)
        return edges.mapNotNull { edge ->
            val neighborId = if (edge.fromNodeId == nodeId) edge.toNodeId else edge.fromNodeId
            graphRepository.getNode(neighborId)?.let { NeighborEdge(it, edge) }
        }
    }

    /**
     * Breadth-first search out to [maxHops], returning every MEMORY-type node reached
     * along the way — "what else have I written that's connected to this?" Issues one
     * DAO call per node visited rather than a single batched query — straightforward
     * and correct for the node counts Phase 2 targets, but the place to optimize first
     * (a WITH RECURSIVE query, or batching edgesTouching by a list of ids) if traversal
     * ever shows up as slow. Not silently assumed fast — see docs/PHASE2_STATUS.md.
     */
    suspend fun relatedMemories(nodeId: String, maxHops: Int = 2): List<Node> {
        val visited = mutableSetOf(nodeId)
        var frontier = setOf(nodeId)
        val memoryNodes = mutableListOf<Node>()

        for (hop in 0 until maxHops) {
            if (frontier.isEmpty()) break
            val nextFrontier = mutableSetOf<String>()
            for (id in frontier) {
                for (edge in graphRepository.edgesTouching(id)) {
                    val neighborId = if (edge.fromNodeId == id) edge.toNodeId else edge.fromNodeId
                    if (!visited.add(neighborId)) continue
                    nextFrontier.add(neighborId)
                    val node = graphRepository.getNode(neighborId)
                    if (node != null && node.type == NodeType.MEMORY) memoryNodes.add(node)
                }
            }
            frontier = nextFrontier
        }
        return memoryNodes
    }
}
