package com.graphcode.ports.outbound

import com.graphcode.ports.inbound.BlastRadiusResult

interface BlastRadiusQueryPort {
    suspend fun findImpactPaths(qualifiedName: String, maxDepth: Int): BlastRadiusResult
}
