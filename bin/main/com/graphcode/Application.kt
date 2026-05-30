package com.graphcode

import com.graphcode.ports.inbound.AnalyzeRepositoryPort
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application(
    private val analyzeRepositoryUseCase: AnalyzeRepositoryPort
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        // Agora a orquestração do parsing, hashing e BD foi delegada à Camada de Aplicação!
        val targetDirectory = "src/main/kotlin/com/graphcode" // Para testarmos o scanner na pasta do código fonte
        analyzeRepositoryUseCase.execute(targetDirectory)
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
