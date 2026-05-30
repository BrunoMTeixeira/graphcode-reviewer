package com.graphcode.ports.inbound

import kotlinx.serialization.Serializable

@Serializable
data class DeadCodeResult(
    val qualifiedName: String,
    val fileLocation: String,
    val severity: String,
    val reason: String
)

interface DeadCodeUseCase {
    suspend fun findDeadCodeAndCycles(): List<DeadCodeResult>
}
