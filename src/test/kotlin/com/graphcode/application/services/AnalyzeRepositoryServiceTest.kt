package com.graphcode.application.services

import com.graphcode.domain.models.FileNode
import com.graphcode.ports.outbound.AstExtractorPort
import com.graphcode.ports.outbound.DirectoryScannerPort
import com.graphcode.ports.outbound.SaveGraphRepositoryPort
import com.graphcode.ports.outbound.ScannedFile
import io.mockk.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AnalyzeRepositoryServiceTest {

    private val scannerPort = mockk<DirectoryScannerPort>()
    private val extractorPort = mockk<AstExtractorPort>()
    private val saveGraphPort = mockk<SaveGraphRepositoryPort>()

    private val service = AnalyzeRepositoryService(scannerPort, extractorPort, saveGraphPort)

    @Test
    fun `deve orquestrar scanner, extrator e persistencia na ordem correta`() {
        val dirPath = "/test/path"
        
        val file1 = ScannedFile("/test/path/File1.kt", "content1")
        val file2 = ScannedFile("/test/path/File2.kt", "content2")
        
        val fileNode1 = FileNode(filePath = file1.filePath, contentHash = "h1", sizeBytes = 1)
        val fileNode2 = FileNode(filePath = file2.filePath, contentHash = "h2", sizeBytes = 2)

        every { scannerPort.scanDirectory(dirPath) } returns listOf(file1, file2)
        every { extractorPort.extract(file1.content, file1.filePath) } returns fileNode1
        every { extractorPort.extract(file2.content, file2.filePath) } returns fileNode2
        every { saveGraphPort.saveFileGraph(any()) } returns fileNode1
        every { saveGraphPort.linkFunctionCalls() } just Runs

        // Action
        service.execute(dirPath)

        // Verifications
        verify(exactly = 1) { scannerPort.scanDirectory(dirPath) }
        verify(exactly = 1) { extractorPort.extract(file1.content, file1.filePath) }
        verify(exactly = 1) { extractorPort.extract(file2.content, file2.filePath) }
        verify(exactly = 1) { saveGraphPort.saveFileGraph(fileNode1) }
        verify(exactly = 1) { saveGraphPort.saveFileGraph(fileNode2) }
        verify(exactly = 1) { saveGraphPort.linkFunctionCalls() }
    }
    
    @Test
    fun `deve continuar a extracao mesmo se um ficheiro falhar`() {
        val dirPath = "/test/path"
        val file1 = ScannedFile("/test/path/File1.kt", "content1")
        val file2 = ScannedFile("/test/path/File2.kt", "content2")
        val fileNode2 = FileNode(filePath = file2.filePath, contentHash = "h2", sizeBytes = 2)

        every { scannerPort.scanDirectory(dirPath) } returns listOf(file1, file2)
        every { extractorPort.extract(file1.content, file1.filePath) } throws RuntimeException("Syntax Error")
        every { extractorPort.extract(file2.content, file2.filePath) } returns fileNode2
        every { saveGraphPort.saveFileGraph(fileNode2) } returns fileNode2
        every { saveGraphPort.linkFunctionCalls() } just Runs

        // Action
        service.execute(dirPath)

        // Verifications
        verify(exactly = 1) { extractorPort.extract(file1.content, file1.filePath) }
        verify(exactly = 1) { extractorPort.extract(file2.content, file2.filePath) }
        verify(exactly = 0) { saveGraphPort.saveFileGraph(match { it.filePath == file1.filePath }) }
        verify(exactly = 1) { saveGraphPort.saveFileGraph(fileNode2) }
        verify(exactly = 1) { saveGraphPort.linkFunctionCalls() }
    }
}
