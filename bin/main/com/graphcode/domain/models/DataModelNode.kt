package com.graphcode.domain.models

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import java.util.UUID

@Node("DataModel")
data class DataModelNode(
    @Id val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fields: List<String>,
    val schemaType: String
)
