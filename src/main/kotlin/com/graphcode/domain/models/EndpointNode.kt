package com.graphcode.domain.models

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.schema.CompositeProperty
import java.util.UUID

@Node("Endpoint")
data class EndpointNode(
    @Id val id: String = UUID.randomUUID().toString(),
    val method: String,
    val path: String,
    val handlerName: String,

    @CompositeProperty
    val position: Position? = null,

    @Relationship(type = "HANDLER", direction = Relationship.Direction.OUTGOING)
    var handler: FunctionNode? = null
)
