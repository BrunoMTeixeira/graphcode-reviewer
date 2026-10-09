package com.graphcode.application.config

import org.yaml.snakeyaml.Yaml
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import com.graphcode.domain.models.config.*



@Component
class GraphCodeConfigLoader {

    private val logger = LoggerFactory.getLogger(GraphCodeConfigLoader::class.java)

    fun loadConfig(workspacePath: String): GraphCodeConfig {
        val configFile = File("$workspacePath/.graphcode.yml")
        if (!configFile.exists()) {
            logger.warn("Ficheiro .graphcode.yml não encontrado no caminho $workspacePath. A usar configurações por defeito.")
            return GraphCodeConfig()
        }

        return try {
            val yaml = Yaml()
            val inputStream = FileInputStream(configFile)
            val map = yaml.load<Map<String, Any>>(inputStream)

            val config = GraphCodeConfig()
            
            if (map.containsKey("ignoredPaths")) {
                config.ignoredPaths = map["ignoredPaths"] as List<String>
            }

            if (map.containsKey("architecture")) {
                val archMap = map["architecture"] as Map<String, Any>
                if (archMap.containsKey("type")) {
                    config.architecture.type = archMap["type"].toString()
                }
                if (archMap.containsKey("layers")) {
                    config.architecture.layers = archMap["layers"] as Map<String, List<String>>
                }
                if (archMap.containsKey("rules")) {
                    val rulesList = archMap["rules"] as List<Map<String, Any>>
                    config.architecture.rules = rulesList.map { ruleMap ->
                        ArchitectureRule(
                            source = ruleMap["source"].toString(),
                            forbiddenDependencies = ruleMap["forbiddenDependencies"] as List<String>
                        )
                    }
                }
            }
            logger.info("Configuração .graphcode.yml carregada com sucesso.")
            config
        } catch (e: Exception) {
            logger.error("Erro ao fazer parse do .graphcode.yml. A usar configurações por defeito.", e)
            GraphCodeConfig()
        }
    }
}
