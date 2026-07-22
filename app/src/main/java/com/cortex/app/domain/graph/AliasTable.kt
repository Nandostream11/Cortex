package com.cortex.app.domain.graph

/**
 * Known aliases that string similarity alone can't catch — "k8s" and "kubernetes" share
 * no characters in common in a way Levenshtein would credit, but they're the same node.
 * GraphEngine.md Step 5 calls this out explicitly as its own mechanism, separate from
 * string similarity. Seed list, meant to grow over time (including from user
 * corrections eventually — not implemented yet, tracked in PHASE2_STATUS.md).
 */
object AliasTable {

    private val aliases: Map<String, String> = mapOf(
        "js" to "javascript",
        "ts" to "typescript",
        "k8s" to "kubernetes",
        "pcl" to "point cloud library",
        "cv" to "opencv",
        "opencv2" to "opencv",
        "ros2" to "ros 2",
        "py" to "python",
        "rpi" to "raspberry pi",
        "raspi" to "raspberry pi",
        "nn" to "neural network",
        "cnn" to "convolutional neural network",
        "rl" to "reinforcement learning",
        "slam" to "simultaneous localization and mapping",
        "llm" to "large language model",
        "api" to "application programming interface",
        "vscode" to "visual studio code",
        "gh" to "github"
    )

    /** Resolves a known alias to its canonical form; returns [value] unchanged if none is known. */
    fun resolve(value: String): String = aliases[value.trim().lowercase()] ?: value
}
