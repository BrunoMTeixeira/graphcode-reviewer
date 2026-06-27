package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.adapters.outbound.persistence.neo4j.entities.FileEntity
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

import org.springframework.data.neo4j.repository.query.Query

@Repository
interface FileEntityRepository : Neo4jRepository<FileEntity, String> {
    fun findByFilePath(filePath: String): FileEntity?
    
    @Query("MATCH (n) DETACH DELETE n")
    fun clearDatabase()
    
    @Query("MATCH (f1:Function) UNWIND f1.calledFunctions AS calledName MATCH (f2:Function {name: calledName}) MERGE (f1)-[:CALLS]->(f2)")
    fun linkFunctionCalls()
}
