package com.graphcode.domain.models

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.schema.CompositeProperty
import java.util.UUID

@Node("File")
data class FileNode(
    @Id val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val contentHash: String,
    val sizeBytes: Long,
    
    @org.springframework.data.annotation.Transient
    val position: Position? = null,
    
    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    val classes: MutableList<ClassNode> = mutableListOf(),

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    val functions: MutableList<FunctionNode> = mutableListOf()
)
