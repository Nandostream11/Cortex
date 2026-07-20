package com.cortex.app.domain.model

/** Where a piece of capture originated. */
enum class MemorySourceType {
    VOICE,
    TEXT,
    CONNECTOR
}

/** Categories from MemoryEngine.md — drives icon, ranking weight, and guidance behavior. */
enum class MemoryCategory {
    IDEA,
    NOTE,
    TASK,
    PROJECT_UPDATE,
    BUG_REPORT,
    PAPER_NOTE,
    DECISION,
    REFLECTION,
    REMINDER,
    CONNECTOR_IMPORT
}

/** Node types from GraphEngine.md / GraphSchema.json. */
enum class NodeType {
    MEMORY,
    CONCEPT,
    PROJECT,
    TASK,
    PERSON,
    TOOL,
    PAPER,
    BUG,
    GOAL,
    CONNECTOR_SOURCE
}

/** Edge/relation types from GraphEngine.md / GraphSchema.json. */
enum class RelationType {
    MENTIONS,
    RELATED_TO,
    DERIVED_FROM,
    DEPENDS_ON,
    BLOCKS,
    RESOLVES,
    FOLLOW_UP_ON,
    SUPPORTS,
    CONTRADICTS,
    IMPORTED_FROM
}

enum class TaskStatus {
    OPEN,
    IN_PROGRESS,
    BLOCKED,
    DONE,
    CANCELLED
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class ProjectStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED
}

/** Guidance autonomy level, per GuidanceEngine.md and ExportFormat.json settings. */
enum class AutonomyMode {
    PASSIVE,
    PROACTIVE,
    THINKING,
    DAILY_BRIEF
}
