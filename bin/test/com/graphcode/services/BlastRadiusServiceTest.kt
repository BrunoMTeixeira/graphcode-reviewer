package com.graphcode.services

import com.graphcode.ports.inbound.BlastRadiusPath
import com.graphcode.ports.inbound.BlastRadiusResult
import com.graphcode.ports.outbound.BlastRadiusQueryPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlastRadiusServiceTest {

    private val blastRadiusQueryPort = mockk<BlastRadiusQueryPort>()
    private val blastRadiusService = BlastRadiusService(blastRadiusQueryPort)

    @Test
    fun `should calculate impact and return result from port`() = runTest {
        // Arrange
        val expectedResult = BlastRadiusResult(
            targetNode = "com.example.Service",
            totalImpactedNodes = 1,
            paths = listOf(BlastRadiusPath(1, listOf("com.example.Controller"), "ALTA"))
        )
        coEvery { blastRadiusQueryPort.findImpactPaths(any(), any()) } returns expectedResult

        // Act
        val result = blastRadiusService.calculateImpact("com.example.Service", 5)

        // Assert
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { blastRadiusQueryPort.findImpactPaths("com.example.Service", 5) }
    }
}
