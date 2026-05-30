package com.graphcode.ports.inbound

import kotlinx.serialization.Serializable

@Serializable
data class MigrationFileContext(
    val filePath: String,
    val fileContent: String,
    val dependencies: List<String>,
    val summary: String // ex: "Contém 3 classes e 5 funções"
)

@Serializable
data class MigrationLoteResult(
    val executionTimeMs: Long,
    val topologicalOrder: List<MigrationFileContext>
)

interface MigrationEngineUseCase {
    suspend fun getMigrationContext(filePaths: List<String>): MigrationLoteResult
}
