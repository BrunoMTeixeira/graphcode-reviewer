package com.graphcode.domain.models

import java.util.UUID

/**
 * Nó de domínio que representa um endpoint HTTP (REST, GraphQL, etc.)
 * extraído do código-fonte. Puro — sem dependências de infraestrutura.
 */
data class EndpointNode(
    val id: String = UUID.randomUUID().toString(),
    val method: String,
    val path: String,
    val handlerName: String,
    val position: Position? = null,
    var handler: FunctionNode? = null
)
