package com.graphcode.domain.models.config

data class GraphCodeConfig(
    var ignoredPaths: List<String> = emptyList(),
    var architecture: ArchitectureConfig = ArchitectureConfig()
)

data class ArchitectureConfig(
    var type: String = "hexagonal",
    var layers: Map<String, List<String>> = emptyMap(),
    var rules: List<ArchitectureRule> = emptyList()
)

data class ArchitectureRule(
    var source: String = "",
    var forbiddenDependencies: List<String> = emptyList()
)
