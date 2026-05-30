package com.graphcode.services

import com.graphcode.ports.outbound.LlmClientPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.neo4j.driver.Record
import org.springframework.data.neo4j.core.Neo4jClient

class NlToCypherServiceTest {

    private val llmClientPort = mockk<LlmClientPort>()
    private val neo4jClient = mockk<Neo4jClient>()
    private val nlToCypherService = NlToCypherService(llmClientPort, neo4jClient)

    @Test
    fun `should execute cypher on first attempt`() = runTest {
        // Arrange
        val query = "Show all files"
        val generatedCypher = "MATCH (f:File) RETURN f"
        
        coEvery { llmClientPort.generateCypher(query) } returns generatedCypher
        
        val runnableSpec = mockk<Neo4jClient.UnboundRunnableSpec>()
        val recordFetchSpec = mockk<Neo4jClient.RecordFetchSpec<Map<String, Any>>>()
        
        coEvery { neo4jClient.query(generatedCypher) } returns runnableSpec
        coEvery { runnableSpec.fetch() } returns recordFetchSpec
        coEvery { recordFetchSpec.all() } returns emptyList()

        // Act
        val result = nlToCypherService.executeNaturalLanguageQuery(query)

        // Assert
        assertTrue(result.success)
        assertEquals(0, result.retries)
        assertEquals(generatedCypher, result.generatedCypher)
        assertEquals("[]", result.jsonResult)
    }
}
