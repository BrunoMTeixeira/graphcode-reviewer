package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.ports.inbound.DeadCodeResult
import com.graphcode.ports.outbound.DeadCodeQueryPort
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.stereotype.Component
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Component
class Neo4jDeadCodeAdapter(private val neo4jClient: Neo4jClient) : DeadCodeQueryPort {
    
    override suspend fun findIsolatedNodesAndCycles(): List<DeadCodeResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DeadCodeResult>()
        
        // 1. Detectar Código Morto (Funções ou Classes não referenciadas)
        val deadCodeQuery = """
            MATCH (file:File)-[:CONTAINS*1..2]->(n)
            WHERE (n:`Function` OR n:`Class`)
              AND NOT ()-[:CALLS|DEPENDS_ON]->(n)
              AND NOT n.name CONTAINS "Test"
              AND NOT n.name IN ["main", "run", "startServer", "stopServer", "execute", "scanDirectory", "extract"]
              AND NOT file.filePath CONTAINS "/ports/"
              AND NOT (n:`Function` AND n.complexity = 1 AND (n.`pos.endLine` - n.`pos.startLine`) = 0)
            RETURN coalesce(n.qualifiedName, n.name, "Unknown") AS name, 
                   file.filePath AS fileLoc
        """.trimIndent()

        neo4jClient.query(deadCodeQuery).fetch().all().forEach { record ->
            results.add(
                DeadCodeResult(
                    qualifiedName = record["name"].toString(),
                    fileLocation = record["fileLoc"].toString(),
                    severity = com.graphcode.domain.models.Severity.MEDIA,
                    reason = "Unreachable code (0 incoming references)"
                )
            )
        }
        
        // 2. Detectar Ciclos Directos (A -> B -> A)
        val cyclesQuery = """
            MATCH (a)-[:CALLS|DEPENDS_ON]->(b)-[:CALLS|DEPENDS_ON]->(a)
            WHERE elementId(a) < elementId(b)
            RETURN coalesce(a.qualifiedName, a.name) AS nameA,
                   coalesce(b.qualifiedName, b.name) AS nameB
        """.trimIndent()
        
        neo4jClient.query(cyclesQuery).fetch().all().forEach { record ->
            val a = record["nameA"].toString()
            val b = record["nameB"].toString()
            results.add(
                DeadCodeResult(
                    qualifiedName = "$a <-> $b",
                    fileLocation = "Grafo",
                    severity = com.graphcode.domain.models.Severity.ALTA,
                    reason = "Circular dependency detected"
                )
            )
        }
        
        results
    }
}
