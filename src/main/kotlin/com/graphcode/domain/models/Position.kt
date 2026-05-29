package com.graphcode.domain.models

data class Position(
    val startLine: Long,
    val endLine: Long,
    val startByte: Long,
    val endByte: Long
)