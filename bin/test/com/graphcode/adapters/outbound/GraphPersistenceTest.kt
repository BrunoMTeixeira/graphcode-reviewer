package com.graphcode.adapters.outbound

import com.graphcode.domain.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

class KNeo4jContainer : Neo4jContainer<KNeo4jContainer>("neo4j:5.18.0")

@DataNeo4jTest
@Testcontainers
class GraphPersistenceTest {

    @Autowired
    lateinit var fileNodeRepository: FileNodeRepository

    companion object {
        @Container
        @JvmStatic
        val neo4jContainer = KNeo4jContainer().apply {
            withAdminPassword("secret123")
        }

        @JvmStatic
        @DynamicPropertySource
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl)
            registry.add("spring.neo4j.authentication.username") { "neo4j" }
            registry.add("spring.neo4j.authentication.password") { "secret123" }
        }
    }

    @Test
    fun `should save FileNode and its related FunctionNodes in Neo4j via cascata`() {
        // Arrange (Preparar os Dados do nosso Domínio Puro)
        val fileNode = FileNode(
            filePath = "src/UserService.kt",
            contentHash = "hash123",
            sizeBytes = 1024,
            position = Position(1, 10, 0, 100)
        )

        val funcNode = FunctionNode(
            name = "login",
            signature = "login()",
            returnType = "Unit",
            complexity = 1,
            position = Position(2, 5, 10, 50)
        )

        fileNode.functions.add(funcNode)

        // Act (Agir: Escrever no Grafo)
        fileNodeRepository.save(fileNode)

        // Assert (Verificar se ficou guardado com as relações)
        val saved = fileNodeRepository.findByFilePath("src/UserService.kt")
        
        assertNotNull(saved)
        assertEquals("hash123", saved?.contentHash)
        assertEquals(1, saved?.functions?.size)
        assertEquals("login", saved?.functions?.first()?.name)
        
        // Validação da Posição Embedded (CompositeProperty)
        assertEquals(2L, saved?.functions?.first()?.position?.startLine)
    }
}
