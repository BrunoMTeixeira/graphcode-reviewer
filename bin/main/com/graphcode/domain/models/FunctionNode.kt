package com.graphcode.domain.models

data class FunctionNode(
    val name: String,
    val signature: String,
    val returnType: String,
    val complexity: Int,
    val position: Position? = null
) {
    val calledFunctions: MutableList<String> = mutableListOf()
}
