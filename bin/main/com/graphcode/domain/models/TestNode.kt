package com.graphcode.domain.models

import java.util.UUID

/**
 * Nó de domínio que representa uma suite de testes
 * extraída do código-fonte. Puro — sem dependências de infraestrutura.
 */
data class TestNode(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val suiteName: String,
    val isIgnored: Boolean,
    val position: Position? = null
) {
    val testFunctions: MutableList<FunctionNode> = mutableListOf()
}
