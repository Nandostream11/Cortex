package com.cortex.app.di

import android.content.Context
import com.cortex.app.data.db.CortexDatabase
import com.cortex.app.data.export.ExportManager
import com.cortex.app.data.repository.MemoryRepository
import com.cortex.app.data.repository.MemoryRepositoryImpl
import com.cortex.app.data.settings.SettingsRepository
import com.cortex.app.domain.usecase.CaptureTextMemoryUseCase
import com.cortex.app.security.AndroidSecretStore
import com.cortex.app.security.SecretStore

/**
 * Single composition root for the app. Every dependency is constructed here and handed
 * down; nothing reaches for a global singleton on its own. This keeps every subsystem
 * constructor-testable per CodingStandards.md without needing an annotation-processing
 * DI framework for a project this size yet.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: CortexDatabase by lazy { CortexDatabase.getInstance(appContext) }

    val secretStore: SecretStore by lazy { AndroidSecretStore(appContext) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val memoryRepository: MemoryRepository by lazy { MemoryRepositoryImpl(database.memoryDao()) }

    val exportManager: ExportManager by lazy { ExportManager(database, settingsRepository) }

    val captureTextMemoryUseCase: CaptureTextMemoryUseCase by lazy {
        CaptureTextMemoryUseCase(memoryRepository)
    }
}
