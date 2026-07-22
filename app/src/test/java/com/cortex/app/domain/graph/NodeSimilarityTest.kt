package com.cortex.app.domain.graph

import com.cortex.app.domain.model.NodeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeSimilarityTest {

    @Test
    fun `identical strings have similarity 1`() {
        assertEquals(1.0, NodeSimilarity.combinedSimilarity("ROS2", "ROS2"), 0.0001)
    }

    @Test
    fun `character-level variants score highly via levenshtein`() {
        assertTrue(NodeSimilarity.levenshteinRatio("ROS2", "ROS 2") > 0.7)
    }

    @Test
    fun `token overlap catches reordered or partial phrases`() {
        assertTrue(NodeSimilarity.jaccardTokenSimilarity("Point Cloud Library", "Cloud Library") > 0.5)
    }

    @Test
    fun `unrelated strings score low`() {
        assertTrue(NodeSimilarity.combinedSimilarity("TensorFlow", "Kubernetes") < 0.3)
    }

    @Test
    fun `two short names differing by one whole token do not score as similar`() {
        // Regression test: raw whole-string Levenshtein previously scored "Project A"
        // vs "Project B" at 0.89 (a single-character edit in a 9-character string),
        // which was high enough to wrongly merge two clearly different projects.
        assertTrue(NodeSimilarity.combinedSimilarity("Project A", "Project B") < 0.82)
    }

    @Test
    fun `a real single-word typo within a longer phrase still scores highly`() {
        // The behavior the regression fix must not break: token-wise matching should
        // still catch a typo inside one word of a multi-word name.
        assertTrue(NodeSimilarity.combinedSimilarity("Raspberry Pi", "Rasberry Pi") >= 0.82)
    }
}

class NodeDeduplicatorTest {

    private val existing = listOf(
        ExistingNodeRef("n1", NodeType.TOOL, "ROS 2"),
        ExistingNodeRef("n2", NodeType.PERSON, "Sarah Connor"),
        ExistingNodeRef("n3", NodeType.TOOL, "point cloud library")
    )
    private val dedup = NodeDeduplicator()

    @Test
    fun `alias table resolves an abbreviation to an existing node`() {
        val result = dedup.resolve("ROS2", NodeType.TOOL, existing)
        assertEquals("n1", result.matchedNodeId)
    }

    @Test
    fun `alias table resolves PCL to point cloud library`() {
        val result = dedup.resolve("PCL", NodeType.TOOL, existing)
        assertEquals("n3", result.matchedNodeId)
    }

    @Test
    fun `an unrelated new term does not match anything`() {
        val result = dedup.resolve("TensorFlow", NodeType.TOOL, existing)
        assertNull(result.matchedNodeId)
    }

    @Test
    fun `same name but a different node type never merges`() {
        val result = dedup.resolve("Sarah Connor", NodeType.PROJECT, existing)
        assertNull(result.matchedNodeId)
    }

    @Test
    fun `an exact case-insensitive match returns similarity 1`() {
        val result = dedup.resolve("ros 2", NodeType.TOOL, existing)
        assertEquals("n1", result.matchedNodeId)
        assertEquals(1.0, result.similarity, 0.0001)
    }
}
