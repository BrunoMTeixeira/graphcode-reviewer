package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.adapters.outbound.persistence.neo4j.entities.*
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

class KNeo4jContainer : Neo4jContainer<KNeo4jContainer>("neo4j:5.18.0-apoc") {
    init {
        withEnv("NEO4J_apoc_export_file_enabled", "true")
        withEnv("NEO4J_apoc_import_file_enabled", "true")
        withEnv("NEO4J_apoc_import_file_use__neo4j__config", "true")
        withEnv("NEO4J_PLUGINS", "[\"apoc\"]")
    }
}
@DataNeo4jTest
@Testcontainers
class GraphPersistenceTest {

    @Autowired
    lateinit var fileEntityRepository: FileEntityRepository

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
    fun `should save FileEntity and its CompositeProperties in Neo4j via cascata`() {
        val fileEntity = FileEntity(
            filePath = "src/UserService.kt",
            contentHash = "hash123",
            sizeBytes = 1024,
            position = mapOf("startLine" to 1L, "endLine" to 10L, "startByte" to 0L, "endByte" to 100L)
        )

        val funcEntity = FunctionEntity(
            id = "func-1",
            name = "login",
            signature = "login()",
            returnType = "Unit",
            complexity = 1,
            position = mapOf("startLine" to 2L, "endLine" to 5L, "startByte" to 10L, "endByte" to 50L)
        )

        fileEntity.functions.add(funcEntity)

        fileEntityRepository.save(fileEntity)

        val saved = fileEntityRepository.findByFilePath("src/UserService.kt")
        
        assertNotNull(saved)
        assertEquals("hash123", saved?.contentHash)
        assertEquals(1, saved?.functions?.size)
        assertEquals("login", saved?.functions?.first()?.name)
        
        // Posição agora não é Null! O @CompositeProperty resolve o problema da Persistência Hexagonal.
        assertEquals(2L, saved?.functions?.first()?.position?.get("startLine"))
    }
}
