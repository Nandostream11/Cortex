package com.cortex.app.di

import android.content.Context
import com.cortex.app.data.db.CortexDatabase
import com.cortex.app.data.export.ExportManager
import com.cortex.app.data.graph.GraphBuilder
import com.cortex.app.data.graph.GraphQueryEngine
import com.cortex.app.data.graph.GraphRepository
import com.cortex.app.data.graph.GraphRepositoryImpl
import com.cortex.app.data.graph.GraphStatistics
import com.cortex.app.data.graph.GraphUpdater
import com.cortex.app.data.repository.MemoryRepository
import com.cortex.app.data.repository.MemoryRepositoryImpl
import com.cortex.app.data.search.SearchEngine
import com.cortex.app.data.settings.SettingsRepository
import com.cortex.app.domain.usecase.CaptureTextMemoryUseCase
import com.cortex.app.domain.usecase.MemoryLinkingPipeline
import com.cortex.app.security.AndroidSecretStore
import com.cortex.app.security.DatabasePassphraseProvider
import com.cortex.app.security.SecretStore

/**
 * Single composition root for the app. Every dependency is constructed here and handed
 * down; nothing reaches for a global singleton on its own. This keeps every subsystem
 * constructor-testable per CodingStandards.md without needing an annotation-processing
 * DI framework for a project this size yet.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val secretStore: SecretStore by lazy { AndroidSecretStore(appContext) }

    private val databasePassphraseProvider: DatabasePassphraseProvider by lazy {
        DatabasePassphraseProvider(secretStore)
    }

    val database: CortexDatabase by lazy { CortexDatabase.getInstance(appContext, databasePassphraseProvider) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val memoryRepository: MemoryRepository by lazy { MemoryRepositoryImpl(database.memoryDao()) }

    val exportManager: ExportManager by lazy { ExportManager(database, settingsRepository) }

    val captureTextMemoryUseCase: CaptureTextMemoryUseCase by lazy {
        CaptureTextMemoryUseCase(memoryRepository)
    }

    // --- Phase 2: graph engine ---

    val graphRepository: GraphRepository by lazy {
        GraphRepositoryImpl(database.nodeDao(), database.edgeDao())
    }

    val graphBuilder: GraphBuilder by lazy { GraphBuilder(graphRepository) }

    val graphUpdater: GraphUpdater by lazy { GraphUpdater(graphRepository) }

    val graphQueryEngine: GraphQueryEngine by lazy { GraphQueryEngine(graphRepository) }

    val graphStatistics: GraphStatistics by lazy { GraphStatistics(graphRepository) }

    val memoryLinkingPipeline: MemoryLinkingPipeline by lazy {
        MemoryLinkingPipeline(captureTextMemoryUseCase, graphBuilder, graphUpdater)
    }

    val searchEngine: SearchEngine by lazy {
        SearchEngine(memoryRepository, graphRepository, graphQueryEngine)
    }
}
