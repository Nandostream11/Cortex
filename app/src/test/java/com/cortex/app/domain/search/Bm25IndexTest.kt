package com.cortex.app.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Bm25IndexTest {

    private val docs = listOf(
        Bm25Document("d1", Bm25Index.tokenize("fix the null pointer exception in the graph module")),
        Bm25Document("d2", Bm25Index.tokenize("meeting notes about the roadmap and release plan")),
        Bm25Document("d3", Bm25Index.tokenize("graph module refactor: extract the entity extraction pipeline")),
        Bm25Document("d4", Bm25Index.tokenize("buy groceries: milk, eggs, bread"))
    )
    private val index = Bm25Index(docs)

    @Test
    fun `relevant documents are returned and ranked above irrelevant ones`() {
        val results = index.search(Bm25Index.tokenize("graph module"))
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].id in setOf("d1", "d3"))
        assertTrue(results.none { it.id == "d4" })
    }

    @Test
    fun `an empty query returns no results`() {
        assertTrue(index.search(emptyList()).isEmpty())
    }

    @Test
    fun `a query with no matching terms returns no results`() {
        assertTrue(index.search(Bm25Index.tokenize("quantum entanglement")).isEmpty())
    }

    @Test
    fun `higher term frequency ranks a document first`() {
        val repIndex = Bm25Index(
            listOf(
                Bm25Document("r1", Bm25Index.tokenize("kotlin kotlin kotlin coroutines")),
                Bm25Document("r2", Bm25Index.tokenize("python scripting"))
            )
        )
        val results = repIndex.search(Bm25Index.tokenize("kotlin"))
        assertEquals("r1", results.first().id)
    }

    @Test
    fun `tokenize lowercases and strips common stopwords`() {
        val tokens = Bm25Index.tokenize("The Graph Engine is a work in progress")
        assertTrue("the" !in tokens)
        assertTrue("is" !in tokens)
        assertTrue("graph" in tokens)
        assertTrue("engine" in tokens)
    }

    @Test
    fun `an empty corpus never throws and returns no results`() {
        val empty = Bm25Index(emptyList())
        assertTrue(empty.search(Bm25Index.tokenize("anything")).isEmpty())
    }
}
