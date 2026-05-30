package com.graphcode.adapters.outbound.persistence.neo4j.entities

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.schema.CompositeProperty

@Node("File")
data class FileEntity(
    @Id val filePath: String,
    val contentHash: String,
    val sizeBytes: Long,
    
    @CompositeProperty(prefix = "pos.")
    val position: Map<String, Long>? = null
) {
    @Relationship(type = "CONTAINS")
    val classes = mutableListOf<ClassEntity>()

    @Relationship(type = "CONTAINS")
    val functions = mutableListOf<FunctionEntity>()
}

@Node("Class")
data class ClassEntity(
    @Id val id: String,
    val name: String,
    val qualifiedName: String,
    val isExported: Boolean,
    
    @CompositeProperty(prefix = "pos.")
    val position: Map<String, Long>? = null
) {
    @Relationship(type = "CONTAINS")
    val functions = mutableListOf<FunctionEntity>()
}

@Node("Function")
data class FunctionEntity(
    @Id val id: String,
    val name: String,
    val signature: String,
    val returnType: String,
    val complexity: Int,
    
    @CompositeProperty(prefix = "pos.")
    val position: Map<String, Long>? = null
) {
    val calledFunctions = mutableListOf<String>()
}

@Node("Endpoint")
data class EndpointEntity(
    @Id val id: String,
    val method: String,
    val path: String,
    val handlerName: String,

    @CompositeProperty(prefix = "pos.")
    val position: Map<String, Long>? = null
) {
    @Relationship(type = "HANDLER", direction = Relationship.Direction.OUTGOING)
    var handler: FunctionEntity? = null
}

@Node("DataModel")
data class DataModelEntity(
    @Id val id: String,
    val name: String,
    val fields: List<String>,
    val schemaType: String
)

@Node("Test")
data class TestEntity(
    @Id val id: String,
    val type: String,
    val suiteName: String,
    val isIgnored: Boolean,

    @CompositeProperty(prefix = "pos.")
    val position: Map<String, Long>? = null
) {
    @Relationship(type = "CALLS", direction = Relationship.Direction.OUTGOING)
    val testFunctions: MutableList<FunctionEntity> = mutableListOf()
}
