package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.domain.models.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Neo4jMapperTest {

    @Test
    fun `deve mapear domain FileNode para FileEntity mantendo aninhamentos de classes e funcoes`() {
        val domainFunction = FunctionNode(
            name = "calculate",
            signature = "calculate()",
            returnType = "Unit",
            complexity = 2,
            position = null
        ).apply {
            calledFunctions.add("otherFunction")
        }

        val domainClass = ClassNode(
            name = "Calculator",
            qualifiedName = "com.app.Calculator",
            isExported = true,
            position = null
        ).apply {
            functions.add(domainFunction)
        }

        val domainFile = FileNode(
            filePath = "/src/app.kt",
            contentHash = "hash",
            sizeBytes = 100,
            position = null
        ).apply {
            classes.add(domainClass)
        }

        // Action
        val entityFile = Neo4jMapper.toEntity(domainFile)

        // Verify
        assertEquals("/src/app.kt", entityFile.filePath)
        assertEquals(1, entityFile.classes.size)
        
        val entityClass = entityFile.classes.first()
        assertEquals("Calculator", entityClass.name)
        assertEquals(1, entityClass.functions.size)
        
        val entityFunc = entityClass.functions.first()
        assertEquals("calculate", entityFunc.name)
        assertTrue(entityFunc.calledFunctions.contains("otherFunction"))
    }

    @Test
    fun `deve mapear FileEntity de volta para domain FileNode`() {
        // Aproveitamos a testabilidade bidirecional do Mapper
        val domainOriginal = FileNode(
            filePath = "/src/test.kt",
            contentHash = "123",
            sizeBytes = 50
        ).apply {
            val cls = ClassNode(name = "TestClass", qualifiedName = "test", isExported = false)
            val func = FunctionNode(name = "testFunc", signature = "()", returnType = "Int", complexity = 1)
            cls.functions.add(func)
            classes.add(cls)
        }

        val entity = Neo4jMapper.toEntity(domainOriginal)
        
        // Action
        val domainReconstructed = Neo4jMapper.toDomain(entity)
        
        // Verify
        assertEquals(domainOriginal.filePath, domainReconstructed.filePath)
        assertEquals(1, domainReconstructed.classes.size)
        val clazz = domainReconstructed.classes.first()
        assertEquals("TestClass", clazz.name)
        assertEquals(1, clazz.functions.size)
        assertEquals("testFunc", clazz.functions.first().name)
    }
}
