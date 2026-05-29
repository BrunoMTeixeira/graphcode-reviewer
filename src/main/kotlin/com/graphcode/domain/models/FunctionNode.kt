package com.graphcode.domain.models

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.schema.CompositeProperty
import java.util.UUID

@Node("Function")
data class FunctionNode(
    @Id val id: String = UUID.randomUUID().toString(),
    val name: String,
    val signature: String,
    val returnType: String,
    val complexity: Int,

    @CompositeProperty
    val position: Position? = null,

    @Relationship(type = "CALLS", direction = Relationship.Direction.OUTGOING)
    val calledFunctions: MutableList<FunctionNode> = mutableListOf()
)
