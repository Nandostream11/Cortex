package com.cortex.app.data.search

import com.cortex.app.data.graph.FakeGraphRepository
import com.cortex.app.data.graph.GraphQueryEngine
import com.cortex.app.data.repository.FakeMemoryRepository
import com.cortex.app.domain.model.MemoryCategory
import com.cortex.app.domain.model.MemoryItem
import com.cortex.app.domain.model.MemorySourceType
import com.cortex.app.domain.model.NodeType
import com.cortex.app.domain.model.RelationType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SearchEngineTest {

    private fun memory(id: String, text: String) = MemoryItem(
        id = id,
        createdAt = Instant.parse("2026-07-21T10:00:00Z"),
        updatedAt = Instant.parse("2026-07-21T10:00:00Z"),
        rawText = text,
        normalizedText = text,
        sourceType = MemorySourceType.TEXT,
        sourceRef = null,
        category = MemoryCategory.NOTE,
        importanceScore = 0.5,
        confidenceScore = 1.0,
        isPinned = false,
        isArchived = false
    )

    private fun buildEngine(memoryRepo: FakeMemoryRepository, graphRepo: FakeGraphRepository) =
        SearchEngine(memoryRepo, graphRepo, GraphQueryEngine(graphRepo))

    @Test
    fun `keyword search finds a memory containing the query term`() = runTest {
        val memoryRepo = FakeMemoryRepository()
        memoryRepo.save(memory("m1", "fix the null pointer exception in the graph module"))
        memoryRepo.save(memory("m2", "buy groceries: milk, eggs, bread"))

        val engine = buildEngine(memoryRepo, FakeGraphRepository())
        val results = engine.search("graph module")

        assertTrue(results.any { it.memory.id == "m1" })
        assertTrue(results.none { it.memory.id == "m2" })
    }

    @Test
    fun `a blank query returns no results`() = runTest {
        val memoryRepo = FakeMemoryRepository()
        memoryRepo.save(memory("m1", "some content"))
        val engine = buildEngine(memoryRepo, FakeGraphRepository())

        assertTrue(engine.search("   ").isEmpty())
    }

    @Test
    fun `searchByTag filters to memories with a matching tag, case-insensitively`() = runTest {
        val memoryRepo = FakeMemoryRepository()
        memoryRepo.save(memory("m1", "tagged note").copy(tagIds = listOf("Robotics")))
        memoryRepo.save(memory("m2", "untagged note"))

        val engine = buildEngine(memoryRepo, FakeGraphRepository())
        val results = engine.searchByTag("robotics")

        assertTrue(results.any { it.id == "m1" })
        assertTrue(results.none { it.id == "m2" })
    }

    @Test
    fun `searchByDateRange returns only memories created within the range`() = runTest {
        val memoryRepo = FakeMemoryRepository()
        memoryRepo.save(memory("m1", "in range"))
        memoryRepo.save(memory("m2", "out of range").copy(createdAt = Instant.parse("2020-01-01T00:00:00Z")))

        val engine = buildEngine(memoryRepo, FakeGraphRepository())
        val results = engine.searchByDateRange(
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-12-31T00:00:00Z")
        )

        assertTrue(results.any { it.id == "m1" })
        assertTrue(results.none { it.id == "m2" })
    }

    @Test
    fun `relatedMemories finds a memory sharing a graph node`() = runTest {
        val memoryRepo = FakeMemoryRepository()
        val graphRepo = FakeGraphRepository()

        memoryRepo.save(memory("m1", "working with Kotlin"))
        memoryRepo.save(memory("m2", "more Kotlin work"))

        val mem1Node = graphRepo.findOrCreateMemoryNode("m1", "working with Kotlin")
        val mem2Node = graphRepo.findOrCreateMemoryNode("m2", "more Kotlin work")
        val kotlinNode = graphRepo.findOrCreateNode("kotlin", NodeType.TOOL, "PROGRAMMING_LANGUAGE")
        graphRepo.createOrReinforceEdge(mem1Node.id, kotlinNode.id, RelationType.MENTIONS, 0.8, 0.8)
        graphRepo.createOrReinforceEdge(mem2Node.id, kotlinNode.id, RelationType.MENTIONS, 0.8, 0.8)

        val engine = buildEngine(memoryRepo, graphRepo)
        val related = engine.relatedMemories("m1")

        assertTrue(related.any { it.id == "m2" })
        assertTrue(related.none { it.id == "m1" }) // never includes itself
    }

    @Test
    fun `relatedMemories on a memory with no graph node returns empty rather than throwing`() = runTest {
        val memoryRepo = FakeMemoryRepository()
        memoryRepo.save(memory("orphan", "never linked to the graph"))
        val engine = buildEngine(memoryRepo, FakeGraphRepository())

        assertTrue(engine.relatedMemories("orphan").isEmpty())
    }
}
