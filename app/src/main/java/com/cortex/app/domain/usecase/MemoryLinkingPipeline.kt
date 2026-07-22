package com.cortex.app.domain.usecase

import com.cortex.app.data.graph.GraphBuilder
import com.cortex.app.data.graph.GraphLinkResult
import com.cortex.app.data.graph.GraphUpdater
import com.cortex.app.domain.model.MemoryItem
import com.cortex.app.domain.model.MemorySourceType

data class MemoryLinkingResult(val memory: MemoryItem, val graphLink: GraphLinkResult)

/**
 * The real end-to-end capture flow: [CaptureTextMemoryUseCase] (normalize/classify/
 * score/persist) followed by [GraphBuilder] (extract/dedupe/link) followed by a ranking
 * refresh. This is what the UI should call now, not the bare capture use case —
 * MemoryEngine.md's pipeline doesn't stop at "saved to Room," it continues through graph
 * linking automatically for every capture.
 *
 * The ranking refresh runs synchronously and whole-graph, same tradeoff noted on
 * [GraphUpdater]: fine at current scale, a candidate to move to a background/debounced
 * job once graphs get large. Not deferred here silently — see docs/PHASE2_STATUS.md.
 */
class MemoryLinkingPipeline(
    private val captureTextMemory: CaptureTextMemoryUseCase,
    private val graphBuilder: GraphBuilder,
    private val graphUpdater: GraphUpdater
) {
    suspend operator fun invoke(
        rawText: String,
        sourceType: MemorySourceType = MemorySourceType.TEXT,
        sourceRef: String? = null
    ): MemoryLinkingResult {
        val memory = captureTextMemory(rawText, sourceType, sourceRef)
        val graphLink = graphBuilder.linkMemory(memory)
        graphUpdater.recomputeAllImportanceScores()
        return MemoryLinkingResult(memory, graphLink)
    }
}
