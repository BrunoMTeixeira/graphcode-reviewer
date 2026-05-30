package com.graphcode.services

import com.graphcode.ports.inbound.MigrationFileContext
import com.graphcode.ports.outbound.MigrationQueryPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

class MigrationEngineServiceTest {

    private val migrationQueryPort = mockk<MigrationQueryPort>()
    private val migrationEngineService = MigrationEngineService(migrationQueryPort)

    @Test
    fun `should perform topological sort using Kahn algorithm`() = runTest {
        // Arrange
        // A depende de B e C
        // B depende de C
        // Ordem topológica esperada (folhas primeiro, depois dependentes): C, B, A
        val contexts = listOf(
            MigrationFileContext("A.kt", "codeA", listOf("B.kt", "C.kt"), "sumA"),
            MigrationFileContext("B.kt", "codeB", listOf("C.kt"), "sumB"),
            MigrationFileContext("C.kt", "codeC", emptyList(), "sumC")
        )
        
        coEvery { migrationQueryPort.getSubgraphContext(any()) } returns contexts

        // Act
        val result = migrationEngineService.getMigrationContext(listOf("A.kt", "B.kt", "C.kt"))

        // Assert
        assertEquals(3, result.topologicalOrder.size)
        // C.kt tem 0 dependências de entrada (depois da inversão de Kahn no código), então C é processado primeiro
        assertEquals("C.kt", result.topologicalOrder[0].filePath)
        assertEquals("B.kt", result.topologicalOrder[1].filePath)
        assertEquals("A.kt", result.topologicalOrder[2].filePath)
        
        assertTrue(result.executionTimeMs >= 0)
        coVerify(exactly = 1) { migrationQueryPort.getSubgraphContext(listOf("A.kt", "B.kt", "C.kt")) }
    }
}
