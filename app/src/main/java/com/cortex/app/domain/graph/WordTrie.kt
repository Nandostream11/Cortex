package com.cortex.app.domain.graph

/**
 * Trie keyed on whitespace-tokenized, lowercased words rather than characters, so
 * multi-word dictionary terms ("point cloud library", "jetson orin nano") match as a
 * single unit instead of requiring one regex alternative per term. Used by
 * [EntityExtractor] for the dictionary-based entity kinds (Step 3's "Use: ... Trie,
 * Dictionaries").
 */
class WordTrie<V> {
    private class Node<V> {
        val children = HashMap<String, Node<V>>()
        var value: V? = null
    }

    private val root = Node<V>()

    fun insert(term: String, value: V) {
        val tokens = tokenize(term)
        if (tokens.isEmpty()) return
        var node = root
        for (token in tokens) {
            node = node.children.getOrPut(token) { Node() }
        }
        node.value = value
    }

    /**
     * Scans [tokens] left to right; at each start position, greedily matches the
     * *longest* dictionary term beginning there (so "point cloud library" wins over a
     * hypothetical standalone "point" entry). Returns matches as (startTokenIndex,
     * lengthInTokens, value). [tokens] need not be pre-lowercased — matching is
     * case-insensitive, mirroring [insert].
     */
    fun findAll(tokens: List<String>): List<Triple<Int, Int, V>> {
        val lowered = tokens.map { it.lowercase() }
        val results = mutableListOf<Triple<Int, Int, V>>()
        var i = 0
        while (i < lowered.size) {
            var node = root
            var lastMatch: Pair<Int, V>? = null
            var j = i
            while (j < lowered.size) {
                val next = node.children[lowered[j]] ?: break
                node = next
                j++
                node.value?.let { lastMatch = (j - i) to it }
            }
            if (lastMatch != null) {
                results.add(Triple(i, lastMatch!!.first, lastMatch!!.second))
                i += lastMatch!!.first
            } else {
                i++
            }
        }
        return results
    }

    private fun tokenize(term: String): List<String> =
        term.lowercase().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    companion object {
        fun tokenizeText(text: String): List<String> =
            // Keeps `+`, `#`, `.` inside tokens (C++, C#, kotlinx.coroutines) rather than
            // splitting on them, since those are meaningful in tech-term dictionaries.
            Regex("[\\p{L}0-9+#.]+").findAll(text).map { it.value.lowercase() }.toList()
    }
}
