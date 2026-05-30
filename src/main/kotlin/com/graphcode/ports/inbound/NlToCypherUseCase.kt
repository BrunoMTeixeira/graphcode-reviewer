package com.graphcode.ports.inbound

import kotlinx.serialization.Serializable

@Serializable
data class NlToCypherResult(
    val naturalLanguageQuery: String,
    val generatedCypher: String,
    val jsonResult: String,
    val retries: Int,
    val success: Boolean
)

interface NlToCypherUseCase {
    suspend fun executeNaturalLanguageQuery(query: String): NlToCypherResult
}
