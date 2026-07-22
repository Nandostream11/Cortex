package com.cortex.app.domain.graph

import java.time.Instant
import kotlin.math.ln
import kotlin.math.pow

/** A minimal edge shape for ranking math — just enough to build an adjacency structure. */
data class RankingEdge(val fromId: String, val toId: String, val weight: Double)

/**
 * GraphEngine.md / Phase 2 Step 6. Every function here is pure and operates on plain
 * ids, so it's usable both for the real graph (via GraphRepository) and in isolation in
 * tests, without touching Room.
 */
object GraphRanking {

    /**
     * Degree (in + out, edge-weight-summed) normalized to [0,1] against the highest
     * degree actually observed in this graph, so a small graph doesn't make every node
     * look unimportant just because n-1 is small.
     */
    fun degreeCentrality(nodeIds: List<String>, edges: List<RankingEdge>): Map<String, Double> {
        if (nodeIds.isEmpty()) return emptyMap()
        val raw = nodeIds.associateWith { 0.0 }.toMutableMap()
        for (edge in edges) {
            raw[edge.fromId] = (raw[edge.fromId] ?: 0.0) + edge.weight
            raw[edge.toId] = (raw[edge.toId] ?: 0.0) + edge.weight
        }
        val max = raw.values.maxOrNull()?.takeIf { it > 0.0 } ?: return nodeIds.associateWith { 0.0 }
        return raw.mapValues { (_, v) -> v / max }
    }

    /**
     * Standard power-iteration PageRank with weighted edges and dangling-node mass
     * redistributed uniformly each iteration (otherwise rank leaks out of the system
     * through nodes with no outgoing edges, which describes most leaf entities here —
     * a Person or Technology node rarely points anywhere).
     */
    fun pageRank(
        nodeIds: List<String>,
        edges: List<RankingEdge>,
        damping: Double = 0.85,
        iterations: Int = 30
    ): Map<String, Double> {
        val n = nodeIds.size
        if (n == 0) return emptyMap()
        if (n == 1) return mapOf(nodeIds[0] to 1.0)

        val outWeightSum = nodeIds.associateWith { 0.0 }.toMutableMap()
        val outgoing = nodeIds.associateWith { mutableListOf<RankingEdge>() }
        for (edge in edges) {
            if (edge.fromId !in outgoing || edge.toId !in outgoing) continue // ignore dangling refs
            outgoing[edge.fromId]!!.add(edge)
            outWeightSum[edge.fromId] = (outWeightSum[edge.fromId] ?: 0.0) + edge.weight
        }

        var rank = nodeIds.associateWith { 1.0 / n }
        repeat(iterations) {
            val danglingMass = nodeIds.filter { (outWeightSum[it] ?: 0.0) == 0.0 }.sumOf { rank[it] ?: 0.0 }
            val base = (1.0 - damping) / n + damping * danglingMass / n

            val next = nodeIds.associateWith { base }.toMutableMap()
            for (fromId in nodeIds) {
                val total = outWeightSum[fromId] ?: 0.0
                if (total <= 0.0) continue
                val fromRank = rank[fromId] ?: 0.0
                for (edge in outgoing[fromId] ?: emptyList()) {
                    val share = damping * fromRank * (edge.weight / total)
                    next[edge.toId] = (next[edge.toId] ?: 0.0) + share
                }
            }
            rank = next
        }
        return rank
    }

    /**
     * Exponential recency decay: 1.0 right now, 0.5 at [halfLifeDays], approaching 0 as
     * [updatedAt] recedes into the past. MemoryEngine.md: "unused memories can decay in
     * rank, not disappear" — this is that decay curve.
     */
    fun recencyScore(updatedAt: Instant, now: Instant, halfLifeDays: Double = 14.0): Double {
        val ageDays = (now.epochSecond - updatedAt.epochSecond).coerceAtLeast(0) / 86_400.0
        return 0.5.pow(ageDays / halfLifeDays)
    }

    /** Squashes an unbounded count into [0,1] on a log scale so one very-frequent node can't dominate the score. */
    fun logNormalized(count: Int, cap: Int = 50): Double {
        if (count <= 0) return 0.0
        return (ln(1.0 + count) / ln(1.0 + cap)).coerceIn(0.0, 1.0)
    }

    data class CompositeWeights(
        val degree: Double = 0.2,
        val pageRank: Double = 0.3,
        val recency: Double = 0.25,
        val referenceCount: Double = 0.15,
        val frequency: Double = 0.1
    )

    /**
     * MemoryEngine.md's composite-importance idea, applied to graph nodes: blends
     * structural importance (degree, PageRank) with temporal relevance (recency) and
     * usage signals (reference count, frequency). All inputs pre-normalized to [0,1]
     * except the two counts, which this function normalizes itself.
     */
    fun compositeImportance(
        degreeCentrality: Double,
        pageRank: Double,
        recency: Double,
        referenceCount: Int,
        frequency: Int,
        weights: CompositeWeights = CompositeWeights()
    ): Double {
        val score = weights.degree * degreeCentrality +
            weights.pageRank * pageRank +
            weights.recency * recency +
            weights.referenceCount * logNormalized(referenceCount) +
            weights.frequency * logNormalized(frequency)
        return score.coerceIn(0.0, 1.0)
    }
}
