package com.cortex.app.domain.graph

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Point2D(val x: Double, val y: Double) {
    operator fun plus(other: Point2D) = Point2D(x + other.x, y + other.y)
    operator fun minus(other: Point2D) = Point2D(x - other.x, y - other.y)
    operator fun times(scalar: Double) = Point2D(x * scalar, y * scalar)
    fun length(): Double = sqrt(x * x + y * y)
}

/**
 * Phase 2 Step 9 asks for pan/zoom/tap, not fancy animation — this computes one static
 * layout, not a running physics simulation. Standard Fruchterman-Reingold: nodes repel
 * each other, edges act as springs pulling connected nodes together, both forces cool
 * down over a fixed number of iterations. Deterministic: initial placement is a circle
 * indexed by node order, not random, so the same graph always lays out the same way.
 */
object GraphLayout {

    fun forceDirectedLayout(
        nodeIds: List<String>,
        edges: List<RankingEdge>,
        width: Double = 1000.0,
        height: Double = 1000.0,
        iterations: Int = 150
    ): Map<String, Point2D> {
        if (nodeIds.isEmpty()) return emptyMap()
        if (nodeIds.size == 1) return mapOf(nodeIds[0] to Point2D(width / 2, height / 2))

        val n = nodeIds.size
        val positions = nodeIds.mapIndexed { i, id ->
            val angle = 2 * PI * i / n
            id to Point2D(width / 2 + (width * 0.35) * cos(angle), height / 2 + (height * 0.35) * sin(angle))
        }.toMap().toMutableMap()

        val idealDistance = sqrt((width * height) / n)
        var temperature = width / 10.0

        repeat(iterations) {
            val displacement = nodeIds.associateWith { Point2D(0.0, 0.0) }.toMutableMap()

            for (i in nodeIds.indices) {
                for (j in nodeIds.indices) {
                    if (i == j) continue
                    val a = nodeIds[i]
                    val b = nodeIds[j]
                    val delta = positions.getValue(a) - positions.getValue(b)
                    val dist = delta.length().coerceAtLeast(0.01)
                    val repulsiveForce = (idealDistance * idealDistance) / dist
                    displacement[a] = displacement.getValue(a) + delta * (repulsiveForce / dist)
                }
            }

            for (edge in edges) {
                if (edge.fromId !in positions || edge.toId !in positions || edge.fromId == edge.toId) continue
                val delta = positions.getValue(edge.fromId) - positions.getValue(edge.toId)
                val dist = delta.length().coerceAtLeast(0.01)
                val attractiveForce = (dist * dist) / idealDistance
                val disp = delta * (attractiveForce / dist)
                displacement[edge.fromId] = displacement.getValue(edge.fromId) - disp
                displacement[edge.toId] = displacement.getValue(edge.toId) + disp
            }

            for (id in nodeIds) {
                val disp = displacement.getValue(id)
                val dist = disp.length().coerceAtLeast(0.01)
                val capped = disp * (minOf(dist, temperature) / dist)
                val moved = positions.getValue(id) + capped
                positions[id] = Point2D(
                    moved.x.coerceIn(20.0, width - 20.0),
                    moved.y.coerceIn(20.0, height - 20.0)
                )
            }
            temperature *= 0.95
        }
        return positions
    }
}
