package com.cortex.app.security

import java.security.SecureRandom
import java.util.Base64

/**
 * Generates (once) and retrieves the passphrase used to encrypt the Room/SQLCipher
 * database — Security.md's "encrypt sensitive local files" requirement, applied to the
 * database file itself rather than just [SecretStore]'s own EncryptedSharedPreferences
 * file or [BackupCrypto] exports.
 *
 * This is deliberately a *stored random key*, not a Keystore-native key used directly:
 * SQLCipher's API takes a raw passphrase byte array, not a Keystore `SecretKey` handle,
 * so there's no way to hand SQLCipher a key that never leaves the Keystore's hardware
 * boundary the way [AndroidSecretStore] can for simpler string secrets. The standard,
 * documented mitigation (confirmed via web search this session against current guides)
 * is exactly what's implemented here: generate a strong random passphrase once, and
 * protect *that* with Keystore-backed storage — the same protection [SecretStore]
 * already gives the OpenRouter key and connector tokens. The passphrase is exposed to
 * process memory only for the moment `SupportOpenHelperFactory` uses it to open the
 * database, same as any SQLCipher integration.
 */
class DatabasePassphraseProvider(private val secretStore: SecretStore) {

    private val secureRandom = SecureRandom()

    /** Returns the existing passphrase, or generates, stores, and returns a new one on first run. */
    fun getOrCreatePassphrase(): ByteArray {
        val existing = secretStore.get(SecretStore.KEY_DATABASE_PASSPHRASE)
        if (existing != null) {
            return Base64.getDecoder().decode(existing)
        }

        val newPassphrase = ByteArray(PASSPHRASE_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        secretStore.put(SecretStore.KEY_DATABASE_PASSPHRASE, Base64.getEncoder().encodeToString(newPassphrase))
        return newPassphrase
    }

    private companion object {
        // 256-bit passphrase, matching SQLCipher's default AES-256 cipher.
        const val PASSPHRASE_LENGTH_BYTES = 32
    }
}
