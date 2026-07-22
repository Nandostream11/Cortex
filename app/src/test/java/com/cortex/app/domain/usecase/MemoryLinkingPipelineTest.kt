package com.cortex.app.domain.usecase

import com.cortex.app.data.graph.FakeGraphRepository
import com.cortex.app.data.graph.GraphBuilder
import com.cortex.app.data.graph.GraphUpdater
import com.cortex.app.data.repository.FakeMemoryRepository
import com.cortex.app.domain.model.NodeType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryLinkingPipelineTest {

    private fun buildPipeline(memoryRepo: FakeMemoryRepository, graphRepo: FakeGraphRepository) =
        MemoryLinkingPipeline(
            CaptureTextMemoryUseCase(memoryRepo),
            GraphBuilder(graphRepo),
            GraphUpdater(graphRepo)
        )

    @Test
    fun `capturing text saves the memory and links it into the graph`() = runTest {
        val memoryRepo = FakeMemoryRepository()
        val graphRepo = FakeGraphRepository()
        val pipeline = buildPipeline(memoryRepo, graphRepo)

        val result = pipeline("Task: fix the bug in the Cortex project using Kotlin")

        assertTrue(memoryRepo.saved.any { it.id == result.memory.id })
        assertTrue(graphRepo.allNodesSnapshot.any { it.type == NodeType.MEMORY })
        assertTrue(result.graphLink.entityNodes.isNotEmpty())
    }

    @Test
    fun `capturing text refreshes node importance scores`() = runTest {
        val memoryRepo = FakeMemoryRepository()
        val graphRepo = FakeGraphRepository()
        val pipeline = buildPipeline(memoryRepo, graphRepo)

        pipeline("Using Python for the Cortex project")
        pipeline("More Python work on Cortex")

        // After two captures mentioning the same tool, at least one node should have a
        // nonzero importance score (recency + reference count both contribute).
        assertTrue(graphRepo.allNodesSnapshot.any { it.importanceScore > 0.0 })
    }

    @Test
    fun `blank text still fails before reaching the graph`() = runTest {
        val memoryRepo = FakeMemoryRepository()
        val graphRepo = FakeGraphRepository()
        val pipeline = buildPipeline(memoryRepo, graphRepo)

        var threw = false
        try {
            pipeline("   ")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
        assertTrue(graphRepo.allNodesSnapshot.isEmpty())
    }
}
