package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.domain.models.FileNode
import com.graphcode.ports.outbound.SaveGraphRepositoryPort
import org.springframework.stereotype.Component

@Component
class Neo4jSaveGraphAdapter(
    private val repository: FileEntityRepository
) : SaveGraphRepositoryPort {

    override fun saveFileGraph(fileNode: FileNode): FileNode {
        val existingEntity = repository.findByFilePath(fileNode.filePath)
        
        if (existingEntity != null) {
            if (existingEntity.contentHash == fileNode.contentHash) {
                return fileNode // Hashing Incremental. Sem alterações, return original.
            } else {
                repository.delete(existingEntity)
            }
        }
        
        val newEntity = Neo4jMapper.toEntity(fileNode)
        val savedEntity = repository.save(newEntity)
        return Neo4jMapper.toDomain(savedEntity)
    }

    override fun findByFilePath(filePath: String): FileNode? {
        val entity = repository.findByFilePath(filePath)
        return entity?.let { Neo4jMapper.toDomain(it) }
    }
}
