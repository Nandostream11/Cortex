package com.cortex.app.data.settings

import com.cortex.app.domain.model.AutonomyMode

data class AppSettings(
    val autonomyMode: AutonomyMode = AutonomyMode.PASSIVE,
    val openRouterEnabled: Boolean = false,
    val hasCompletedOnboarding: Boolean = false
)
