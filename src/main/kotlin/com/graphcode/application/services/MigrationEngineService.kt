package com.graphcode.application.services

import com.graphcode.ports.inbound.MigrationEngineUseCase
import com.graphcode.ports.inbound.MigrationFileContext
import com.graphcode.ports.inbound.MigrationLoteResult
import com.graphcode.ports.outbound.MigrationQueryPort
import org.springframework.stereotype.Service
import kotlin.system.measureTimeMillis

@Service
class MigrationEngineService(
    private val migrationQueryPort: MigrationQueryPort
) : MigrationEngineUseCase {

    override suspend fun getMigrationContext(filePaths: List<String>): MigrationLoteResult {
        var sortedContexts: List<MigrationFileContext>
        
        val timeInMillis = measureTimeMillis {
            val contexts = migrationQueryPort.getSubgraphContext(filePaths)
            sortedContexts = com.graphcode.domain.services.TopologicalSortService.sort(contexts)
        }
        
        return MigrationLoteResult(
            executionTimeMs = timeInMillis,
            topologicalOrder = sortedContexts
        )
    }
    
    }
