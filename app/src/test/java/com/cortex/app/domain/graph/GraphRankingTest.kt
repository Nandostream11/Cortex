package com.cortex.app.domain.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GraphRankingTest {

    private val nodeIds = listOf("cortex", "a", "b", "c", "isolated1", "isolated2")
    private val edges = listOf(
        RankingEdge("a", "cortex", 1.0),
        RankingEdge("b", "cortex", 1.0),
        RankingEdge("c", "cortex", 1.0),
        RankingEdge("isolated1", "isolated2", 1.0)
    )

    @Test
    fun `pagerank sums to approximately 1 and ranks a hub above its spokes`() {
        val pr = GraphRanking.pageRank(nodeIds, edges)
        assertTrue(pr.values.all { it in 0.0..1.0 })
        assertEquals(1.0, pr.values.sum(), 0.01)
        assertTrue(pr.getValue("cortex") > pr.getValue("a"))
        assertTrue(pr.getValue("cortex") > pr.getValue("isolated1"))
    }

    @Test
    fun `pagerank on a single node returns 1_0`() {
        val pr = GraphRanking.pageRank(listOf("solo"), emptyList())
        assertEquals(1.0, pr.getValue("solo"), 0.0001)
    }

    @Test
    fun `pagerank on an empty graph returns an empty map`() {
        assertTrue(GraphRanking.pageRank(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `degree centrality normalizes to 1_0 at the observed maximum`() {
        val degree = GraphRanking.degreeCentrality(nodeIds, edges)
        assertEquals(1.0, degree.getValue("cortex"), 0.0001)
        assertTrue(degree.getValue("a") < 1.0)
    }

    @Test
    fun `recency score is 1 now, 0_5 at one half-life, and near zero far in the past`() {
        val now = Instant.parse("2026-07-21T00:00:00Z")
        assertEquals(1.0, GraphRanking.recencyScore(now, now, 14.0), 0.0001)
        assertEquals(0.5, GraphRanking.recencyScore(now.minusSeconds(14L * 86_400), now, 14.0), 0.01)
        assertTrue(GraphRanking.recencyScore(now.minusSeconds(365L * 86_400), now, 14.0) < 0.01)
    }

    @Test
    fun `log-normalized count is monotonic and clamped to 1`() {
        assertEquals(0.0, GraphRanking.logNormalized(0), 0.0001)
        assertTrue(GraphRanking.logNormalized(5) < GraphRanking.logNormalized(20))
        assertTrue(GraphRanking.logNormalized(10_000) <= 1.0)
    }

    @Test
    fun `composite importance always stays within zero and one`() {
        val score = GraphRanking.compositeImportance(
            degreeCentrality = 1.0, pageRank = 1.0, recency = 1.0, referenceCount = 1_000_000, frequency = 1_000_000
        )
        assertTrue(score in 0.0..1.0)
    }
}
