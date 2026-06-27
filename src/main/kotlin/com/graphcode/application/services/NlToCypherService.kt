package com.graphcode.application.services

import com.graphcode.ports.inbound.NlToCypherResult
import com.graphcode.ports.inbound.NlToCypherUseCase
import com.graphcode.ports.outbound.LlmClientPort
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Service
class NlToCypherService(
    private val llmClientPort: LlmClientPort,
    private val neo4jClient: Neo4jClient
) : NlToCypherUseCase {

    private val logger = LoggerFactory.getLogger(NlToCypherService::class.java)

    override suspend fun executeNaturalLanguageQuery(query: String): NlToCypherResult {
        var retries = 0
        var maxRetries = 3
        var currentCypher = ""
        var lastError = ""
        
        while (retries < maxRetries) {
            try {
                if (retries == 0) {
                    currentCypher = llmClientPort.generateCypher(query)
                } else {
                    logger.warn("Tentativa $retries/3 de corrigir Cypher: $currentCypher | Erro: $lastError")
                    currentCypher = llmClientPort.generateCypher(query, lastError, currentCypher)
                }
                
                logger.info("Executando Cypher gerado: $currentCypher")
                
                // Tenta executar
                val results = neo4jClient.query(currentCypher).fetch().all()
                val jsonResult = Json.encodeToString(results.map { it.toString() })
                
                return NlToCypherResult(
                    naturalLanguageQuery = query,
                    generatedCypher = currentCypher,
                    jsonResult = jsonResult,
                    retries = retries,
                    success = true
                )
                
            } catch (e: Exception) {
                lastError = e.message ?: "Erro desconhecido"
                retries++
            }
        }
        
        return NlToCypherResult(
            naturalLanguageQuery = query,
            generatedCypher = currentCypher,
            jsonResult = "Falha após $maxRetries tentativas. Último erro: $lastError",
            retries = retries,
            success = false
        )
    }
}
