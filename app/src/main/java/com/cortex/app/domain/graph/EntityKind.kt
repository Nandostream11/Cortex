package com.cortex.app.domain.graph

/**
 * Fine-grained entity categories extracted from raw text (Phase 2 Step 3). This is
 * intentionally more granular than [com.cortex.app.domain.model.NodeType] — GraphEngine.md
 * documents 10 node types, and expanding that enum to 24+ variants would be a breaking
 * change to an already-shipped contract for little benefit. Instead, [EntityToNodeMapping]
 * folds each kind down to a NodeType plus a free-text subtype string that preserves the
 * extraction detail without changing the schema's shape.
 */
enum class EntityKind {
    PROJECT,
    TASK,
    GOAL,
    PERSON,
    TECHNOLOGY,
    PROGRAMMING_LANGUAGE,
    FRAMEWORK,
    LIBRARY,
    HARDWARE,
    RESEARCH_PAPER,
    REPOSITORY,
    URL,
    COMMAND,
    FILE_PATH,
    DATE,
    TIME,
    NUMBER,
    VERSION,
    BUG_ID,
    ERROR_MESSAGE,
    OS_NAME,
    PACKAGE_NAME,
    BRANCH_NAME,
    GIT_COMMIT
}

/**
 * A single extraction hit: what was found, where, and how confident the extractor is.
 * [normalizedValue] is what gets used for node canonicalization/dedup; [rawValue] is kept
 * for provenance/display, per MemoryEngine.md's "preserve technical terms... store raw
 * and normalized text."
 */
data class ExtractedEntity(
    val kind: EntityKind,
    val rawValue: String,
    val normalizedValue: String,
    val startIndex: Int,
    val endIndex: Int,
    val confidence: Double
)
