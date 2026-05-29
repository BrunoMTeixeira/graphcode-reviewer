package com.graphcode.domain.models

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.schema.CompositeProperty
import java.util.UUID

@Node("Test")
data class TestNode(
    @Id val id: String = UUID.randomUUID().toString(),
    val type: String,
    val suiteName: String,
    val isIgnored: Boolean,

    @CompositeProperty
    val position: Position? = null,

    @Relationship(type = "CALLS", direction = Relationship.Direction.OUTGOING)
    val testFunctions: MutableList<FunctionNode> = mutableListOf()
)
