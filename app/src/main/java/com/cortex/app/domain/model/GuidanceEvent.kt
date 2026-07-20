package com.cortex.app.domain.model

import java.time.Instant

/**
 * A single piece of guidance surfaced to the user (summary, suggestion, follow-up
 * question, pattern insight, etc.). Always ties back to the memories that justify it
 * so guidance stays explainable, per GuidanceEngine.md's quality rules.
 */
data class GuidanceEvent(
    val id: String,
    val type: GuidanceEventType,
    val content: String,
    val createdAt: Instant,
    val sourceMemoryIds: List<String>
)

enum class GuidanceEventType {
    SUMMARY,
    NEXT_STEP,
    FOLLOW_UP_QUESTION,
    CONTRADICTION_WARNING,
    PATTERN_INSIGHT,
    PROJECT_STATUS,
    DAILY_BRIEF
}
