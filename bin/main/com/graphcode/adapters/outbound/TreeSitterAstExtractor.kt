package com.graphcode.adapters.outbound

import com.graphcode.domain.models.*
import com.graphcode.ports.outbound.AstExtractorPort
import org.treesitter.TSParser
import org.treesitter.TSLanguage
import org.treesitter.TSNode
import org.treesitter.TreeSitterKotlin
import org.treesitter.TreeSitterJava
import org.treesitter.TreeSitterPython
import org.treesitter.TreeSitterJavascript
import org.treesitter.TreeSitterC
import org.treesitter.TreeSitterCpp
import java.security.MessageDigest

/**
 * Adaptador de Saída (Infrastructure) que implementa a extração usando Tree-sitter.
 * Instancia dinamicamente a gramática dependendo da linguagem.
 */
class TreeSitterAstExtractor : AstExtractorPort {

    override fun extract(fileContent: String, filePath: String): FileNode {
        val language = getLanguageForExtension(filePath)
            ?: return createEmptyFileNode(fileContent, filePath)
            
        val parser = TSParser()
        parser.setLanguage(language)
        
        val tree = parser.parseString(null, fileContent)
        val rootNode = tree.getRootNode()

        // 1. Criação do FileNode base
        val fileNode = FileNode(
            filePath = filePath,
            contentHash = calculateHash(fileContent),
            sizeBytes = fileContent.toByteArray().size.toLong(),
            position = null
        )

        // 2. Travessia da Árvore
        traverseAndExtract(rootNode, fileNode, fileContent)

        return fileNode
    }

    private fun getLanguageForExtension(filePath: String): TSLanguage? {
        val ext = filePath.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "kt", "kts" -> TreeSitterKotlin()
            "java" -> TreeSitterJava()
            "py" -> TreeSitterPython()
            "js", "jsx", "ts" -> TreeSitterJavascript()
            "c" -> TreeSitterC()
            "cpp", "hpp", "h", "cc", "cxx" -> TreeSitterCpp()
            else -> null
        }
    }
    
    private fun createEmptyFileNode(fileContent: String, filePath: String): FileNode {
        return FileNode(
            filePath = filePath,
            contentHash = calculateHash(fileContent),
            sizeBytes = fileContent.toByteArray().size.toLong(),
            position = null
        )
    }

    private fun traverseAndExtract(node: TSNode, fileNode: FileNode, sourceCode: String) {
        for (i in 0 until node.getChildCount()) {
            val currentNode = node.getChild(i)
            val type = currentNode.getType() ?: ""

            when {
                type.contains("class_declaration") || type.contains("class_definition") -> {
                    val classNode = extractClass(currentNode, sourceCode)
                    fileNode.classes.add(classNode)
                }
                type.contains("function_declaration") || type.contains("method_declaration") || type.contains("function_definition") || type.contains("fun_declaration") -> {
                    val functionNode = extractFunction(currentNode, sourceCode)
                    fileNode.functions.add(functionNode)
                }
            }
            
            // Recursão para apanhar classes/funções aninhadas (nested)
            traverseAndExtract(currentNode, fileNode, sourceCode)
        }
    }

    private fun extractClass(node: TSNode, sourceCode: String): ClassNode {
        val nameNode = findChildByType(node, listOf("identifier", "type_identifier", "name"))
        val className = nameNode?.let { extractText(it, sourceCode) } ?: "UnknownClass"

        return ClassNode(
            name = className,
            qualifiedName = className,
            isExported = true,
            position = null
        )
    }

    private fun extractFunction(node: TSNode, sourceCode: String): FunctionNode {
        val nameNode = findChildByType(node, listOf("identifier", "name"))
        val funcName = nameNode?.let { extractText(it, sourceCode) } ?: "UnknownFunction"

        return FunctionNode(
            name = funcName,
            signature = "$funcName()",
            returnType = "Unknown",
            complexity = 1,
            position = null
        )
    }

    private fun extractText(node: TSNode, sourceCode: String): String {
        val start = node.getStartByte()
        val end = node.getEndByte()
        if (start < 0 || end > sourceCode.length || start >= end) return ""
        return sourceCode.substring(start, end)
    }

    private fun findChildByType(node: TSNode, types: List<String>): TSNode? {
        for (i in 0 until node.getChildCount()) {
            val current = node.getChild(i)
            val type = current.getType() ?: ""
            if (types.any { type.contains(it) }) return current
        }
        return null
    }

    private fun calculateHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
