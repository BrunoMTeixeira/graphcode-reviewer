package com.graphcode.ports.outbound

import com.graphcode.ports.inbound.MigrationFileContext

interface MigrationQueryPort {
    suspend fun getSubgraphContext(filePaths: List<String>): List<MigrationFileContext>
}
