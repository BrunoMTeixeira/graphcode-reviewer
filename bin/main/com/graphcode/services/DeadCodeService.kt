package com.graphcode.services

import com.graphcode.ports.inbound.DeadCodeResult
import com.graphcode.ports.inbound.DeadCodeUseCase
import com.graphcode.ports.outbound.DeadCodeQueryPort
import org.springframework.stereotype.Service

@Service
class DeadCodeService(
    private val deadCodeQueryPort: DeadCodeQueryPort
) : DeadCodeUseCase {

    override suspend fun findDeadCodeAndCycles(): List<DeadCodeResult> {
        return deadCodeQueryPort.findIsolatedNodesAndCycles()
    }
}
