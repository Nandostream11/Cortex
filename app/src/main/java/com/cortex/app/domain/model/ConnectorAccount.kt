package com.cortex.app.domain.model

import java.time.Instant

/**
 * Represents a configured connector. The actual credential is never held here —
 * it lives only in [com.cortex.app.security.SecretStore], keyed by [id]. This object
 * is what gets persisted, displayed, and exported.
 */
data class ConnectorAccount(
    val id: String,
    val name: String,
    val type: String,
    val enabled: Boolean,
    val authMode: ConnectorAuthMode,
    val scopes: List<String>,
    val lastSyncAt: Instant?,
    val lastError: String?
)

enum class ConnectorAuthMode {
    API_KEY,
    OAUTH,
    PAT,
    CUSTOM_TOKEN,
    MANUAL_IMPORT
}
