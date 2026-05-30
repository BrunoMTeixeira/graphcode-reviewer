package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.ports.inbound.MigrationFileContext
import com.graphcode.ports.outbound.MigrationQueryPort
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.stereotype.Component
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Component
class Neo4jMigrationAdapter(private val neo4jClient: Neo4jClient) : MigrationQueryPort {
    
    override suspend fun getSubgraphContext(filePaths: List<String>): List<MigrationFileContext> = withContext(Dispatchers.IO) {
        val query = """
            MATCH (f:`File`)
            WHERE f.filePath IN $filePaths
            OPTIONAL MATCH (f)-[:CONTAINS]->(elem)
            OPTIONAL MATCH (elem)-[:DEPENDS_ON|CALLS]->(depElem)<-[:CONTAINS]-(depFile:`File`)
            WHERE depFile.filePath IN $filePaths AND f <> depFile
            RETURN f.filePath AS filePath, 
                   count(DISTINCT elem) AS totalElements, 
                   collect(DISTINCT depFile.filePath) AS deps
        """.trimIndent()
        
        val results = mutableListOf<MigrationFileContext>()
        
        neo4jClient.query(query)
            .bind(filePaths).to("filePaths")
            .fetch()
            .all()
            .forEach { record ->
                val filePath = record["filePath"].toString()
                val totalElements = (record["totalElements"] as? Number)?.toInt() ?: 0
                val depsList = record["deps"] as? List<*> ?: emptyList<Any>()
                val deps = depsList.map { it.toString() }
                
                val content = try {
                    File(filePath).readText()
                } catch (e: Exception) {
                    "Erro ao ler ficheiro: ${e.message}"
                }
                
                results.add(
                    MigrationFileContext(
                        filePath = filePath,
                        fileContent = content,
                        dependencies = deps,
                        summary = "Ficheiro contém $totalElements elementos declarados (classes/funções)."
                    )
                )
            }
            
        results
    }
}
