package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.adapters.outbound.persistence.neo4j.entities.FileEntity
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

@Repository
interface FileEntityRepository : Neo4jRepository<FileEntity, String> {
    fun findByFilePath(filePath: String): FileEntity?
}
