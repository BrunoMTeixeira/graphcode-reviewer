package com.graphcode.application.services

import com.graphcode.ports.inbound.AnalyzeRepositoryPort
import com.graphcode.ports.outbound.AstExtractorPort
import com.graphcode.ports.outbound.DirectoryScannerPort
import com.graphcode.ports.outbound.SaveGraphRepositoryPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class AnalyzeRepositoryService(
    private val scannerPort: DirectoryScannerPort,
    private val extractorPort: AstExtractorPort,
    private val saveGraphPort: SaveGraphRepositoryPort
) : AnalyzeRepositoryPort {

    private val log = KotlinLogging.logger {}

    override fun execute(directoryPath: String) {
        log.info { "A iniciar análise do repositório em: ${directoryPath}" }
        
        val files = scannerPort.scanDirectory(directoryPath)
        log.info { "Foram encontrados ${files.size} ficheiros para análise." }
        
        var filesSaved = 0
        var classesExtracted = 0
        var functionsExtracted = 0

        for (file in files) {
            try {
                // 1. Extração da Árvore de Sintaxe
                val fileNode = extractorPort.extract(file.content, file.filePath)
                
                // 2. Persistência Incremental
                saveGraphPort.saveFileGraph(fileNode)
                
                filesSaved++
                classesExtracted += fileNode.classes.size
                functionsExtracted += fileNode.functions.size
                
            } catch (e: Exception) {
                log.warn { "Falha a analisar o ficheiro ${file.filePath}: ${e.message}" }
            }
        }
        
        log.info { "✅ Análise Concluída! Ficheiros validados: $filesSaved | Classes: $classesExtracted | Funções: $functionsExtracted" }
    }
}
