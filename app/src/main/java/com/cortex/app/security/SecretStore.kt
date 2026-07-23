package com.cortex.app.security

/**
 * The only place raw secrets (connector tokens, the OpenRouter API key, etc.) are
 * allowed to live. Backed by Android Keystore via [AndroidSecretStore] in production.
 * Per Security.md: never hardcode, never log, never let these leak into Room, exports,
 * or crash reports.
 */
interface SecretStore {
    fun put(key: String, value: String)
    fun get(key: String): String?
    fun remove(key: String)
    fun contains(key: String): Boolean

    companion object {
        // Well-known keys. Connector secrets use "connector:<connectorId>" as the key.
        const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        const val KEY_DATABASE_PASSPHRASE = "database_passphrase"
        fun connectorKey(connectorId: String) = "connector:$connectorId"
    }
}
