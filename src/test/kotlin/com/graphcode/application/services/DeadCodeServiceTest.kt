package com.graphcode.application.services

import com.graphcode.ports.inbound.DeadCodeResult
import com.graphcode.ports.outbound.DeadCodeQueryPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeadCodeServiceTest {

    private val deadCodeQueryPort = mockk<DeadCodeQueryPort>()
    private val deadCodeService = DeadCodeService(deadCodeQueryPort)

    @Test
    fun `should find dead code and cycles`() = runTest {
        // Arrange
        val expectedList = listOf(
            DeadCodeResult("OldClass", "src/OldClass.kt", com.graphcode.domain.models.Severity.MEDIA, "Unreachable code"),
            DeadCodeResult("A <-> B", "Grafo", com.graphcode.domain.models.Severity.ALTA, "Circular dependency detected")
        )
        coEvery { deadCodeQueryPort.findIsolatedNodesAndCycles() } returns expectedList

        // Act
        val result = deadCodeService.findDeadCodeAndCycles()

        // Assert
        assertEquals(2, result.size)
        assertEquals(expectedList, result)
        coVerify(exactly = 1) { deadCodeQueryPort.findIsolatedNodesAndCycles() }
    }
}
