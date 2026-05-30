package com.graphcode.adapters.outbound

import com.graphcode.domain.models.FileNode
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

/**
 * Adaptador de persistência do Spring Data Neo4j (SDN) para o nó FileNode.
 */
@Repository
interface FileNodeRepository : Neo4jRepository<FileNode, String> {
    /**
     * Procura um ficheiro pela sua path absoluta/relativa.
     * Usado no Algoritmo de Hashing Incremental.
     */
    fun findByFilePath(filePath: String): FileNode?
}
