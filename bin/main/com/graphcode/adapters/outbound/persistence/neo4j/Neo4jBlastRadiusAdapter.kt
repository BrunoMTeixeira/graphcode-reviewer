package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.ports.inbound.BlastRadiusPath
import com.graphcode.ports.inbound.BlastRadiusResult
import com.graphcode.ports.outbound.BlastRadiusQueryPort
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.stereotype.Component
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Component
class Neo4jBlastRadiusAdapter(
    private val neo4jClient: Neo4jClient
) : BlastRadiusQueryPort {

    override suspend fun findImpactPaths(qualifiedName: String, maxDepth: Int): BlastRadiusResult = withContext(Dispatchers.IO) {
        val query = """
            MATCH (start) WHERE start.qualifiedName = $qualifiedName OR start.name = $qualifiedName
            CALL apoc.path.spanningTree(start, {
                relationshipFilter: "<CALLS|<CONTAINS",
                minLevel: 1,
                maxLevel: $maxDepth
            })
            YIELD path
            RETURN [n in nodes(path) | coalesce(n.qualifiedName, n.name, n.filePath, "Unknown")] AS nodeNames, length(path) as depth
        """.trimIndent()

        val paths = mutableListOf<BlastRadiusPath>()
        var pathIdCounter = 1

        neo4jClient.query(query)
            .bind(qualifiedName).to("qualifiedName")
            .bind(maxDepth).to("maxDepth")
            .fetch()
            .all()
            .forEach { record ->
                val nodeNamesObj = record["nodeNames"] as? List<*> ?: emptyList<Any>()
                val nodeNames = nodeNamesObj.map { it.toString() }
                
                val depth = (record["depth"] as? Number)?.toInt() ?: 1
                
                val severity = when (depth) {
                    1 -> "ALTA"
                    2 -> "MÉDIA"
                    else -> "BAIXA"
                }

                paths.add(
                    BlastRadiusPath(
                        pathId = pathIdCounter++,
                        nodes = nodeNames,
                        severity = severity
                    )
                )
            }

        val uniqueImpacted = paths.flatMap { it.nodes }.toSet().size

        BlastRadiusResult(
            targetNode = qualifiedName,
            totalImpactedNodes = uniqueImpacted,
            paths = paths
        )
    }
}
