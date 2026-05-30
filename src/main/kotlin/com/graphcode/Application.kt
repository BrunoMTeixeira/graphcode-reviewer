package com.graphcode

import com.graphcode.domain.models.*
import com.graphcode.adapters.outbound.FileNodeRepository
import com.graphcode.adapters.outbound.TreeSitterAstExtractor
import com.graphcode.adapters.outbound.Neo4jSaveGraphAdapter
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.slf4j.LoggerFactory

@SpringBootApplication
class Application {
    private val log = LoggerFactory.getLogger(Application::class.java)

    @Bean
    fun init(repository: FileNodeRepository) = CommandLineRunner {
        log.info("Limpando a base de dados local para o teste...")
        repository.deleteAll()

        log.info("A iniciar o Extrator de AST Multi-Linguagem...")
        val extractor = TreeSitterAstExtractor()
        
        // Vamos ler o próprio código fonte deste ficheiro para testar o parser!
        val myCodePath = "src/main/kotlin/com/graphcode/Application.kt"
        val codeContent = java.io.File(myCodePath).readText()
        
        log.info("Extraindo AST de Application.kt...")
        val fileNode = extractor.extract(codeContent, myCodePath)
        
        log.info("Encontradas ${fileNode.classes.size} classes e ${fileNode.functions.size} funções.")
        
        log.info("A gravar na Base de Dados (Teste de Incremento #24)...")
        // Como o adaptador ainda não está diretamente ligado no Application, fazemos a verificação de hash nós próprios aqui para testar o Neo4jSaveGraphAdapter (que faz isto por trás)
        val adapter = Neo4jSaveGraphAdapter(repository)
        adapter.saveFileGraph(fileNode)
        
        log.info("✅ Código lido, extraído e persistido com sucesso! Abre o teu Neo4j Desktop.")
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
