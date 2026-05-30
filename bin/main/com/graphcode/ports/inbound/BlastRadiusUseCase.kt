package com.graphcode.ports.inbound

import kotlinx.serialization.Serializable

@Serializable
data class BlastRadiusPath(
    val pathId: Int,
    val nodes: List<String>,
    val severity: String // ALTA (direct), MÉDIA (1 hop), BAIXA (>1 hop)
)

@Serializable
data class BlastRadiusResult(
    val targetNode: String,
    val totalImpactedNodes: Int,
    val paths: List<BlastRadiusPath>
)

interface BlastRadiusUseCase {
    suspend fun calculateImpact(qualifiedName: String, maxDepth: Int = 5): BlastRadiusResult
}
