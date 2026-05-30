package com.graphcode.ports.outbound

import com.graphcode.ports.inbound.DeadCodeResult

interface DeadCodeQueryPort {
    suspend fun findIsolatedNodesAndCycles(): List<DeadCodeResult>
}
