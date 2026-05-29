package com.graphcode.adapters.outbound

import com.graphcode.domain.models.*
import com.graphcode.ports.outbound.AstExtractorPort
import io.github.treesitter.jtreesitter.Parser
import io.github.treesitter.jtreesitter.Language
import io.github.treesitter.jtreesitter.Node
import java.security.MessageDigest

/**
 * Adaptador de Saída (Infrastructure) que implementa a extração usando Tree-sitter.
 */
class TreeSitterAstExtractor(
    private val language: Language // A linguagem (ex: Java/Kotlin) é injetada
) : AstExtractorPort {

    override fun extract(fileContent: String, filePath: String): FileNode {
        val parser = Parser(language)
        val tree = parser.parse(fileContent) 
            ?: throw IllegalStateException("Não foi possível gerar a AST para o ficheiro $filePath")

        // val rootNode = tree.getRootNode()

        // 1. Criação do FileNode base
        val fileNode = FileNode(
            filePath = filePath,
            contentHash = calculateHash(fileContent),
            sizeBytes = fileContent.toByteArray().size.toLong(),
            position = null // Temporário até as bibliotecas de gramática estarem montadas
        )

        // 2. Travessia da Árvore
        // traverseAndExtract(rootNode, fileNode, fileContent)

        return fileNode
    }

    /*
    private fun traverseAndExtract(node: Node, fileNode: FileNode, sourceCode: String) {
        val cursor = node.walk()
        var hasNext = cursor.gotoFirstChild()

        while (hasNext) {
            val currentNode = cursor.currentNode

            // NOTA: Estes tipos ("class_declaration", "function_declaration") dependem
            // da gramática exata carregada (Java, Kotlin, etc).
            when (currentNode.type) {
                "class_declaration" -> {
                    val classNode = extractClass(currentNode, sourceCode)
                    fileNode.classes.add(classNode)
                }
                "function_declaration", "method_declaration" -> {
                    val functionNode = extractFunction(currentNode, sourceCode)
                    fileNode.functions.add(functionNode)
                }
                // TODO: Adicionar lógica para endpoints e anotações
            }

            hasNext = cursor.gotoNextSibling()
        }
    }
    */

    /*
    private fun extractClass(node: Node, sourceCode: String): ClassNode {
        // A extração do nome depende de procurar o nó filho correspondente a um "identifier"
        val nameNode = findChildByType(node, "identifier")
        val className = nameNode?.let { sourceCode.substring(it.startByte, it.endByte) } ?: "UnknownClass"

        return ClassNode(
            name = className,
            qualifiedName = className, // Simplificado
            isExported = true,
            position = extractPosition(node)
        )
    }

    private fun extractFunction(node: Node, sourceCode: String): FunctionNode {
        val nameNode = findChildByType(node, "identifier")
        val funcName = nameNode?.let { sourceCode.substring(it.startByte, it.endByte) } ?: "UnknownFunction"

        return FunctionNode(
            name = funcName,
            signature = "$funcName()", // Simplificado
            returnType = "Unit",       // Simplificado
            complexity = calculateComplexity(node),
            position = extractPosition(node)
        )
    }

    private fun extractPosition(node: Node): Position {
        val startPoint = node.startPoint
        val endPoint = node.endPoint
        return Position(
            startLine = startPoint.row.toLong() + 1, // Tree-sitter é 0-indexed
            endLine = endPoint.row.toLong() + 1,
            startByte = node.startByte.toLong(),
            endByte = node.endByte.toLong()
        )
    }

    private fun findChildByType(node: Node, type: String): Node? {
        val cursor = node.walk()
        if (!cursor.gotoFirstChild()) return null
        do {
            if (cursor.currentNode.type == type) return cursor.currentNode
        } while (cursor.gotoNextSibling())
        return null
    }

    private fun calculateComplexity(node: Node): Int {
        // Simplificação: Cada nó if, for, while ou catch incrementaria a complexidade ciclomática.
        return 1
    }
    */

    private fun calculateHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
