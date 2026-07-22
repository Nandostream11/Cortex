package com.cortex.app.domain.graph

import com.cortex.app.domain.model.MemoryCategory
import com.cortex.app.domain.model.RelationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationInfererTest {

    private val inferer = RelationInferer()

    private fun entity(kind: EntityKind, value: String) =
        ExtractedEntity(kind, value, value, 0, value.length, 0.7)

    @Test
    fun `task and project infer a DEPENDS_ON relation`() {
        val relations = inferer.infer(
            listOf(entity(EntityKind.TASK, "fix bug"), entity(EntityKind.PROJECT, "Cortex")),
            MemoryCategory.TASK
        )
        assertTrue(relations.any {
            it.fromKind == EntityKind.TASK && it.toKind == EntityKind.PROJECT && it.relationType == RelationType.DEPENDS_ON
        })
    }

    @Test
    fun `bug blocking task only fires for BUG_REPORT category`() {
        val entities = listOf(entity(EntityKind.BUG_ID, "CORTEX-1"), entity(EntityKind.TASK, "fix bug"))

        val whenBugReport = inferer.infer(entities, MemoryCategory.BUG_REPORT)
        assertTrue(whenBugReport.any { it.relationType == RelationType.BLOCKS })

        val whenNote = inferer.infer(entities, MemoryCategory.NOTE)
        assertFalse(whenNote.any { it.relationType == RelationType.BLOCKS })
    }

    @Test
    fun `a git commit resolving a bug maps to RESOLVES`() {
        val relations = inferer.infer(
            listOf(entity(EntityKind.GIT_COMMIT, "a1b2c3d"), entity(EntityKind.BUG_ID, "CORTEX-9")),
            MemoryCategory.BUG_REPORT
        )
        assertTrue(relations.any { it.relationType == RelationType.RESOLVES })
    }

    @Test
    fun `fewer than two distinct entities yields no relations`() {
        assertTrue(inferer.infer(listOf(entity(EntityKind.PROJECT, "Cortex")), MemoryCategory.NOTE).isEmpty())
        assertTrue(inferer.infer(emptyList(), MemoryCategory.NOTE).isEmpty())
    }

    @Test
    fun `every inferred relation has confidence below 1_0`() {
        val relations = inferer.infer(
            listOf(entity(EntityKind.TASK, "fix bug"), entity(EntityKind.PROJECT, "Cortex")),
            MemoryCategory.TASK
        )
        assertTrue(relations.all { it.confidence < 1.0 })
    }

    @Test
    fun `duplicate entity mentions do not produce duplicate relations`() {
        val relations = inferer.infer(
            listOf(
                entity(EntityKind.TASK, "fix bug"),
                entity(EntityKind.TASK, "fix bug"), // same kind+value repeated
                entity(EntityKind.PROJECT, "Cortex")
            ),
            MemoryCategory.TASK
        )
        val dependsOnCount = relations.count { it.relationType == RelationType.DEPENDS_ON }
        assertEquals(1, dependsOnCount)
    }
}
