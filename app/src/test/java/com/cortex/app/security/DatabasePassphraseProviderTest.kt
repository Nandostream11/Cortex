package com.cortex.app.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

private class FakeSecretStore : SecretStore {
    private val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String): String? = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun contains(key: String): Boolean = map.containsKey(key)
}

class DatabasePassphraseProviderTest {

    @Test
    fun `generates a 256-bit passphrase`() {
        val passphrase = DatabasePassphraseProvider(FakeSecretStore()).getOrCreatePassphrase()
        assertEquals(32, passphrase.size)
    }

    @Test
    fun `repeated calls on the same provider return the identical passphrase`() {
        val provider = DatabasePassphraseProvider(FakeSecretStore())
        assertArrayEquals(provider.getOrCreatePassphrase(), provider.getOrCreatePassphrase())
    }

    @Test
    fun `a fresh provider over the same store retrieves the same stored passphrase`() {
        val store = FakeSecretStore()
        val first = DatabasePassphraseProvider(store).getOrCreatePassphrase()
        val second = DatabasePassphraseProvider(store).getOrCreatePassphrase()
        assertArrayEquals(first, second)
    }

    @Test
    fun `independent stores get independently random passphrases`() {
        val a = DatabasePassphraseProvider(FakeSecretStore()).getOrCreatePassphrase()
        val b = DatabasePassphraseProvider(FakeSecretStore()).getOrCreatePassphrase()
        assertFalse(a.contentEquals(b))
    }
}
