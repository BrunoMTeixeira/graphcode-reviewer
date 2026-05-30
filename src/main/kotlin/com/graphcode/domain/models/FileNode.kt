package com.graphcode.domain.models

import java.util.UUID

data class FileNode(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val contentHash: String,
    val sizeBytes: Long,
    val position: Position? = null
) {
    val classes: MutableList<ClassNode> = mutableListOf()
    val functions: MutableList<FunctionNode> = mutableListOf()
}
