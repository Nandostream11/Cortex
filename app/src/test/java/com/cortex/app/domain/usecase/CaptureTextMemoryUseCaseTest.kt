package com.cortex.app.domain.usecase

import com.cortex.app.data.repository.FakeMemoryRepository
import com.cortex.app.domain.model.MemoryCategory
import com.cortex.app.domain.model.MemorySourceType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CaptureTextMemoryUseCaseTest {

    private lateinit var repository: FakeMemoryRepository
    private lateinit var useCase: CaptureTextMemoryUseCase

    @Before
    fun setUp() {
        repository = FakeMemoryRepository()
        useCase = CaptureTextMemoryUseCase(repository)
    }

    @Test
    fun `blank text is rejected before touching the repository`() = runTest {
        var threw = false
        try {
            useCase("   ")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("expected IllegalArgumentException for blank input", threw)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `capturing text persists a normalized memory item`() = runTest {
        val saved = useCase("  This   has\nweird   whitespace  ")

        assertEquals("This has weird whitespace", saved.normalizedText)
        assertEquals(1, repository.saved.size)
        assertEquals(saved.id, repository.saved.first().id)
    }

    @Test
    fun `bug-flavored text is classified as BUG_REPORT`() = runTest {
        val saved = useCase("Getting a null pointer exception on startup")
        assertEquals(MemoryCategory.BUG_REPORT, saved.category)
    }

    @Test
    fun `text capture is fully trusted, voice capture is not`() = runTest {
        val typed = useCase("typed note", sourceType = MemorySourceType.TEXT)
        val spoken = useCase("spoken note", sourceType = MemorySourceType.VOICE)

        assertEquals(1.0, typed.confidenceScore, 0.0)
        assertTrue(spoken.confidenceScore < typed.confidenceScore)
    }

    @Test
    fun `importance score always stays within zero and one`() = runTest {
        val saved = useCase("x".repeat(5_000))
        assertTrue(saved.importanceScore in 0.0..1.0)
    }
}
