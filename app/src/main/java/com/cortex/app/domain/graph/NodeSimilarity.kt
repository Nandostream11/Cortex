package com.cortex.app.domain.graph

/**
 * Pure string-similarity functions for node deduplication (GraphEngine.md Step 5:
 * "String similarity, Token overlap, Normalized names"). No AI/embeddings — see
 * NodeDeduplicator for how these combine with an alias table.
 */
object NodeSimilarity {

    /** Token-set overlap: |A ∩ B| / |A ∪ B|, on lowercased whitespace-separated tokens. */
    fun jaccardTokenSimilarity(a: String, b: String): Double {
        val tokensA = tokenize(a)
        val tokensB = tokenize(b)
        if (tokensA.isEmpty() && tokensB.isEmpty()) return 1.0
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0
        val intersection = tokensA.intersect(tokensB).size
        val union = tokensA.union(tokensB).size
        return intersection.toDouble() / union.toDouble()
    }

    /** 1.0 - (Levenshtein edit distance / length of the longer string). 1.0 = identical. */
    fun levenshteinRatio(a: String, b: String): Double {
        val x = a.lowercase().trim()
        val y = b.lowercase().trim()
        if (x == y) return 1.0
        val maxLen = maxOf(x.length, y.length)
        if (maxLen == 0) return 1.0
        return 1.0 - (levenshteinDistance(x, y).toDouble() / maxLen.toDouble())
    }

    /**
     * The more forgiving of the two token-level signals: catches both loose word-order/
     * subset variants ("Point Cloud Library" vs "Cloud Library Point") and loose
     * per-word typos ("Raspberry Pi" vs "Rasberry Pi"). Deliberately does NOT fall back
     * to raw whole-string [levenshteinRatio] here — that over-credits short strings
     * where one entire token differs: "Project A" vs "Project B" scores 0.89 on raw
     * Levenshtein (a 1-character edit in a 9-character string) despite being two
     * clearly different projects. [tokenwiseSimilarity] catches the same real typos
     * without that false-positive, by scoring token-to-token rather than
     * character-to-character across the whole phrase. [levenshteinRatio] remains
     * public for callers that genuinely want raw whole-string comparison.
     */
    fun combinedSimilarity(a: String, b: String): Double =
        maxOf(jaccardTokenSimilarity(a, b), tokenwiseSimilarity(a, b))

    /**
     * Greedily matches each token in the shorter string to its best-scoring
     * (Levenshtein-ratio) counterpart in the longer string, without reuse, then
     * averages matched-pair scores over the *longer* string's token count — so leftover
     * unmatched tokens (a length mismatch) pull the score down rather than being
     * ignored. This is what lets a same-length single-word typo ("Rasberry" vs
     * "Raspberry") score highly while a same-length but fully-different final token
     * ("A" vs "B") scores near zero for that pair, instead of both being judged only by
     * how short the surrounding phrase happens to be.
     */
    fun tokenwiseSimilarity(a: String, b: String): Double {
        val tokensA = tokenize(a).toList()
        val tokensB = tokenize(b).toList()
        if (tokensA.isEmpty() && tokensB.isEmpty()) return 1.0
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0

        val (shorter, longer) = if (tokensA.size <= tokensB.size) tokensA to tokensB else tokensB to tokensA
        val remaining = longer.toMutableList()
        var totalScore = 0.0

        for (token in shorter) {
            if (remaining.isEmpty()) break
            var bestIndex = 0
            var bestScore = -1.0
            for (i in remaining.indices) {
                val score = levenshteinRatio(token, remaining[i])
                if (score > bestScore) {
                    bestScore = score
                    bestIndex = i
                }
            }
            totalScore += bestScore
            remaining.removeAt(bestIndex)
        }
        // Divide by the longer list's size, not the shorter's: an unmatched leftover
        // token (a real length mismatch between the two names) should cost something,
        // not be silently dropped from the average.
        return totalScore / longer.size
    }

    private fun tokenize(s: String): Set<String> =
        s.lowercase().split(Regex("[^\\p{L}0-9]+")).filter { it.isNotEmpty() }.toSet()

    private fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previousRow = IntArray(b.length + 1) { it }
        var currentRow = IntArray(b.length + 1)

        for (i in 1..a.length) {
            currentRow[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                currentRow[j] = minOf(
                    currentRow[j - 1] + 1,      // insertion
                    previousRow[j] + 1,         // deletion
                    previousRow[j - 1] + cost   // substitution
                )
            }
            val tmp = previousRow
            previousRow = currentRow
            currentRow = tmp
        }
        return previousRow[b.length]
    }
}
