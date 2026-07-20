package com.cortex.app.security

import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts/decrypts export archives with a user-supplied password rather than an
 * Android Keystore key. This is intentional: Keystore keys are device-bound and do not
 * survive reinstall or a device switch, but Security.md requires backups to be
 * "restored... with a password or secure key" — i.e. portable. Connector tokens and the
 * OpenRouter key still use Keystore-backed [AndroidSecretStore] since those never need
 * to leave the device.
 *
 * Format written to disk: [salt(16 bytes)][iv(12 bytes)][ciphertext+GCM tag].
 */
object BackupCrypto {

    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        return salt + iv + ciphertext
    }

    fun decrypt(payload: ByteArray, password: CharArray): ByteArray {
        require(payload.size > SALT_LENGTH_BYTES + IV_LENGTH_BYTES) { "Backup file is truncated or corrupt." }

        val salt = payload.copyOfRange(0, SALT_LENGTH_BYTES)
        val iv = payload.copyOfRange(SALT_LENGTH_BYTES, SALT_LENGTH_BYTES + IV_LENGTH_BYTES)
        val ciphertext = payload.copyOfRange(SALT_LENGTH_BYTES + IV_LENGTH_BYTES, payload.size)
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        // Throws AEADBadTagException on wrong password / tampered file — caller should
        // surface that as "wrong password or corrupted backup", never silently succeed.
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec: KeySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
}
