package com.cortex.app.ui.screens.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cortex.app.data.graph.GraphQueryEngine
import com.cortex.app.data.graph.GraphRepository
import com.cortex.app.data.graph.GraphStatistics
import com.cortex.app.data.graph.GraphStats
import com.cortex.app.domain.graph.GraphLayout
import com.cortex.app.domain.graph.Point2D
import com.cortex.app.domain.graph.RankingEdge
import com.cortex.app.domain.model.Edge
import com.cortex.app.domain.model.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GraphUiState(
    val isLoading: Boolean = true,
    val nodes: List<Node> = emptyList(),
    val edges: List<Edge> = emptyList(),
    val positions: Map<String, Point2D> = emptyMap(),
    val selectedNodeId: String? = null,
    val neighborIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val stats: GraphStats? = null
)

class GraphViewModel(
    private val graphRepository: GraphRepository,
    private val graphQueryEngine: GraphQueryEngine,
    private val graphStatistics: GraphStatistics
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState

    init {
        loadGraph()
    }

    fun loadGraph() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val allNodes = graphRepository.getAllNodes()
            val allEdges = graphRepository.getAllEdges()
            val stats = graphStatistics.compute()

            // Force-directed layout is O(n^2) per iteration -- cap what actually gets
            // laid out and drawn to the most important nodes so this stays smooth on a
            // phone as a graph grows. Documented tradeoff, same spirit as GraphUpdater's.
            val nodesForLayout = if (allNodes.size > MAX_RENDERED_NODES) {
                allNodes.sortedByDescending { it.importanceScore }.take(MAX_RENDERED_NODES)
            } else {
                allNodes
            }
            val renderedIds = nodesForLayout.map { it.id }.toSet()
            val renderedEdges = allEdges.filter { it.fromNodeId in renderedIds && it.toNodeId in renderedIds }

            val positions = withContext(Dispatchers.Default) {
                GraphLayout.forceDirectedLayout(
                    nodesForLayout.map { it.id },
                    renderedEdges.map { RankingEdge(it.fromNodeId, it.toNodeId, it.weight) }
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    nodes = nodesForLayout,
                    edges = renderedEdges,
                    positions = positions,
                    stats = stats
                )
            }
        }
    }

    fun onNodeTapped(nodeId: String) {
        if (_uiState.value.selectedNodeId == nodeId) {
            clearSelection()
            return
        }
        viewModelScope.launch {
            val neighborIds = graphQueryEngine.neighbors(nodeId).map { it.id }.toSet()
            _uiState.update { it.copy(selectedNodeId = nodeId, neighborIds = neighborIds) }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedNodeId = null, neighborIds = emptySet()) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private companion object {
        const val MAX_RENDERED_NODES = 150
    }
}
