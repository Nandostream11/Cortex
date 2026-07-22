package com.cortex.app.domain.graph

import com.cortex.app.domain.model.MemoryCategory
import com.cortex.app.domain.model.RelationType

/**
 * Infers edges between entities that co-occur in the same memory. Deliberately
 * conservative: every rule fires on a specific, explainable co-occurrence pattern, and
 * every relation carries a confidence below 1.0 and a human-readable rationale, since
 * these are heuristic guesses, not certainties (GraphEngine.md: "edge confidence should
 * be scored").
 *
 * GraphEngine.md's example relations use words ("belongs_to", "resolved_by", "used_in",
 * "executed_for") that don't all exist as distinct [RelationType] values — the schema
 * has 10 edge types, not one per English verb. Each rule below documents which existing
 * type it maps onto and why.
 */
class RelationInferer {

    private data class Rule(
        val fromKind: EntityKind,
        val toKind: EntityKind,
        val relationType: RelationType,
        val confidence: Double,
        val rationale: String,
        val requiresCategory: MemoryCategory? = null
    )

    private val rules = listOf(
        // "Task belongs_to Project" — no dedicated part-of edge type exists, DEPENDS_ON
        // is the closest fit (a task's scope depends on/is defined by its project).
        Rule(EntityKind.TASK, EntityKind.PROJECT, RelationType.DEPENDS_ON, 0.6, "task is scoped to this project"),

        // "Repository related_to Project" maps directly onto RELATED_TO.
        Rule(EntityKind.REPOSITORY, EntityKind.PROJECT, RelationType.RELATED_TO, 0.6, "repository associated with project"),

        // "Technology used_in Project" — read as the project depending on the tech.
        Rule(EntityKind.PROJECT, EntityKind.TECHNOLOGY, RelationType.DEPENDS_ON, 0.55, "project appears to use this technology"),
        Rule(EntityKind.PROJECT, EntityKind.FRAMEWORK, RelationType.DEPENDS_ON, 0.55, "project appears to use this framework"),
        Rule(EntityKind.PROJECT, EntityKind.LIBRARY, RelationType.DEPENDS_ON, 0.55, "project appears to use this library"),
        Rule(EntityKind.PROJECT, EntityKind.PROGRAMMING_LANGUAGE, RelationType.DEPENDS_ON, 0.55, "project appears to use this language"),
        Rule(EntityKind.PROJECT, EntityKind.HARDWARE, RelationType.DEPENDS_ON, 0.5, "project appears to target this hardware"),

        // "Command executed_for Bug" — FOLLOW_UP_ON captures "action taken in response to."
        Rule(EntityKind.COMMAND, EntityKind.BUG_ID, RelationType.FOLLOW_UP_ON, 0.5, "command run in the context of this bug"),

        // A commit/branch mentioned alongside a bug id is very often the fix for it.
        Rule(EntityKind.GIT_COMMIT, EntityKind.BUG_ID, RelationType.RESOLVES, 0.55, "commit likely addresses this bug"),
        Rule(EntityKind.BRANCH_NAME, EntityKind.BUG_ID, RelationType.RESOLVES, 0.5, "branch name suggests it addresses this bug"),

        // "Paper supports Idea" maps directly onto SUPPORTS.
        Rule(EntityKind.RESEARCH_PAPER, EntityKind.PROJECT, RelationType.SUPPORTS, 0.5, "paper referenced in this project's context"),
        Rule(EntityKind.RESEARCH_PAPER, EntityKind.GOAL, RelationType.SUPPORTS, 0.5, "paper referenced in support of this goal"),

        // Weak signals: a mentioned person is related to, not necessarily working on.
        Rule(EntityKind.PERSON, EntityKind.PROJECT, RelationType.RELATED_TO, 0.4, "person mentioned alongside this project"),
        Rule(EntityKind.PERSON, EntityKind.TASK, RelationType.RELATED_TO, 0.4, "person mentioned alongside this task"),

        // Only fires for bug-report-flavored memories, where a mentioned task is more
        // plausibly blocked by the bug than merely "related."
        Rule(EntityKind.BUG_ID, EntityKind.TASK, RelationType.BLOCKS, 0.45, "bug may be blocking this task", MemoryCategory.BUG_REPORT)
    )

    /**
     * [entities] should be everything [EntityExtractor] found in one memory.
     * Deduplicates by (kind, normalizedValue) first so repeated mentions of the same
     * entity in one memory don't produce redundant relations.
     */
    fun infer(entities: List<ExtractedEntity>, memoryCategory: MemoryCategory): List<InferredRelation> {
        val distinct = entities.distinctBy { it.kind to it.normalizedValue }
        if (distinct.size < 2) return emptyList()

        val results = mutableListOf<InferredRelation>()
        for (i in distinct.indices) {
            for (j in distinct.indices) {
                if (i == j) continue
                val a = distinct[i]
                val b = distinct[j]
                val rule = rules.firstOrNull {
                    it.fromKind == a.kind && it.toKind == b.kind &&
                        (it.requiresCategory == null || it.requiresCategory == memoryCategory)
                } ?: continue
                results.add(
                    InferredRelation(
                        fromKind = a.kind,
                        fromValue = a.normalizedValue,
                        toKind = b.kind,
                        toValue = b.normalizedValue,
                        relationType = rule.relationType,
                        confidence = rule.confidence,
                        rationale = rule.rationale
                    )
                )
            }
        }
        return results
    }
}
