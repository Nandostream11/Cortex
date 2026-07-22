package com.cortex.app.data.search

import com.cortex.app.data.graph.GraphQueryEngine
import com.cortex.app.data.graph.GraphRepository
import com.cortex.app.data.repository.MemoryRepository
import com.cortex.app.domain.graph.NodeSimilarity
import com.cortex.app.domain.model.MemoryItem
import com.cortex.app.domain.model.NodeType
import com.cortex.app.domain.search.Bm25Document
import com.cortex.app.domain.search.Bm25Index
import java.time.Instant

data class SearchResult(val memory: MemoryItem, val score: Double, val matchReason: String)

class SearchEngine(
    private val memoryRepository: MemoryRepository,
    private val graphRepository: GraphRepository,
    private val graphQueryEngine: GraphQueryEngine
) {
    // MemoryRepository has no "get everything" call; getRecent's underlying query is
    // ORDER BY createdAt DESC LIMIT :limit, so a large limit is a pragmatic stand-in for
    // "all active memories" at the scale a personal memory graph reaches early on.
    private val effectivelyAllMemories = 10_000

    /**
     * Keyword search via BM25 over every active memory's normalized text. Rebuilds the
     * BM25 index from scratch on every call rather than caching it — simplest-correct
     * choice for Phase 2, and fine at the memory counts a personal graph reaches early
     * on. The [SearchViewModel] debounces calls so this isn't run on every keystroke.
     * If this becomes a real cost, the fix is caching a [com.cortex.app.domain.search.Bm25Index]
     * and invalidating it on writes, not changing this method's contract — flagged here
     * rather than silently deferred (see docs/PHASE2_STATUS.md).
     */
    suspend fun search(queryText: String, limit: Int = 30): List<SearchResult> {
        if (queryText.isBlank()) return emptyList()
        val memories = memoryRepository.getRecent(limit = effectivelyAllMemories)
        val documents = memories.map { Bm25Document(it.id, Bm25Index.tokenize(it.normalizedText)) }
        val index = Bm25Index(documents)
        val matches = index.search(Bm25Index.tokenize(queryText), limit)
        val byId = memories.associateBy { it.id }
        return matches.mapNotNull { match ->
            byId[match.id]?.let { SearchResult(it, match.score, "keyword match") }
        }
    }

    suspend fun searchByTag(tag: String): List<MemoryItem> {
        val normalized = tag.trim().lowercase()
        return memoryRepository.getRecent(limit = effectivelyAllMemories).filter { memory ->
            memory.tagIds.any { it.trim().lowercase() == normalized }
        }
    }

    suspend fun searchByDateRange(start: Instant, end: Instant): List<MemoryItem> =
        memoryRepository.getRecent(limit = effectivelyAllMemories).filter { it.createdAt in start..end }

    /**
     * "Project search" / "Task search" / "Node search" from Phase 2 Step 8: find the
     * best-matching node of [type] (or any type if null) by name, then walk the graph
     * out from it for memories that mention it.
     */
    suspend fun searchByNodeName(name: String, type: NodeType? = null, maxHops: Int = 2): List<MemoryItem> {
        val candidates = graphRepository.getAllNodes().filter { type == null || it.type == type }
        val best = candidates.maxByOrNull { NodeSimilarity.combinedSimilarity(it.canonicalName, name) } ?: return emptyList()
        if (NodeSimilarity.combinedSimilarity(best.canonicalName, name) < 0.4) return emptyList()
        return memoriesFromNodeIds(graphQueryEngine.relatedMemories(best.id, maxHops).map { it.canonicalName })
    }

    /** Everything within [maxHops] of the memory graph node for [memoryId] — "related memories." */
    suspend fun relatedMemories(memoryId: String, maxHops: Int = 2): List<MemoryItem> {
        val memoryNode = graphRepository.findNodeByExactName(memoryId, NodeType.MEMORY) ?: return emptyList()
        val related = graphQueryEngine.relatedMemories(memoryNode.id, maxHops)
        return memoriesFromNodeIds(related.map { it.canonicalName }.filter { it != memoryId })
    }

    private suspend fun memoriesFromNodeIds(memoryIds: List<String>): List<MemoryItem> =
        memoryIds.mapNotNull { memoryRepository.getById(it) }
}
