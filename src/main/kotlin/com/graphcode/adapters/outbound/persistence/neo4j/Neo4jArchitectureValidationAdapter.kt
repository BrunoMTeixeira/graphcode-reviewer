package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.domain.models.config.GraphCodeConfig
import com.graphcode.ports.outbound.ArchitectureValidationQueryPort
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.stereotype.Component
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

@Component
class Neo4jArchitectureValidationAdapter(
    private val neo4jClient: Neo4jClient
) : ArchitectureValidationQueryPort {

    private val logger = LoggerFactory.getLogger(Neo4jArchitectureValidationAdapter::class.java)

    override suspend fun getViolations(config: GraphCodeConfig?): Pair<List<String>, Int> = withContext(Dispatchers.IO) {
        val violations = mutableListOf<String>()
        var effortScore = 0

        if (config == null || config.architecture.rules.isEmpty()) {
            logger.warn("Nenhuma regra de arquitetura definida. A usar validacao hexagonal padrao.")
            val query = """
                MATCH (domain:Class)-[:CONTAINS*0..1]->(func:Function)-[:CALLS]->(targetFunc:Function)<-[:CONTAINS*0..1]-(target:Class)
                MATCH (fileD:File)-[:CONTAINS]->(domain)
                MATCH (fileT:File)-[:CONTAINS]->(target)
                WHERE fileD.filePath CONTAINS "/domain/" 
                  AND (fileT.filePath CONTAINS "/ports/" OR fileT.filePath CONTAINS "/adapters/" OR fileT.filePath CONTAINS "/application/")
                RETURN DISTINCT domain.name AS source, target.name AS target
            """.trimIndent()

            val records = neo4jClient.query(query).fetch().all()
            for (record in records) {
                val s = record["source"]?.toString() ?: "Unknown"
                val t = record["target"]?.toString() ?: "Unknown"
                violations.add("Hexagonal Drift: A classe de Dominio '' chama '' (Adaptador/Porta), quebrando o isolamento do core.")
                effortScore += 5
            }
        } else {
            for (rule in config.architecture.rules) {
                val sourceLayer = config.architecture.layers[rule.source] ?: continue
                for (forbidden in rule.forbiddenDependencies) {
                    val forbiddenLayer = config.architecture.layers[forbidden] ?: continue
                    
                    for (srcPath in sourceLayer) {
                        for (forbidPath in forbiddenLayer) {
                            val query = """
                                MATCH (srcC:Class)-[:CONTAINS*0..1]->(f1:Function)-[:CALLS]->(f2:Function)<-[:CONTAINS*0..1]-(tgtC:Class)
                                MATCH (fSrc:File)-[:CONTAINS]->(srcC)
                                MATCH (fTgt:File)-[:CONTAINS]->(tgtC)
                                WHERE fSrc.filePath CONTAINS srcPath 
                                  AND fTgt.filePath CONTAINS forbidPath
                                RETURN DISTINCT srcC.name AS source, tgtC.name AS target
                            """.trimIndent()
                            
                            val records = neo4jClient.query(query)
                                .bindAll(mapOf("srcPath" to srcPath, "forbidPath" to forbidPath))
                                .fetch().all()
                                
                            for (record in records) {
                                val s = record["source"]?.toString() ?: "Unknown"
                                val t = record["target"]?.toString() ?: "Unknown"
                                violations.add("Arquitetura C4 Drift: '${rule.source}' ($s) depende ilegalmente de '$forbidden' ($t).")
                                effortScore += 5
                            }
                        }
                    }
                }
            }
        }
        Pair(violations, effortScore)
    }
}
