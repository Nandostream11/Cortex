package com.cortex.app.ui.screens.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cortex.app.domain.graph.Point2D
import com.cortex.app.domain.model.Node
import com.cortex.app.domain.model.NodeType
import com.cortex.app.ui.screens.placeholder.PlaceholderScreen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

private fun colorForNodeType(type: NodeType): Color = when (type) {
    NodeType.MEMORY -> Color(0xFF6C7BFF)
    NodeType.CONCEPT -> Color(0xFF9B8CFF)
    NodeType.PROJECT -> Color(0xFF4FC3F7)
    NodeType.TASK -> Color(0xFF66BB6A)
    NodeType.PERSON -> Color(0xFFFFB74D)
    NodeType.TOOL -> Color(0xFF4DB6AC)
    NodeType.PAPER -> Color(0xFFBA68C8)
    NodeType.BUG -> Color(0xFFE57373)
    NodeType.GOAL -> Color(0xFFFFD54F)
    NodeType.CONNECTOR_SOURCE -> Color(0xFF90A4AE)
}

@Composable
fun GraphScreen(
    viewModel: GraphViewModel,
    onOpenRelatedMemories: (nodeId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.nodes.isEmpty() -> PlaceholderScreen(
                title = "Nothing to show yet",
                subtitle = "Your graph fills in automatically as you capture memories.",
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                val matchedNodeIds = remember(state.searchQuery, state.nodes) {
                    if (state.searchQuery.isBlank()) emptySet()
                    else state.nodes.filter { it.label.contains(state.searchQuery, ignoreCase = true) }.map { it.id }.toSet()
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .pointerInput(state.nodes, state.positions) {
                            coroutineScope {
                                launch {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.3f, 4f)
                                        offset += pan
                                    }
                                }
                                launch {
                                    detectTapGestures { tapPosition ->
                                        val graphPoint = screenToGraph(tapPosition, scale, offset)
                                        val nearest = nearestNode(graphPoint, state.nodes, state.positions, maxDistance = 40f / scale)
                                        if (nearest != null) viewModel.onNodeTapped(nearest.id) else viewModel.clearSelection()
                                    }
                                }
                            }
                        }
                ) {
                    // Edges first, so nodes draw on top of the lines touching them.
                    for (edge in state.edges) {
                        val from = state.positions[edge.fromNodeId] ?: continue
                        val to = state.positions[edge.toNodeId] ?: continue
                        val isHighlighted = state.selectedNodeId != null &&
                            (edge.fromNodeId == state.selectedNodeId || edge.toNodeId == state.selectedNodeId)
                        drawLine(
                            color = if (isHighlighted) Color(0xFF6C7BFF) else Color(0x33FFFFFF),
                            start = graphToScreen(from, scale, offset),
                            end = graphToScreen(to, scale, offset),
                            strokeWidth = if (isHighlighted) 2.5f else 1f
                        )
                    }

                    for (node in state.nodes) {
                        val pos = state.positions[node.id] ?: continue
                        val screenPos = graphToScreen(pos, scale, offset)
                        val isSelected = node.id == state.selectedNodeId
                        val isNeighbor = node.id in state.neighborIds
                        val isSearchMatch = node.id in matchedNodeIds
                        val dimmed = state.selectedNodeId != null && !isSelected && !isNeighbor

                        val baseRadius = 8f + (node.importanceScore.toFloat() * 10f)
                        val radius = (baseRadius * scale).coerceIn(4f, 60f)

                        drawCircle(
                            color = colorForNodeType(node.type).let { if (dimmed) it.copy(alpha = 0.25f) else it },
                            radius = radius,
                            center = screenPos
                        )
                        if (isSelected || isSearchMatch) {
                            drawCircle(
                                color = Color.White,
                                radius = radius + 4f,
                                center = screenPos,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                GraphSearchBar(
                    query = state.searchQuery,
                    onQueryChanged = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                )

                state.selectedNodeId?.let { selectedId ->
                    val node = state.nodes.firstOrNull { it.id == selectedId }
                    if (node != null) {
                        SelectedNodePanel(
                            node = node,
                            neighborCount = state.neighborIds.size,
                            onOpenRelatedMemories = { onOpenRelatedMemories(node.id) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphSearchBar(query: String, onQueryChanged: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier,
        placeholder = { Text("Find a node…") },
        singleLine = true
    )
}

@Composable
private fun SelectedNodePanel(
    node: Node,
    neighborCount: Int,
    onOpenRelatedMemories: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = node.label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${node.type.name}${node.subtype?.let { " · $it" } ?: ""} · $neighborCount connection${if (neighborCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (node.type == NodeType.MEMORY) {
                Text(
                    text = "View related memories",
                    style = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .pointerInput(Unit) { detectTapGestures { onOpenRelatedMemories() } }
                )
            }
        }
    }
}

// --- coordinate transforms -------------------------------------------------------

private fun graphToScreen(point: Point2D, scale: Float, offset: Offset): Offset =
    Offset(point.x.toFloat() * scale + offset.x, point.y.toFloat() * scale + offset.y)

private fun screenToGraph(screenPoint: Offset, scale: Float, offset: Offset): Point2D =
    Point2D(
        ((screenPoint.x - offset.x) / scale).toDouble(),
        ((screenPoint.y - offset.y) / scale).toDouble()
    )

private fun nearestNode(
    graphPoint: Point2D,
    nodes: List<Node>,
    positions: Map<String, Point2D>,
    maxDistance: Float
): Node? {
    var best: Node? = null
    var bestDist = Double.MAX_VALUE
    for (node in nodes) {
        val pos = positions[node.id] ?: continue
        val dx = pos.x - graphPoint.x
        val dy = pos.y - graphPoint.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        if (dist < bestDist) {
            bestDist = dist
            best = node
        }
    }
    return if (bestDist <= max(maxDistance, 1f)) best else null
}
