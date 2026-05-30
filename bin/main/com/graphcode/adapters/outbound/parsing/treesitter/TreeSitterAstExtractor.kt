package com.graphcode.adapters.outbound.parsing.treesitter

import com.graphcode.domain.models.*
import com.graphcode.ports.outbound.AstExtractorPort
import org.springframework.stereotype.Component
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

@Component
class TreeSitterAstExtractor : AstExtractorPort {

    override fun extract(fileContent: String, filePath: String): FileNode {
        val fileNode = FileNode(
            filePath = filePath,
            contentHash = calculateHash(fileContent),
            sizeBytes = fileContent.toByteArray().size.toLong(),
            position = null
        )

        try {
            val language = getLanguageForExtension(filePath)
                ?: return fileNode
                
            val parser = TSParser()
            parser.setLanguage(language)
            
            val tree = parser.parseString(null, fileContent)
            val rootNode = tree.getRootNode()

            traverseAndExtract(rootNode, fileNode, fileContent, currentFunction = null)
        } catch (e: Exception) {
            // Se o parser nativo atirar exceção por syntax error grave, engolimos e devolvemos o nó vazio
        }
        
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

    private fun traverseAndExtract(node: TSNode, fileNode: FileNode, sourceCode: String, currentFunction: FunctionNode?) {
        for (i in 0 until node.getChildCount()) {
            val currentNode = node.getChild(i)
            val type = currentNode.getType() ?: ""

            var activeFunction = currentFunction

            when {
                type.contains("class_declaration") || type.contains("class_definition") -> {
                    fileNode.classes.add(extractClass(currentNode, sourceCode))
                }
                type.contains("function_declaration") || type.contains("method_declaration") || type.contains("fun_declaration") || type.contains("function_definition") -> {
                    val newFunc = extractFunction(currentNode, sourceCode)
                    fileNode.functions.add(newFunc)
                    activeFunction = newFunc // Mergulhar o contexto
                }
                type.contains("call_expression") || type.contains("method_invocation") -> {
                    // Mapeamento do Call Graph (Se encontrarmos uma chamada dentro de uma função, registamo-la)
                    if (activeFunction != null) {
                        val calledName = extractCalledFunctionName(currentNode, sourceCode)
                        if (calledName.isNotEmpty()) {
                            activeFunction.calledFunctions.add(calledName)
                        }
                    }
                }
            }
            
            // Recursão com a função ativa para capturar nested calls
            traverseAndExtract(currentNode, fileNode, sourceCode, activeFunction)
        }
    }

    private fun extractClass(node: TSNode, sourceCode: String): ClassNode {
        val nameNode = findChildByType(node, listOf("identifier", "type_identifier", "name"))
        val className = nameNode?.let { extractText(it, sourceCode) } ?: "UnknownClass"

        return ClassNode(
            name = className,
            qualifiedName = className,
            isExported = true,
            position = Position(
                startLine = node.getStartPoint().getRow().toLong(),
                startByte = node.getStartByte().toLong(),
                endLine = node.getEndPoint().getRow().toLong(),
                endByte = node.getEndByte().toLong()
            )
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
            position = Position(
                startLine = node.getStartPoint().getRow().toLong(),
                startByte = node.getStartByte().toLong(),
                endLine = node.getEndPoint().getRow().toLong(),
                endByte = node.getEndByte().toLong()
            )
        )
    }

    private fun extractCalledFunctionName(callNode: TSNode, sourceCode: String): String {
        // A sintaxe da call expression varia: `identifier` (foo()), ou `member_expression` (obj.foo())
        val nameNode = findChildByType(callNode, listOf("identifier", "name", "member_expression", "property_identifier"))
        return nameNode?.let { extractText(it, sourceCode) } ?: ""
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
        return digest.digest(content.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
