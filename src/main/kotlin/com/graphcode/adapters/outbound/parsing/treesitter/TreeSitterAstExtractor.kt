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
import org.treesitter.TreeSitterCSharp
import org.treesitter.TreeSitterGo
import org.treesitter.TreeSitterRuby
import org.treesitter.TreeSitterPhp
import org.treesitter.TreeSitterRust
import org.treesitter.TreeSitterScala
import org.treesitter.TreeSitterSwift
import org.treesitter.TreeSitterTypescript
import org.treesitter.TreeSitterBash
import org.treesitter.TreeSitterHtml
import org.treesitter.TreeSitterCss
import org.treesitter.TreeSitterJson
import org.treesitter.TreeSitterYaml
import org.treesitter.TreeSitterToml
import org.treesitter.TreeSitterHaskell
import org.treesitter.TreeSitterJulia
import org.treesitter.TreeSitterOcaml
import org.treesitter.TreeSitterNim
import org.treesitter.TreeSitterErlang
import org.treesitter.TreeSitterLua
import org.treesitter.TreeSitterPerl
import org.treesitter.TreeSitterVue
import org.treesitter.TreeSitterSvelte
import org.treesitter.TreeSitterMarkdown
import org.treesitter.TreeSitterLatex
import org.treesitter.TreeSitterRacket
import org.treesitter.TreeSitterScheme
import org.treesitter.TreeSitterPascal
import org.treesitter.TreeSitterM68k
import org.treesitter.TreeSitterAgda
import org.treesitter.TreeSitterPgn
import org.treesitter.TreeSitterRegex
import org.treesitter.TreeSitterMeson
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
            val sourceCodeBytes = fileContent.toByteArray(Charsets.UTF_8)

            traverseAndExtract(rootNode, fileNode, sourceCodeBytes, currentClass = null, currentFunction = null)
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
            "js", "jsx" -> TreeSitterJavascript()
            "ts", "tsx" -> TreeSitterTypescript()
            "c" -> TreeSitterC()
            "cpp", "hpp", "h", "cc", "cxx" -> TreeSitterCpp()
            "cs" -> TreeSitterCSharp()
            "go" -> TreeSitterGo()
            "rb" -> TreeSitterRuby()
            "php" -> TreeSitterPhp()
            "rs" -> TreeSitterRust()
            "scala" -> TreeSitterScala()
            "swift" -> TreeSitterSwift()
            "sh", "bash" -> TreeSitterBash()
            "html", "htm" -> TreeSitterHtml()
            "css" -> TreeSitterCss()
            "json" -> TreeSitterJson()
            "yml", "yaml" -> TreeSitterYaml()
            "toml" -> TreeSitterToml()
            "hs" -> TreeSitterHaskell()
            "jl" -> TreeSitterJulia()
            "ml", "mli" -> TreeSitterOcaml()
            "nim" -> TreeSitterNim()
            "erl", "hrl" -> TreeSitterErlang()
            "lua" -> TreeSitterLua()
            "pl", "pm" -> TreeSitterPerl()
            "vue" -> TreeSitterVue()
            "svelte" -> TreeSitterSvelte()
            "md" -> TreeSitterMarkdown()
            "tex" -> TreeSitterLatex()
            "rkt" -> TreeSitterRacket()
            "scm", "ss" -> TreeSitterScheme()
            "pas" -> TreeSitterPascal()
            "agda" -> TreeSitterAgda()
            "pgn" -> TreeSitterPgn()
            else -> null
        }
    }

    private fun traverseAndExtract(
        node: TSNode, 
        fileNode: FileNode, 
        sourceCodeBytes: ByteArray, 
        currentClass: ClassNode?, 
        currentFunction: FunctionNode?
    ) {
        for (i in 0 until node.getChildCount()) {
            val currentNode = node.getChild(i)

            var activeClass = currentClass
            var activeFunction = currentFunction

            processClassNode(currentNode, sourceCodeBytes)?.let { newClass ->
                fileNode.classes.add(newClass)
                activeClass = newClass
            }

            processFunctionNode(currentNode, sourceCodeBytes)?.let { newFunc ->
                if (activeClass != null) {
                    activeClass!!.functions.add(newFunc)
                } else {
                    fileNode.functions.add(newFunc)
                }
                activeFunction = newFunc // Mergulhar o contexto
            }

            processCallNode(currentNode, sourceCodeBytes, activeFunction)

            // Recursão com a classe e função ativas para capturar nested structures
            traverseAndExtract(currentNode, fileNode, sourceCodeBytes, activeClass, activeFunction)
        }
    }

    private fun processClassNode(node: TSNode, sourceCodeBytes: ByteArray): ClassNode? {
        val type = node.getType() ?: ""
        if (type.contains("class_declaration") || type.contains("class_definition")) {
            return extractClass(node, sourceCodeBytes)
        }
        return null
    }

    private fun processFunctionNode(node: TSNode, sourceCodeBytes: ByteArray): FunctionNode? {
        val type = node.getType() ?: ""
        if (type.contains("function_declaration") || type.contains("method_declaration") || type.contains("fun_declaration") || type.contains("function_definition")) {
            return extractFunction(node, sourceCodeBytes)
        }
        return null
    }

    private fun processCallNode(node: TSNode, sourceCodeBytes: ByteArray, activeFunction: FunctionNode?) {
        val type = node.getType() ?: ""
        if (type == "call" || type.contains("call_expression") || type.contains("method_invocation")) {
            if (activeFunction != null) {
                val calledName = extractCalledFunctionName(node, sourceCodeBytes)
                if (calledName.isNotEmpty() && calledName.matches(Regex("^[A-Za-z][A-Za-z0-9_]*$"))) {
                    activeFunction.calledFunctions.add(calledName)
                }
            }
        }
    }

    private fun extractClass(node: TSNode, sourceCodeBytes: ByteArray): ClassNode? {
        val nameNode = findChildByType(node, listOf("identifier", "type_identifier", "name"))
        val className = nameNode?.let { extractText(it, sourceCodeBytes) } ?: "UnknownClass"
        
        if (!className.matches(Regex("^[A-Za-z][A-Za-z0-9_]*$"))) return null

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

    private fun extractFunction(node: TSNode, sourceCodeBytes: ByteArray): FunctionNode? {
        val nameNode = findChildByType(node, listOf("identifier", "name", "simple_identifier"))
        val funcName = nameNode?.let { extractText(it, sourceCodeBytes) } ?: "UnknownFunction"

        if (!funcName.matches(Regex("^[A-Za-z][A-Za-z0-9_]*$"))) return null

        val typeNode = findChildByType(node, listOf("type_reference", "type_identifier", "type", "primitive_type", "return_type"))
        val returnType = typeNode?.let { extractText(it, sourceCodeBytes) } ?: "Unit"

        val complexity = 1 + calculateComplexity(node)

        return FunctionNode(
            name = funcName,
            signature = "$funcName()",
            returnType = returnType,
            complexity = complexity,
            position = Position(
                startLine = node.getStartPoint().getRow().toLong(),
                startByte = node.getStartByte().toLong(),
                endLine = node.getEndPoint().getRow().toLong(),
                endByte = node.getEndByte().toLong()
            )
        )
    }

    private fun calculateComplexity(node: TSNode): Int {
        var complexity = 0
        for (i in 0 until node.getChildCount()) {
            val child = node.getChild(i)
            val type = child.getType() ?: ""
            if (type in listOf("if_expression", "if_statement", "when_expression", "switch_statement", "for_statement", "while_statement", "catch_clause", "catch_block", "&&", "||")) {
                complexity++
            }
            complexity += calculateComplexity(child)
        }
        return complexity
    }

    private fun extractCalledFunctionName(callNode: TSNode, sourceCodeBytes: ByteArray): String {
        // Em Python um método de classe é 'attribute' em vez de 'member_expression', e a chamada em si é 'call'
        var targetNode: TSNode? = callNode
        var nameNode = findChildByType(targetNode!!, listOf("identifier", "name", "member_expression", "property_identifier", "attribute"))
        
        // Se for um atributo ou membro, a verdadeira função é o identificador mais à direita
        if (nameNode != null && (nameNode.getType() == "attribute" || nameNode.getType() == "member_expression")) {
            val nestedName = findLastChildByType(nameNode, listOf("identifier", "property_identifier"))
            if (nestedName != null) {
                nameNode = nestedName
            }
        }
        
        return nameNode?.let { extractText(it, sourceCodeBytes) } ?: ""
    }

    private fun extractText(node: TSNode, sourceCodeBytes: ByteArray): String {
        val start = node.getStartByte()
        val end = node.getEndByte()
        if (start < 0 || end > sourceCodeBytes.size || start >= end) return ""
        return String(sourceCodeBytes.copyOfRange(start, end), Charsets.UTF_8).trim()
    }

    private fun findChildByType(node: TSNode, types: List<String>): TSNode? {
        for (i in 0 until node.getChildCount()) {
            val current = node.getChild(i)
            val type = current.getType() ?: ""
            if (types.any { type.contains(it) }) return current
        }
        return null
    }

    private fun findLastChildByType(node: TSNode, types: List<String>): TSNode? {
        for (i in node.getChildCount() - 1 downTo 0) {
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
