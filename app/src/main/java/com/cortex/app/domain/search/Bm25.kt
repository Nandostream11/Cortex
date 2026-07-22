package com.cortex.app.domain.search

import kotlin.math.ln

/** One document in the BM25 corpus: an id to return and its tokenized text. */
data class Bm25Document(val id: String, val tokens: List<String>)

data class Bm25Match(val id: String, val score: Double)

/**
 * Standard Okapi BM25 (MemoryEngine.md / Phase 2 Step 8: "BM25... No semantic AI search
 * yet"). Built once per corpus snapshot — term frequencies, document frequencies, and
 * average document length are all precomputed at construction, so repeated queries
 * against the same corpus don't redo that work. [com.cortex.app.data.search.SearchEngine]
 * rebuilds this from Room whenever the memory set changes meaningfully.
 */
class Bm25Index(documents: List<Bm25Document>, private val k1: Double = 1.5, private val b: Double = 0.75) {

    private val docCount = documents.size
    private val docLengths: Map<String, Int> = documents.associate { it.id to it.tokens.size }
    private val avgDocLength: Double =
        if (documents.isEmpty()) 0.0 else documents.sumOf { it.tokens.size }.toDouble() / documents.size

    /** term -> (docId -> term frequency in that doc) */
    private val termFrequencies: Map<String, Map<String, Int>> = run {
        val map = HashMap<String, HashMap<String, Int>>()
        for (doc in documents) {
            val counts = HashMap<String, Int>()
            for (token in doc.tokens) counts[token] = (counts[token] ?: 0) + 1
            for ((term, count) in counts) {
                map.getOrPut(term) { HashMap() }[doc.id] = count
            }
        }
        map
    }

    /** term -> IDF, precomputed once (Robertson-Sparck Jones with a +1 floor so common terms never go negative). */
    private val idf: Map<String, Double> = termFrequencies.mapValues { (_, postings) ->
        val n = postings.size
        ln((docCount - n + 0.5) / (n + 0.5) + 1.0)
    }

    fun search(queryTokens: List<String>, limit: Int = 50): List<Bm25Match> {
        if (docCount == 0 || queryTokens.isEmpty()) return emptyList()

        val scores = HashMap<String, Double>()
        for (term in queryTokens.distinct()) {
            val termIdf = idf[term] ?: continue // unseen term contributes nothing, not an error
            val postings = termFrequencies[term] ?: continue
            for ((docId, freq) in postings) {
                val docLen = docLengths[docId] ?: continue
                val denom = freq + k1 * (1 - b + b * (docLen / avgDocLength))
                val termScore = termIdf * (freq * (k1 + 1)) / denom
                scores[docId] = (scores[docId] ?: 0.0) + termScore
            }
        }
        return scores.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { Bm25Match(it.key, it.value) }
    }

    companion object {
        private val stopwords = setOf(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been",
            "to", "of", "in", "on", "at", "for", "with", "as", "by", "this", "that", "it",
            "i", "you", "he", "she", "we", "they", "from", "not", "so", "if", "then"
        )

        /** Lowercased word tokens with basic stopwords removed. Kept minimal on purpose — no stemming. */
        fun tokenize(text: String): List<String> =
            Regex("[\\p{L}0-9]+").findAll(text)
                .map { it.value.lowercase() }
                .filter { it.length > 1 && it !in stopwords }
                .toList()
    }
}
