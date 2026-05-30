package com.graphcode.domain.models

import java.util.UUID

data class ClassNode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val qualifiedName: String,
    val isExported: Boolean,
    val position: Position? = null
) {
    val functions: MutableList<FunctionNode> = mutableListOf()
}
