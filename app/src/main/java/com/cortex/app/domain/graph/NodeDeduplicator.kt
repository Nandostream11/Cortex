package com.cortex.app.domain.graph

import com.cortex.app.domain.model.NodeType

/** The minimal shape NodeDeduplicator needs from an already-persisted node. */
data class ExistingNodeRef(val id: String, val type: NodeType, val canonicalName: String)

data class DedupResult(val matchedNodeId: String?, val canonicalName: String, val similarity: Double)

/**
 * GraphEngine.md Step 5: alias table first (cheap, exact, catches things similarity
 * can't), then string similarity within the same [NodeType] only — a Project and a
 * Person should never merge just because their names happen to look alike.
 */
class NodeDeduplicator(private val similarityThreshold: Double = 0.82) {

    /**
     * Resolves [rawName] through the alias table, then looks for an existing node of the
     * same [type] whose canonical name is similar enough to merge into. Returns the
     * matched node's id (null if this should become a new node) plus the canonical name
     * to store either way.
     */
    fun resolve(rawName: String, type: NodeType, existing: List<ExistingNodeRef>): DedupResult {
        val canonical = normalize(rawName)

        val sameType = existing.filter { it.type == type }

        val exact = sameType.firstOrNull { normalize(it.canonicalName) == canonical }
        if (exact != null) return DedupResult(exact.id, canonical, 1.0)

        var best: ExistingNodeRef? = null
        var bestScore = 0.0
        for (candidate in sameType) {
            val score = NodeSimilarity.combinedSimilarity(canonical, normalize(candidate.canonicalName))
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }

        return if (best != null && bestScore >= similarityThreshold) {
            DedupResult(best.id, canonical, bestScore)
        } else {
            DedupResult(null, canonical, bestScore)
        }
    }

    private fun normalize(name: String): String =
        AliasTable.resolve(name.trim().lowercase()).trim()
}
