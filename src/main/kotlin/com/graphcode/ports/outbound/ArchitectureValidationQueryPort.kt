package com.graphcode.ports.outbound

import com.graphcode.domain.models.config.GraphCodeConfig

interface ArchitectureValidationQueryPort {
    suspend fun getViolations(config: GraphCodeConfig?): Pair<List<String>, Int>
}
