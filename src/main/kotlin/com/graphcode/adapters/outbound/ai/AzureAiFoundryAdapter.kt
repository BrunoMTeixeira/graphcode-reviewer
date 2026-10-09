package com.graphcode.adapters.outbound.ai

import com.graphcode.ports.outbound.LlmClientPort
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.springframework.stereotype.Component
import io.github.oshai.kotlinlogging.KotlinLogging

@Component
class AzureAiFoundryAdapter : LlmClientPort {
    private val log = KotlinLogging.logger {}
    private val dotenv = dotenv { ignoreIfMissing = true }
    private val endpoint = dotenv["AZURE_OPENAI_ENDPOINT"] ?: System.getenv("AZURE_OPENAI_ENDPOINT")
    private val apiKey = dotenv["AZURE_OPENAI_API_KEY"] ?: System.getenv("AZURE_OPENAI_API_KEY")
    private val deploymentName = dotenv["AZURE_OPENAI_DEPLOYMENT_NAME"] ?: System.getenv("AZURE_OPENAI_DEPLOYMENT_NAME")
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun generateCypher(naturalLanguageQuery: String, errorToFix: String?, previousCypher: String?): String {
        if (endpoint == null || apiKey == null || deploymentName == null) {
            throw IllegalStateException("Azure AI Foundry credentials not configured in .env")
        }

        val url = if (endpoint.contains("/openai/deployments/")) {
            endpoint
        } else {
            val baseUrl = endpoint.removeSuffix("/")
            "$baseUrl/openai/deployments/$deploymentName/chat/completions?api-version=2024-02-15-preview"
        }
        
        var systemPrompt = "Tu és um assistente perito em Neo4j. Gera APENAS a query Cypher válida, sem formatação Markdown e sem explicações."
        systemPrompt += " O grafo tem nós com labels `File`, `Class`, `Function`, `DataModel`, `Endpoint`. As relações incluem `CONTAINS`, `CALLS`, `DEPENDS_ON`."
        
        var userPrompt = naturalLanguageQuery
        if (errorToFix != null && previousCypher != null) {
            userPrompt += "\n\nA query anterior falhou:\nQuery: $previousCypher\nErro: $errorToFix\nPor favor, corrige a query Cypher."
        }

        val requestBody = buildJsonObject {
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("temperature", 0.0)
        }
        
        println("A chamar a API da Azure: $url")

        val response: JsonObject = httpClient.post(url) {
            headers {
                append("api-key", apiKey)
            }
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()

        val cypher = response["choices"]
            ?.jsonArray?.get(0)
            ?.jsonObject?.get("message")
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content ?: throw IllegalStateException("Empty response from Azure AI")
            
        // Limpar markdown residual
        return cypher.replace("```cypher", "").replace("```", "").trim()
    }
}
