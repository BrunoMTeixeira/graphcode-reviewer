package com.graphcode.ports.inbound

import com.graphcode.application.config.GraphCodeConfig
import kotlinx.serialization.Serializable

@Serializable
data class MigrationResult(
    val totalViolations: Int,
    val effortScore: Int,
    val violations: List<String>,
    val recommendation: String
)

interface MigrationValidationUseCase {
    suspend fun assessMigrationEffort(config: GraphCodeConfig?): MigrationResult
}
