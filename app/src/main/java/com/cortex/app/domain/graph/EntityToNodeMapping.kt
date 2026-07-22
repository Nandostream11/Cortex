package com.cortex.app.domain.graph

import com.cortex.app.domain.model.NodeType

/**
 * Where each [EntityKind] lands in the graph. Two deliberate design choices, both
 * because "every memory eventually becomes structured data" (the constitution) doesn't
 * mean "every extracted token becomes a permanent node":
 *
 * - DATE, TIME, NUMBER, VERSION, ERROR_MESSAGE are extracted (useful as classification
 *   signal and for potential future due-date parsing) but never promoted to a node.
 *   Creating a node per date/number mention would flood the graph with low-reuse,
 *   low-signal nodes — dates and numbers are attributes of a memory, not entities the
 *   graph should track relationships for.
 * - Several kinds collapse onto [NodeType.TOOL] or [NodeType.CONCEPT] since
 *   GraphEngine.md's schema has 10 node types, not one per EntityKind. [subtypeFor]
 *   keeps the finer distinction as metadata (Node.subtype) instead of losing it.
 *
 * One thing that looks like it should be a third exclusion but isn't: GIT_COMMIT,
 * FILE_PATH, URL, BRANCH_NAME, and COMMAND are usually mentioned exactly once each — a
 * commit sha in particular is close to globally unique and will almost never repeat.
 * They're still promoted to nodes anyway, because unlike a bare date or number, they're
 * frequently the *target* of a real relationship worth keeping (a specific commit
 * RESOLVES a specific bug; RelationInferer needs a node id on both ends to record that
 * edge at all). A single-use node isn't wasted here — it's provenance. GraphRanking
 * already keeps these from cluttering anything that matters: a degree-1, rarely-linked
 * node naturally gets a low composite importance score, so it renders small in
 * GraphScreen and stays out of GraphStatistics' "most connected" list without needing
 * to be excluded from the graph outright.
 */
object EntityToNodeMapping {

    private val notPromoted = setOf(
        EntityKind.DATE, EntityKind.TIME, EntityKind.NUMBER, EntityKind.VERSION, EntityKind.ERROR_MESSAGE
    )

    private val nodeTypeFor: Map<EntityKind, NodeType> = mapOf(
        EntityKind.PROJECT to NodeType.PROJECT,
        EntityKind.TASK to NodeType.TASK,
        EntityKind.GOAL to NodeType.GOAL,
        EntityKind.PERSON to NodeType.PERSON,
        EntityKind.TECHNOLOGY to NodeType.TOOL,
        EntityKind.PROGRAMMING_LANGUAGE to NodeType.TOOL,
        EntityKind.FRAMEWORK to NodeType.TOOL,
        EntityKind.LIBRARY to NodeType.TOOL,
        EntityKind.HARDWARE to NodeType.TOOL,
        EntityKind.RESEARCH_PAPER to NodeType.PAPER,
        EntityKind.REPOSITORY to NodeType.TOOL,
        EntityKind.URL to NodeType.CONCEPT,
        EntityKind.COMMAND to NodeType.CONCEPT,
        EntityKind.FILE_PATH to NodeType.CONCEPT,
        EntityKind.BUG_ID to NodeType.BUG,
        EntityKind.OS_NAME to NodeType.TOOL,
        EntityKind.PACKAGE_NAME to NodeType.TOOL,
        EntityKind.BRANCH_NAME to NodeType.CONCEPT,
        EntityKind.GIT_COMMIT to NodeType.CONCEPT
    )

    fun isPromotedToNode(kind: EntityKind): Boolean = kind !in notPromoted

    /** Null if [kind] is deliberately not promoted to a node — see class doc. */
    fun nodeTypeFor(kind: EntityKind): NodeType? = nodeTypeFor[kind]

    /** Preserves the finer EntityKind as Node.subtype even where several kinds share one NodeType. */
    fun subtypeFor(kind: EntityKind): String = kind.name
}
