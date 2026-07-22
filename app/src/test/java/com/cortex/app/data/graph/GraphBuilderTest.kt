package com.cortex.app.data.graph

import com.cortex.app.domain.model.MemoryCategory
import com.cortex.app.domain.model.MemoryItem
import com.cortex.app.domain.model.MemorySourceType
import com.cortex.app.domain.model.NodeType
import com.cortex.app.domain.model.RelationType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class GraphBuilderTest {

    private fun memory(text: String, category: MemoryCategory = MemoryCategory.NOTE) = MemoryItem(
        id = UUID.randomUUID().toString(),
        createdAt = Instant.parse("2026-07-21T10:00:00Z"),
        updatedAt = Instant.parse("2026-07-21T10:00:00Z"),
        rawText = text,
        normalizedText = text,
        sourceType = MemorySourceType.TEXT,
        sourceRef = null,
        category = category,
        importanceScore = 0.5,
        confidenceScore = 1.0,
        isPinned = false,
        isArchived = false
    )

    @Test
    fun `linking a memory creates a memory node plus one node per extracted entity`() = runTest {
        val repo = FakeGraphRepository()
        val builder = GraphBuilder(repo)

        val result = builder.linkMemory(memory("Task: fix CORTEX-99 in the Cortex project using Kotlin"))

        assertTrue(repo.allNodesSnapshot.any { it.type == NodeType.MEMORY })
        assertTrue(repo.allNodesSnapshot.any { it.type == NodeType.BUG })
        assertTrue(repo.allNodesSnapshot.any { it.type == NodeType.TASK })
        assertTrue(result.entityNodes.isNotEmpty())
    }

    @Test
    fun `every entity node is connected to the memory node via a MENTIONS edge`() = runTest {
        val repo = FakeGraphRepository()
        val builder = GraphBuilder(repo)

        val result = builder.linkMemory(memory("Using Python for this project"))

        val memoryNodeId = result.memoryNode.id
        for (entityNode in result.entityNodes) {
            val hasMentionsEdge = repo.allEdgesSnapshot.any {
                it.fromNodeId == memoryNodeId && it.toNodeId == entityNode.id && it.relationType == RelationType.MENTIONS
            }
            assertTrue("expected a MENTIONS edge from memory to ${entityNode.label}", hasMentionsEdge)
        }
    }

    @Test
    fun `linking the same memory twice does not duplicate its memory node`() = runTest {
        val repo = FakeGraphRepository()
        val builder = GraphBuilder(repo)
        val mem = memory("Simple note about Kotlin")

        builder.linkMemory(mem)
        builder.linkMemory(mem)

        val memoryNodes = repo.allNodesSnapshot.filter { it.type == NodeType.MEMORY && it.canonicalName == mem.id }
        assertEquals(1, memoryNodes.size)
    }

    @Test
    fun `mentioning the same technology in two different memories reuses one node`() = runTest {
        val repo = FakeGraphRepository()
        val builder = GraphBuilder(repo)

        builder.linkMemory(memory("Working with Python today"))
        builder.linkMemory(memory("More Python work tomorrow"))

        val pythonNodes = repo.allNodesSnapshot.filter { it.type == NodeType.TOOL && it.canonicalName.contains("python") }
        assertEquals(1, pythonNodes.size)
    }

    @Test
    fun `a bug-report memory infers a BLOCKS relation between the bug and a mentioned task`() = runTest {
        val repo = FakeGraphRepository()
        val builder = GraphBuilder(repo)

        builder.linkMemory(memory("Task: deploy release, blocked by CORTEX-5", category = MemoryCategory.BUG_REPORT))

        assertTrue(repo.allEdgesSnapshot.any { it.relationType == RelationType.BLOCKS })
    }

    @Test
    fun `a memory with no extractable entities still creates a memory node with no entity edges`() = runTest {
        val repo = FakeGraphRepository()
        val builder = GraphBuilder(repo)

        val result = builder.linkMemory(memory("hello"))

        assertTrue(repo.allNodesSnapshot.any { it.type == NodeType.MEMORY })
        assertTrue(result.entityNodes.isEmpty())
    }
}
