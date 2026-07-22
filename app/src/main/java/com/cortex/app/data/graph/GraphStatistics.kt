package com.cortex.app.data.graph

import com.cortex.app.domain.model.Node
import com.cortex.app.domain.model.NodeType
import com.cortex.app.domain.model.RelationType

data class GraphStats(
    val totalNodes: Int,
    val totalEdges: Int,
    val nodeCountByType: Map<NodeType, Int>,
    val edgeCountByType: Map<RelationType, Int>,
    val averageDegree: Double,
    val mostConnectedNodes: List<Pair<Node, Int>>
)

class GraphStatistics(private val graphRepository: GraphRepository) {

    suspend fun compute(topN: Int = 10): GraphStats {
        val nodes = graphRepository.getAllNodes()
        val edges = graphRepository.getAllEdges()

        val degree = HashMap<String, Int>()
        for (edge in edges) {
            degree[edge.fromNodeId] = (degree[edge.fromNodeId] ?: 0) + 1
            degree[edge.toNodeId] = (degree[edge.toNodeId] ?: 0) + 1
        }

        val averageDegree = if (nodes.isEmpty()) 0.0 else degree.values.sum().toDouble() / nodes.size

        val mostConnected = nodes
            .sortedByDescending { degree[it.id] ?: 0 }
            .take(topN)
            .map { it to (degree[it.id] ?: 0) }

        return GraphStats(
            totalNodes = nodes.size,
            totalEdges = edges.size,
            nodeCountByType = nodes.groupingBy { it.type }.eachCount(),
            edgeCountByType = edges.groupingBy { it.relationType }.eachCount(),
            averageDegree = averageDegree,
            mostConnectedNodes = mostConnected
        )
    }
}
