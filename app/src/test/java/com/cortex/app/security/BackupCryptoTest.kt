package com.cortex.app.security

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    @Test
    fun `encrypt then decrypt returns the original plaintext`() {
        val plaintext = """{"hello":"world","memories":[1,2,3]}""".toByteArray()
        val password = "correct horse battery staple".toCharArray()

        val encrypted = BackupCrypto.encrypt(plaintext, password.copyOf())
        assertNotEquals(plaintext.toList(), encrypted.toList())

        val decrypted = BackupCrypto.decrypt(encrypted, password.copyOf())
        assertTrue(plaintext.contentEquals(decrypted))
    }

    @Test
    fun `decrypt with wrong password throws`() {
        val plaintext = "sensitive backup contents".toByteArray()
        val encrypted = BackupCrypto.encrypt(plaintext, "right password".toCharArray())

        assertThrows(javax.crypto.AEADBadTagException::class.java) {
            BackupCrypto.decrypt(encrypted, "wrong password".toCharArray())
        }
    }

    @Test
    fun `decrypt of a truncated payload throws IllegalArgumentException`() {
        val plaintext = "sensitive backup contents".toByteArray()
        val encrypted = BackupCrypto.encrypt(plaintext, "a password".toCharArray())

        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decrypt(encrypted.copyOfRange(0, 10), "a password".toCharArray())
        }
    }

    @Test
    fun `two encryptions of the same plaintext produce different ciphertext`() {
        // Confirms salt+IV are actually randomized per call, not reused.
        val plaintext = "same content every time".toByteArray()
        val password = "a password".toCharArray()

        val first = BackupCrypto.encrypt(plaintext, password.copyOf())
        val second = BackupCrypto.encrypt(plaintext, password.copyOf())

        assertNotEquals(first.toList(), second.toList())
    }
}
