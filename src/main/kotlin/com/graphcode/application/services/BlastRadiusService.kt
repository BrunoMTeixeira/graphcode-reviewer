package com.graphcode.application.services

import com.graphcode.ports.inbound.BlastRadiusResult
import com.graphcode.ports.inbound.BlastRadiusUseCase
import com.graphcode.ports.outbound.BlastRadiusQueryPort
import org.springframework.stereotype.Service

@Service
class BlastRadiusService(
    private val blastRadiusQueryPort: BlastRadiusQueryPort
) : BlastRadiusUseCase {
    
    override suspend fun calculateImpact(qualifiedName: String, maxDepth: Int): BlastRadiusResult {
        return blastRadiusQueryPort.findImpactPaths(qualifiedName, maxDepth)
    }
}
