package com.cortex.app.data.graph

import com.cortex.app.domain.graph.GraphRanking
import com.cortex.app.domain.graph.RankingEdge
import com.cortex.app.domain.model.RelationType
import java.time.Instant

/**
 * Recomputes every node's composite importance score (Phase 2 Step 6). Deliberately
 * separate from [GraphBuilder] rather than folded into every single capture: ranking is
 * a whole-graph computation (PageRank needs every node and edge), so running it after
 * every capture is O(n) Room writes per capture. Fine at the node counts a personal
 * memory graph reaches early on; if that stops being true, this is the piece to move to
 * a debounced/background job — flagged here rather than silently left as a future
 * surprise (see docs/PHASE2_STATUS.md).
 */
class GraphUpdater(private val graphRepository: GraphRepository) {

    suspend fun recomputeAllImportanceScores(now: Instant = Instant.now()) {
        val nodes = graphRepository.getAllNodes()
        if (nodes.isEmpty()) return
        val edges = graphRepository.getAllEdges()

        val nodeIds = nodes.map { it.id }
        val rankingEdges = edges.map { RankingEdge(it.fromNodeId, it.toNodeId, it.weight) }

        val degree = GraphRanking.degreeCentrality(nodeIds, rankingEdges)
        val pageRank = GraphRanking.pageRank(nodeIds, rankingEdges)

        // How many memories mention this node — the inbound MENTIONS edge count.
        // "Frequency" reuses the same count for now: MemoryEngine.md's composite score
        // distinguishes "reference count" from "frequency of retrieval," and Cortex
        // doesn't track retrieval/read events yet, so there's nothing else to feed
        // frequency with honestly. Documented rather than faked with a second number
        // that would just equal the first under a different name.
        val referenceCounts = nodeIds.associateWith { id ->
            edges.count { it.toNodeId == id && it.relationType == RelationType.MENTIONS }
        }

        for (node in nodes) {
            val score = GraphRanking.compositeImportance(
                degreeCentrality = degree[node.id] ?: 0.0,
                pageRank = pageRank[node.id] ?: 0.0,
                recency = GraphRanking.recencyScore(node.updatedAt, now),
                referenceCount = referenceCounts[node.id] ?: 0,
                frequency = referenceCounts[node.id] ?: 0
            )
            graphRepository.updateNodeImportance(node.id, score)
        }
    }
}
