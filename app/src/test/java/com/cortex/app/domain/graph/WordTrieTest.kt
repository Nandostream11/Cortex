package com.cortex.app.domain.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordTrieTest {

    @Test
    fun `matches a single-word term`() {
        val trie = WordTrie<String>()
        trie.insert("python", "lang")
        val hits = trie.findAll(listOf("i", "love", "python", "scripting"))
        assertEquals(1, hits.size)
        assertEquals(2 to "lang", hits[0].first to hits[0].third)
    }

    @Test
    fun `prefers the longest matching multi-word term`() {
        val trie = WordTrie<String>()
        trie.insert("point", "short")
        trie.insert("point cloud library", "long")
        val hits = trie.findAll(listOf("using", "point", "cloud", "library", "today"))
        assertEquals(1, hits.size)
        assertEquals(3, hits[0].second) // matched 3 tokens, not 1
        assertEquals("long", hits[0].third)
    }

    @Test
    fun `matching is case-insensitive`() {
        val trie = WordTrie<String>()
        trie.insert("Jetson Orin", "hw")
        val hits = trie.findAll(listOf("the", "JETSON", "ORIN", "board"))
        assertEquals(1, hits.size)
    }

    @Test
    fun `no match returns an empty list`() {
        val trie = WordTrie<String>()
        trie.insert("kubernetes", "tech")
        assertTrue(trie.findAll(listOf("hello", "world")).isEmpty())
    }

    @Test
    fun `adjacent non-overlapping matches are both found`() {
        val trie = WordTrie<String>()
        trie.insert("ros", "framework")
        trie.insert("python", "lang")
        val hits = trie.findAll(listOf("ros", "python"))
        assertEquals(2, hits.size)
    }
}
