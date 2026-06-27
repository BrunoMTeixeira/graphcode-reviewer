package com.graphcode.application.services

import com.graphcode.application.config.GraphCodeConfig
import com.graphcode.ports.inbound.MigrationResult
import com.graphcode.ports.inbound.MigrationValidationUseCase
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service
class MigrationValidationService(
    private val neo4jClient: Neo4jClient
) : MigrationValidationUseCase {

    private val logger = LoggerFactory.getLogger(MigrationValidationService::class.java)

    override suspend fun assessMigrationEffort(config: GraphCodeConfig?): MigrationResult = withContext(Dispatchers.IO) {
        val violations = mutableListOf<String>()
        var effortScore = 0

        if (config == null || config.architecture.rules.isEmpty()) {
            logger.warn("Nenhuma regra de arquitetura definida. A usar validação hexagonal padrão.")
            // Default Hexagonal validation: Domain cannot depend on Ports/Adapters/App
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
                violations.add("Hexagonal Drift: A classe de Domínio '$s' chama '$t' (Adaptador/Porta), quebrando o isolamento do core.")
                effortScore += 5
            }
        } else {
            // Validate based on config
            for (rule in config.architecture.rules) {
                val sourceLayer = config.architecture.layers[rule.source] ?: continue
                for (forbidden in rule.forbiddenDependencies) {
                    val forbiddenLayer = config.architecture.layers[forbidden] ?: continue
                    
                    // Simple path checking for custom rules
                    for (srcPath in sourceLayer) {
                        for (forbidPath in forbiddenLayer) {
                            val query = """
                                MATCH (srcC:Class)-[:CONTAINS*0..1]->(f1:Function)-[:CALLS]->(f2:Function)<-[:CONTAINS*0..1]-(tgtC:Class)
                                MATCH (fSrc:File)-[:CONTAINS]->(srcC)
                                MATCH (fTgt:File)-[:CONTAINS]->(tgtC)
                                WHERE fSrc.filePath CONTAINS ${'$'}srcPath 
                                  AND fTgt.filePath CONTAINS ${'$'}forbidPath
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

        MigrationResult(
            totalViolations = violations.size,
            effortScore = effortScore,
            violations = violations,
            recommendation = if (violations.isEmpty()) "A arquitetura está limpa e consistente!" else "Recomenda-se refatorar as dependências invertidas para usar Interfaces (Ports)."
        )
    }
}
