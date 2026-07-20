package com.cortex.app.domain.usecase

import com.cortex.app.data.repository.MemoryRepository
import com.cortex.app.domain.model.MemoryCategory
import com.cortex.app.domain.model.MemoryItem
import com.cortex.app.domain.model.MemorySourceType
import java.time.Instant
import java.util.UUID

/**
 * Turns raw captured text into a stored [MemoryItem].
 *
 * This is intentionally the *thin* Phase 1 slice of MemoryEngine.md's nine-step
 * pipeline: normalize -> classify -> score -> persist. Entity extraction, relation
 * extraction, dedup, and graph/index updates are Phase 2 (GraphEngine.md) and are not
 * implemented here — this use case is the seam they'll hook into.
 */
class CaptureTextMemoryUseCase(private val repository: MemoryRepository) {

    suspend operator fun invoke(
        rawText: String,
        sourceType: MemorySourceType = MemorySourceType.TEXT,
        sourceRef: String? = null
    ): MemoryItem {
        require(rawText.isNotBlank()) { "Cannot capture empty text." }

        val normalized = normalize(rawText)
        val category = classify(normalized)
        val importance = scoreImportance(normalized, category)

        val now = Instant.now()
        val item = MemoryItem(
            id = UUID.randomUUID().toString(),
            createdAt = now,
            updatedAt = now,
            rawText = rawText,
            normalizedText = normalized,
            sourceType = sourceType,
            sourceRef = sourceRef,
            category = category,
            importanceScore = importance,
            // Text capture is authored directly by the user, so treat it as fully
            // trustworthy. Voice (ASR) and connector imports get their own confidence
            // scoring once those pipelines exist.
            confidenceScore = if (sourceType == MemorySourceType.TEXT) 1.0 else 0.7,
            isPinned = false,
            isArchived = false
        )
        repository.save(item)
        return item
    }

    /** Deterministic normalization: whitespace/noise cleanup only, per MemoryEngine.md. */
    private fun normalize(text: String): String =
        text.trim().replace(Regex("\\s+"), " ")

    /**
     * Lightweight keyword-based classifier. This is a deliberately simple heuristic to
     * seed the pipeline — MemoryEngine.md's full categorization (and eventual OpenRouter
     * assist for ambiguous cases) is Phase 2 work.
     */
    private fun classify(text: String): MemoryCategory {
        val lower = text.lowercase()
        return when {
            listOf("bug", "crash", "error", "exception", "stack trace").any { lower.contains(it) } ->
                MemoryCategory.BUG_REPORT
            listOf("todo", "need to", "remember to", "don't forget").any { lower.contains(it) } ->
                MemoryCategory.REMINDER
            listOf("decided", "decision:", "going with", "we'll use").any { lower.contains(it) } ->
                MemoryCategory.DECISION
            listOf("paper", "arxiv", "citation", "et al").any { lower.contains(it) } ->
                MemoryCategory.PAPER_NOTE
            listOf("task:", "next step", "action item").any { lower.contains(it) } ->
                MemoryCategory.TASK
            listOf("shipped", "progress on", "status:", "update:").any { lower.contains(it) } ->
                MemoryCategory.PROJECT_UPDATE
            text.trimEnd().endsWith("?") -> MemoryCategory.REFLECTION
            else -> MemoryCategory.IDEA
        }
    }

    /**
     * Baseline importance score in [0.0, 1.0]. Recency/frequency/graph-link factors from
     * MemoryEngine.md apply after the item has lived in the graph a while; at capture
     * time only content-intrinsic signals exist.
     */
    private fun scoreImportance(text: String, category: MemoryCategory): Double {
        var score = 0.5
        val categoryWeight = when (category) {
            MemoryCategory.DECISION, MemoryCategory.BUG_REPORT -> 0.15
            MemoryCategory.TASK, MemoryCategory.REMINDER -> 0.1
            MemoryCategory.PROJECT_UPDATE, MemoryCategory.PAPER_NOTE -> 0.05
            else -> 0.0
        }
        score += categoryWeight
        // Very short captures ("k", "ok") are rarely important; longer, substantive
        // captures get a small boost, capped so length alone can't dominate.
        val lengthBoost = (text.length / 400.0).coerceAtMost(0.2)
        score += lengthBoost
        return score.coerceIn(0.0, 1.0)
    }
}
