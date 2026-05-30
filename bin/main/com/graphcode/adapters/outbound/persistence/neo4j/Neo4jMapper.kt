package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.domain.models.*
import com.graphcode.adapters.outbound.persistence.neo4j.entities.*

object Neo4jMapper {

    fun toEntity(domain: FileNode): FileEntity {
        val entity = FileEntity(
            filePath = domain.filePath,
            contentHash = domain.contentHash,
            sizeBytes = domain.sizeBytes,
            position = domain.position?.let { toEntity(it) }
        )
        entity.classes.addAll(domain.classes.map { toEntity(it) })
        entity.functions.addAll(domain.functions.map { toEntity(it) })
        return entity
    }

    private fun toEntity(domain: ClassNode): ClassEntity {
        val entity = ClassEntity(
            id = domain.id,
            name = domain.name,
            qualifiedName = domain.qualifiedName,
            isExported = domain.isExported,
            position = domain.position?.let { toEntity(it) }
        )
        entity.functions.addAll(domain.functions.map { toEntity(it) })
        return entity
    }

    private fun toEntity(domain: FunctionNode): FunctionEntity {
        val entity = FunctionEntity(
            id = java.util.UUID.randomUUID().toString(), // ou domain.id se tiver
            name = domain.name,
            signature = domain.signature,
            returnType = domain.returnType,
            complexity = domain.complexity,
            position = domain.position?.let { toEntity(it) }
        )
        entity.calledFunctions.addAll(domain.calledFunctions)
        return entity
    }

    private fun toEntity(domain: Position): Map<String, Long> {
        return mapOf(
            "startLine" to domain.startLine,
            "endLine" to domain.endLine,
            "startByte" to domain.startByte,
            "endByte" to domain.endByte
        )
    }

    // --- REVERSE (Entity -> Domain) ---

    fun toDomain(entity: FileEntity): FileNode {
        val domain = FileNode(
            filePath = entity.filePath,
            contentHash = entity.contentHash,
            sizeBytes = entity.sizeBytes,
            position = entity.position?.let { toDomain(it) }
        )
        domain.classes.addAll(entity.classes.map { toDomain(it) })
        domain.functions.addAll(entity.functions.map { toDomain(it) })
        return domain
    }

    private fun toDomain(entity: ClassEntity): ClassNode {
        val domain = ClassNode(
            id = entity.id,
            name = entity.name,
            qualifiedName = entity.qualifiedName,
            isExported = entity.isExported,
            position = entity.position?.let { toDomain(it) }
        )
        domain.functions.addAll(entity.functions.map { toDomain(it) })
        return domain
    }

    private fun toDomain(entity: FunctionEntity): FunctionNode {
        val domain = FunctionNode(
            name = entity.name,
            signature = entity.signature,
            returnType = entity.returnType,
            complexity = entity.complexity,
            position = entity.position?.let { toDomain(it) }
        )
        domain.calledFunctions.addAll(entity.calledFunctions)
        return domain
    }

    private fun toDomain(entity: Map<String, Long>): Position {
        return Position(
            startLine = entity["startLine"] ?: 0,
            endLine = entity["endLine"] ?: 0,
            startByte = entity["startByte"] ?: 0,
            endByte = entity["endByte"] ?: 0
        )
    }
}
