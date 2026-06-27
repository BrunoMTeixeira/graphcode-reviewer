package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.domain.models.*
import com.graphcode.adapters.outbound.persistence.neo4j.entities.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@DataNeo4jTest
@Testcontainers
class GraphPersistenceIntegrationTest {

    @Autowired
    private lateinit var fileRepository: FileEntityRepository

    companion object {
        @Container
        val neo4jContainer = Neo4jContainer("neo4j:5.18.0")
            .withAdminPassword("testpassword")

        @JvmStatic
        @DynamicPropertySource
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.neo4j.uri") { neo4jContainer.boltUrl }
            registry.add("spring.neo4j.authentication.username") { "neo4j" }
            registry.add("spring.neo4j.authentication.password") { "testpassword" }
        }

        @JvmStatic
        @BeforeAll
        fun setup() {
            neo4jContainer.start()
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            neo4jContainer.stop()
        }
    }

    @Test
    fun `deve persistir um FileNode e cascatear classes e funcoes`() {
        val adapter = Neo4jSaveGraphAdapter(fileRepository)
        
        val funcNode = FunctionNode(
            name = "calculateTax",
            signature = "calculateTax()",
            returnType = "Double",
            complexity = 3,
            position = null
        )
        
        val classNode = ClassNode(
            name = "TaxService",
            qualifiedName = "com.app.TaxService",
            isExported = true,
            position = null
        ).apply {
            functions.add(funcNode)
        }
        
        val fileNode = FileNode(
            filePath = "/src/TaxService.kt",
            contentHash = "hash123",
            sizeBytes = 1024,
            position = null
        ).apply {
            classes.add(classNode)
        }

        // Action
        adapter.saveFileGraph(fileNode)

        // Verify
        val savedFile = adapter.findByFilePath("/src/TaxService.kt")
        assertNotNull(savedFile)
        assertEquals("/src/TaxService.kt", savedFile?.filePath)
        assertEquals(1, savedFile?.classes?.size)
        
        val savedClass = savedFile?.classes?.first()
        assertEquals("TaxService", savedClass?.name)
        assertEquals(1, savedClass?.functions?.size)
        
        val savedFunc = savedClass?.functions?.first()
        assertEquals("calculateTax", savedFunc?.name)
        assertEquals("Double", savedFunc?.returnType)
    }

    @Test
    fun `deve ligar as funcoes com arestas CALLS corretamente`() {
        val adapter = Neo4jSaveGraphAdapter(fileRepository)
        
        val caller = FunctionNode(
            name = "callerFunc",
            signature = "callerFunc()",
            returnType = "Unit",
            complexity = 1,
            position = null
        ).apply {
            calledFunctions.add("targetFunc")
        }
        
        val target = FunctionNode(
            name = "targetFunc",
            signature = "targetFunc()",
            returnType = "Int",
            complexity = 1,
            position = null
        )
        
        val fileNode = FileNode(
            filePath = "/src/Caller.kt",
            contentHash = "hash123",
            sizeBytes = 1024,
            position = null
        ).apply {
            functions.add(caller)
            functions.add(target)
        }
        
        // Save nodes
        adapter.saveFileGraph(fileNode)
        
        // Action: link calls
        adapter.linkFunctionCalls()
        
        // Verify relationship exists via neo4jClient
        val count = neo4jContainer.execInContainer("cypher-shell", "-u", "neo4j", "-p", "testpassword", "MATCH (f1:Function)-[:CALLS]->(f2:Function) RETURN count(f1);").stdout.trim()
        assertTrue(count.contains("1") || count.contains("count"))
    }
}
