package com.graphcode.ports.outbound

interface DirectoryScannerPort {
    fun scanDirectory(directoryPath: String): List<ScannedFile>
}

data class ScannedFile(
    val filePath: String,
    val content: String
)
