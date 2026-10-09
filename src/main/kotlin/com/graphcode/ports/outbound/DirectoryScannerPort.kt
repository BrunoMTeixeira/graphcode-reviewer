package com.graphcode.ports.outbound

import com.graphcode.domain.models.ScannedFile

interface DirectoryScannerPort {
    fun scanDirectory(directoryPath: String): List<ScannedFile>
}

