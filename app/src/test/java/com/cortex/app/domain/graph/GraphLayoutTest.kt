package com.cortex.app.domain.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class GraphLayoutTest {

    private fun dist(a: Point2D, b: Point2D): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    @Test
    fun `every node gets a position within bounds`() {
        val nodeIds = (1..20).map { "n$it" }
        val positions = GraphLayout.forceDirectedLayout(nodeIds, emptyList(), width = 1000.0, height = 1000.0)
        assertEquals(20, positions.size)
        positions.values.forEach { p ->
            assertTrue(!p.x.isNaN() && !p.y.isNaN())
            assertTrue(p.x in 0.0..1000.0 && p.y in 0.0..1000.0)
        }
    }

    @Test
    fun `nodes do not collapse onto the same point`() {
        val nodeIds = (1..10).map { "n$it" }
        val positions = GraphLayout.forceDirectedLayout(nodeIds, emptyList(), iterations = 100)
        val coords = positions.values.toList()
        for (i in coords.indices) for (j in coords.indices) {
            if (i == j) continue
            assertTrue(dist(coords[i], coords[j]) > 1.0)
        }
    }

    @Test
    fun `connected nodes end up closer together than unrelated ones`() {
        val nodeIds = (1..8).map { "n$it" }
        val edges = listOf(RankingEdge("n1", "n2", 1.0), RankingEdge("n1", "n3", 1.0), RankingEdge("n1", "n4", 1.0))
        val positions = GraphLayout.forceDirectedLayout(nodeIds, edges, iterations = 150)
        val withinCluster = dist(positions.getValue("n1"), positions.getValue("n2"))
        val acrossClusters = dist(positions.getValue("n1"), positions.getValue("n5"))
        assertTrue(withinCluster < acrossClusters)
    }

    @Test
    fun `layout is deterministic for the same input`() {
        val nodeIds = (1..12).map { "n$it" }
        val edges = listOf(RankingEdge("n1", "n2", 1.0), RankingEdge("n3", "n4", 1.0))
        val first = GraphLayout.forceDirectedLayout(nodeIds, edges)
        val second = GraphLayout.forceDirectedLayout(nodeIds, edges)
        assertEquals(first, second)
    }

    @Test
    fun `empty and single-node graphs are handled without error`() {
        assertTrue(GraphLayout.forceDirectedLayout(emptyList(), emptyList()).isEmpty())
        val single = GraphLayout.forceDirectedLayout(listOf("solo"), emptyList())
        assertEquals(1, single.size)
    }

    @Test
    fun `edges referencing unknown node ids are ignored rather than throwing`() {
        val nodeIds = listOf("n1", "n2")
        val edges = listOf(RankingEdge("n1", "ghost", 1.0))
        val positions = GraphLayout.forceDirectedLayout(nodeIds, edges)
        assertEquals(2, positions.size)
    }
}
