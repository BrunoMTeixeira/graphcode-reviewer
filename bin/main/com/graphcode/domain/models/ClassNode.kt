package com.graphcode.domain.models

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.schema.CompositeProperty
import java.util.UUID

@Node("Class")
data class ClassNode(
    @Id val id: String = UUID.randomUUID().toString(),
    val name: String,
    val qualifiedName: String,
    val isExported: Boolean,

    @org.springframework.data.annotation.Transient
    val position: Position? = null,

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    val functions: MutableList<FunctionNode> = mutableListOf(),

    @Relationship(type = "IMPLEMENTS", direction = Relationship.Direction.OUTGOING)
    val implements: MutableList<ClassNode> = mutableListOf(),

    @Relationship(type = "PARENT_OF", direction = Relationship.Direction.OUTGOING)
    val childClasses: MutableList<ClassNode> = mutableListOf()
)
