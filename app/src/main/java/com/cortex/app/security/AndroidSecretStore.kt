package com.cortex.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-backed secret storage. The MasterKey itself never leaves the device's
 * hardware-backed keystore (or software fallback on devices without StrongBox), and
 * EncryptedSharedPreferences encrypts both keys and values with it — see Security.md
 * "Store API keys using Android Keystore-backed encryption or equivalent secure storage."
 *
 * Deliberately has no logging of [value] anywhere, including in exceptions.
 */
class AndroidSecretStore(context: Context) : SecretStore {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun get(key: String): String? = prefs.getString(key, null)

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun contains(key: String): Boolean = prefs.contains(key)

    private companion object {
        const val PREFS_FILE_NAME = "cortex_secrets"
    }
}
