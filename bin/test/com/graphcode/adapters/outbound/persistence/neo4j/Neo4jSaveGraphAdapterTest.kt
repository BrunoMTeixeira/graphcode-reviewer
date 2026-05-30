package com.graphcode.adapters.outbound.persistence.neo4j

import com.graphcode.domain.models.FileNode
import com.graphcode.adapters.outbound.persistence.neo4j.entities.FileEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Neo4jSaveGraphAdapterTest {

    private val repository: FileEntityRepository = mockk(relaxed = true)
    private val adapter = Neo4jSaveGraphAdapter(repository)

    @Test
    fun `should save new file when it does not exist in repository`() {
        val newNode = FileNode(filePath = "src/Test.kt", contentHash = "hash123", sizeBytes = 100)
        every { repository.findByFilePath("src/Test.kt") } returns null

        adapter.saveFileGraph(newNode)

        verify(exactly = 1) { repository.save(any()) }
        verify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `should skip saving when file exists and contentHash is identical`() {
        val existingEntity = FileEntity(filePath = "src/Test.kt", contentHash = "hash123", sizeBytes = 100)
        val incomingNode = FileNode(filePath = "src/Test.kt", contentHash = "hash123", sizeBytes = 100)
        
        every { repository.findByFilePath("src/Test.kt") } returns existingEntity

        val result = adapter.saveFileGraph(incomingNode)

        assertEquals(incomingNode.filePath, result.filePath)
        verify(exactly = 0) { repository.save(any()) } 
        verify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `should delete old file and save new file when contentHash is different`() {
        val oldEntity = FileEntity(filePath = "src/Test.kt", contentHash = "oldHash", sizeBytes = 100)
        val updatedNode = FileNode(filePath = "src/Test.kt", contentHash = "newHash", sizeBytes = 105)
        
        every { repository.findByFilePath("src/Test.kt") } returns oldEntity

        adapter.saveFileGraph(updatedNode)

        verify(exactly = 1) { repository.delete(oldEntity) }
        verify(exactly = 1) { repository.save(any()) }
    }
}
