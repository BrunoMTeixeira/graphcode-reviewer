package com.graphcode.ports.inbound

import kotlinx.serialization.Serializable

@Serializable
data class DeadCodeResult(
    val qualifiedName: String,
    val fileLocation: String,
    val severity: com.graphcode.domain.models.Severity,
    val reason: String
)

interface DeadCodeUseCase {
    suspend fun findDeadCodeAndCycles(): List<DeadCodeResult>
}
