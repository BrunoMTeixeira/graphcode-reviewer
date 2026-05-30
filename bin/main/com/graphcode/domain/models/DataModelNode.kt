package com.graphcode.domain.models

import java.util.UUID

/**
 * Nó de domínio que representa um modelo de dados (Entity, DTO, Schema)
 * extraído do código-fonte. Puro — sem dependências de infraestrutura.
 */
data class DataModelNode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fields: List<String>,
    val schemaType: String
)
