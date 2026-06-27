package com.graphcode.adapters.outbound.parsing.treesitter

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TreeSitterAstExtractorTest {

    private val extractor = TreeSitterAstExtractor()

    @Test
    fun `should parse kotlin code and extract classes and functions`() {
        val kotlinCode = """
            package com.example
            class MyKotlinClass {
                fun myAwesomeFunction() {
                    println("Hello")
                }
            }
            fun topLevelFunction() {
                MyKotlinClass()
            }
        """.trimIndent()
        
        val fileNode = extractor.extract(kotlinCode, "src/MyKotlinClass.kt")
        
        assertEquals("src/MyKotlinClass.kt", fileNode.filePath)
        assertEquals(1, fileNode.classes.size)
        assertEquals("MyKotlinClass", fileNode.classes[0].name)
        
        assertEquals(1, fileNode.functions.size)
        assertTrue(fileNode.functions.any { it.name == "topLevelFunction" })
        
        assertEquals(1, fileNode.classes[0].functions.size)
        assertTrue(fileNode.classes[0].functions.any { it.name == "myAwesomeFunction" })
    }

    @Test
    fun `should generate identical SHA-256 hash for identical content`() {
        val code = "fun main() {}"
        
        val node1 = extractor.extract(code, "A.kt")
        val node2 = extractor.extract(code, "B.kt")
        val node3 = extractor.extract(code + " ", "C.kt")
        
        assertNotNull(node1.contentHash)
        assertEquals(node1.contentHash, node2.contentHash)
        assertTrue(node1.contentHash != node3.contentHash)
    }
}
