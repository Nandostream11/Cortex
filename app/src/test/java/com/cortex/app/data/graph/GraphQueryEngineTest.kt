package com.cortex.app.data.graph

import com.cortex.app.domain.model.NodeType
import com.cortex.app.domain.model.RelationType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphQueryEngineTest {

    @Test
    fun `neighbors returns nodes connected in either edge direction`() = runTest {
        val repo = FakeGraphRepository()
        val a = repo.findOrCreateNode("A", NodeType.PROJECT, null)
        val b = repo.findOrCreateNode("B", NodeType.PERSON, null)
        val c = repo.findOrCreateNode("C", NodeType.TOOL, null)
        repo.createOrReinforceEdge(a.id, b.id, RelationType.RELATED_TO, 0.5, 0.5) // a -> b
        repo.createOrReinforceEdge(c.id, a.id, RelationType.RELATED_TO, 0.5, 0.5) // c -> a

        val engine = GraphQueryEngine(repo)
        val neighbors = engine.neighbors(a.id).map { it.id }.toSet()

        assertEquals(setOf(b.id, c.id), neighbors)
    }

    @Test
    fun `relatedMemories finds a memory two hops away through a shared entity`() = runTest {
        val repo = FakeGraphRepository()
        val mem1 = repo.findOrCreateMemoryNode("mem-1", "first memory")
        val mem2 = repo.findOrCreateMemoryNode("mem-2", "second memory")
        val sharedTool = repo.findOrCreateNode("Kotlin", NodeType.TOOL, null)

        repo.createOrReinforceEdge(mem1.id, sharedTool.id, RelationType.MENTIONS, 0.8, 0.8)
        repo.createOrReinforceEdge(mem2.id, sharedTool.id, RelationType.MENTIONS, 0.8, 0.8)

        val engine = GraphQueryEngine(repo)
        val related = engine.relatedMemories(mem1.id, maxHops = 2)

        assertTrue(related.any { it.id == mem2.id })
    }

    @Test
    fun `relatedMemories does not find a memory beyond maxHops`() = runTest {
        val repo = FakeGraphRepository()
        val mem1 = repo.findOrCreateMemoryNode("mem-1", "first")
        val bridge = repo.findOrCreateNode("Bridge", NodeType.CONCEPT, null)
        val mem2 = repo.findOrCreateMemoryNode("mem-2", "second")
        val bridge2 = repo.findOrCreateNode("Bridge2", NodeType.CONCEPT, null)

        // mem1 -> bridge -> bridge2 -> mem2 is 3 hops
        repo.createOrReinforceEdge(mem1.id, bridge.id, RelationType.MENTIONS, 0.5, 0.5)
        repo.createOrReinforceEdge(bridge.id, bridge2.id, RelationType.RELATED_TO, 0.5, 0.5)
        repo.createOrReinforceEdge(bridge2.id, mem2.id, RelationType.MENTIONS, 0.5, 0.5)

        val engine = GraphQueryEngine(repo)
        val relatedAt1Hop = engine.relatedMemories(mem1.id, maxHops = 1)
        val relatedAt3Hops = engine.relatedMemories(mem1.id, maxHops = 3)

        assertTrue(relatedAt1Hop.none { it.id == mem2.id })
        assertTrue(relatedAt3Hops.any { it.id == mem2.id })
    }

    @Test
    fun `an isolated node with no edges has no neighbors and no related memories`() = runTest {
        val repo = FakeGraphRepository()
        val lonely = repo.findOrCreateNode("Lonely", NodeType.CONCEPT, null)
        val engine = GraphQueryEngine(repo)

        assertTrue(engine.neighbors(lonely.id).isEmpty())
        assertTrue(engine.relatedMemories(lonely.id).isEmpty())
    }
}

class GraphStatisticsTest {

    @Test
    fun `computes total counts and per-type breakdowns`() = runTest {
        val repo = FakeGraphRepository()
        val p1 = repo.findOrCreateNode("Project A", NodeType.PROJECT, null)
        val p2 = repo.findOrCreateNode("Project B", NodeType.PROJECT, null)
        val person = repo.findOrCreateNode("Alice", NodeType.PERSON, null)
        repo.createOrReinforceEdge(p1.id, person.id, RelationType.RELATED_TO, 0.5, 0.5)

        val stats = GraphStatistics(repo).compute()

        assertEquals(3, stats.totalNodes)
        assertEquals(1, stats.totalEdges)
        assertEquals(2, stats.nodeCountByType[NodeType.PROJECT])
        assertEquals(1, stats.nodeCountByType[NodeType.PERSON])
        assertEquals(1, stats.edgeCountByType[RelationType.RELATED_TO])
        assertTrue(p2.id != p1.id) // sanity: two distinct projects were actually created
    }

    @Test
    fun `most connected nodes are ranked by degree`() = runTest {
        val repo = FakeGraphRepository()
        val hub = repo.findOrCreateNode("Hub", NodeType.PROJECT, null)
        val leaf1 = repo.findOrCreateNode("Leaf1", NodeType.TOOL, null)
        val leaf2 = repo.findOrCreateNode("Leaf2", NodeType.TOOL, null)
        val leaf3 = repo.findOrCreateNode("Leaf3", NodeType.TOOL, null)
        repo.createOrReinforceEdge(hub.id, leaf1.id, RelationType.DEPENDS_ON, 0.5, 0.5)
        repo.createOrReinforceEdge(hub.id, leaf2.id, RelationType.DEPENDS_ON, 0.5, 0.5)
        repo.createOrReinforceEdge(hub.id, leaf3.id, RelationType.DEPENDS_ON, 0.5, 0.5)

        val stats = GraphStatistics(repo).compute(topN = 1)

        assertEquals(1, stats.mostConnectedNodes.size)
        assertEquals(hub.id, stats.mostConnectedNodes.first().first.id)
        assertEquals(3, stats.mostConnectedNodes.first().second)
    }

    @Test
    fun `an empty graph produces zeroed-out statistics without throwing`() = runTest {
        val stats = GraphStatistics(FakeGraphRepository()).compute()
        assertEquals(0, stats.totalNodes)
        assertEquals(0, stats.totalEdges)
        assertEquals(0.0, stats.averageDegree, 0.0001)
        assertTrue(stats.mostConnectedNodes.isEmpty())
    }
}
