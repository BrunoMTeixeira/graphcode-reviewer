package com.graphcode

import com.graphcode.ports.inbound.AnalyzeRepositoryPort
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import com.graphcode.adapters.outbound.persistence.neo4j.FileEntityRepository

@SpringBootApplication
class Application(
    private val analyzeRepositoryUseCase: AnalyzeRepositoryPort,
    private val repository: FileEntityRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        // Limpar a BD a cada arranque (útil para desenvolvimento/estágio)
        repository.clearDatabase()
        
        // Agora a orquestração do parsing, hashing e BD foi delegada à Camada de Aplicação!
        val targetDirectory = "C:/Users/btmic/Desktop/graphcode-reviewer/src/main/kotlin"
        analyzeRepositoryUseCase.execute(targetDirectory)
        
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
