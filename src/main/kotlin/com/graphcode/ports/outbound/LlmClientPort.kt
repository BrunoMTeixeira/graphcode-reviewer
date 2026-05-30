package com.graphcode.ports.outbound

interface LlmClientPort {
    suspend fun generateCypher(naturalLanguageQuery: String, errorToFix: String? = null, previousCypher: String? = null): String
}
