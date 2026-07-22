package com.cortex.app.domain.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EntityExtractorTest {

    private val extractor = EntityExtractor()
    private val referenceTime = Instant.parse("2026-07-21T10:00:00Z")

    private val sample = """
        Task: fix the null pointer exception in the Cortex project graph module.
        Getting a java.lang.NullPointerException on startup, see error CORTEX-142.
        Repro: run `./gradlew assembleDebug` on branch fix/graph-npe, commit a1b2c3d.
        Talked with Sarah Connor about switching from ROS to ROS2 for the SLAM stack on
        the Jetson Orin. Also using OpenCV and kotlinx.coroutines 1.11.0.
        Paper worth reading: Smith et al. (2023), see also arxiv:2401.01234.
        Meeting tomorrow at 3:30pm to review /home/user/projects/cortex/README.md.
        Repo: https://github.com/Nandostream11/Cortex
        Goal: ship the graph engine by 2026-08-01.
    """.trimIndent()

    private fun has(entities: List<ExtractedEntity>, kind: EntityKind, predicate: (String) -> Boolean) =
        entities.any { it.kind == kind && predicate(it.normalizedValue) }

    @Test
    fun `extracts a bug id`() {
        val entities = extractor.extract(sample, referenceTime)
        assertTrue(has(entities, EntityKind.BUG_ID) { it == "CORTEX-142" })
    }

    @Test
    fun `extracts a backtick-quoted command`() {
        val entities = extractor.extract(sample, referenceTime)
        assertTrue(has(entities, EntityKind.COMMAND) { it.contains("gradlew assembleDebug") })
    }

    @Test
    fun `extracts branch name and git commit`() {
        val entities = extractor.extract(sample, referenceTime)
        assertTrue(has(entities, EntityKind.BRANCH_NAME) { it == "fix/graph-npe" })
        assertTrue(has(entities, EntityKind.GIT_COMMIT) { it == "a1b2c3d" })
    }

    @Test
    fun `extracts a person from a cue phrase`() {
        val entities = extractor.extract(sample, referenceTime)
        assertTrue(has(entities, EntityKind.PERSON) { it.contains("Sarah Connor") })
    }

    @Test
    fun `extracts multi-word and single-word dictionary terms via the trie`() {
        val entities = extractor.extract(sample, referenceTime)
        assertTrue("ROS as framework", has(entities, EntityKind.FRAMEWORK) { it == "ros" })
        assertTrue("ROS2 as framework", has(entities, EntityKind.FRAMEWORK) { it == "ros2" })
        assertTrue("Jetson Orin as hardware (multi-word)", has(entities, EntityKind.HARDWARE) { it == "jetson orin" })
        assertTrue("kotlinx.coroutines as library", has(entities, EntityKind.LIBRARY) { it == "kotlinx.coroutines" })
    }

    @Test
    fun `a dictionary term followed by a sentence period is not corrupted`() {
        // Regression test: "Jetson Orin." at end of sentence previously failed to match
        // because the tokenizer kept the trailing period as part of the last token.
        val entities = extractor.extract("We're deploying on the Jetson Orin.", referenceTime)
        assertTrue(has(entities, EntityKind.HARDWARE) { it == "jetson orin" })
    }

    @Test
    fun `resolves relative dates against the reference time`() {
        val entities = extractor.extract(sample, referenceTime)
        assertTrue(has(entities, EntityKind.DATE) { it == "2026-07-22" }) // "tomorrow"
        assertTrue(has(entities, EntityKind.DATE) { it == "2026-08-01" }) // ISO date
    }

    @Test
    fun `extracts research paper citations in two different formats`() {
        val entities = extractor.extract(sample, referenceTime)
        assertTrue(has(entities, EntityKind.RESEARCH_PAPER) { it.contains("Smith et al") })
        assertTrue(has(entities, EntityKind.RESEARCH_PAPER) { it == "arxiv:2401.01234" })
    }

    @Test
    fun `a github URL is classified as REPOSITORY, not a duplicate URL`() {
        val entities = extractor.extract(sample, referenceTime)
        assertTrue(has(entities, EntityKind.REPOSITORY) { it.contains("github.com/nandostream11/cortex") })
        val githubSpans = entities.count { it.rawValue.contains("github.com", ignoreCase = true) }
        assertEquals("the github URL span should be classified exactly once", 1, githubSpans)
    }

    @Test
    fun `a file path with a trailing sentence period is not corrupted`() {
        val entities = extractor.extract(sample, referenceTime)
        assertTrue(has(entities, EntityKind.FILE_PATH) { it == "/home/user/projects/cortex/README.md" })
    }

    @Test
    fun `structural spans (TASK, GOAL, ERROR_MESSAGE) survive alongside point entities nested inside them`() {
        val entities = extractor.extract(sample, referenceTime)
        assertTrue("GOAL should survive despite containing a DATE", has(entities, EntityKind.GOAL) { it.contains("ship the graph engine") })
        assertTrue("the DATE nested inside the GOAL should also be extracted", has(entities, EntityKind.DATE) { it == "2026-08-01" })
        assertTrue("ERROR_MESSAGE should survive despite containing a BUG_ID", has(entities, EntityKind.ERROR_MESSAGE) { it.contains("NullPointerException") })
        assertTrue("the BUG_ID nested inside the ERROR_MESSAGE should also be extracted", has(entities, EntityKind.BUG_ID) { it == "CORTEX-142" })
        assertTrue(has(entities, EntityKind.TASK) { it.contains("fix the null pointer exception") })
    }

    @Test
    fun `blank text yields no entities`() {
        assertTrue(extractor.extract("   ", referenceTime).isEmpty())
    }
}
