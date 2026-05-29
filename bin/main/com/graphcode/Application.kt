package com.graphcode

import com.graphcode.domain.models.*
import com.graphcode.adapters.outbound.FileNodeRepository
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

        log.info("A criar um ficheiro virtual e enviá-lo para o Neo4j...")
        val node = FileNode(
            filePath = "src/Magia.kt",
            contentHash = "xyz789",
            sizeBytes = 2048,
            position = Position(1, 10, 0, 100)
        )
        
        node.functions.add(FunctionNode(name = "lancarMagia()", signature = "fun lancarMagia(): Unit", returnType = "Unit", complexity = 1, position = Position(1, 10, 0, 100)))
        
        repository.save(node)
        log.info("✅ Nó gravado com sucesso! Abre o teu Neo4j Desktop para veres o Grafo.")
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
