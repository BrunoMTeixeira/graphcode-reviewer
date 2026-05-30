package com.graphcode.adapters.outbound

import com.graphcode.domain.models.FileNode
import com.graphcode.ports.outbound.SaveGraphRepositoryPort
import org.springframework.stereotype.Component

/**
 * Adaptador de Saída que implementa a Porta da camada de Aplicação utilizando
 * o Repositório SDN por trás (Isolamento Hexagonal).
 */
@Component
class Neo4jSaveGraphAdapter(
    private val repository: FileNodeRepository
) : SaveGraphRepositoryPort {

    override fun saveFileGraph(fileNode: FileNode): FileNode {
        // Algoritmo Incremental (Issue #24)
        val existingNode = repository.findByFilePath(fileNode.filePath)
        
        if (existingNode != null) {
            if (existingNode.contentHash == fileNode.contentHash) {
                // Ficheiro não foi modificado (Hash idêntico). Saltar a persistência!
                return existingNode
            } else {
                // Ficheiro modificado. Apagar o subgrafo obsoleto antes de gravar o novo.
                repository.delete(existingNode)
            }
        }
        
        // Gravar o novo grafo via cascata
        return repository.save(fileNode)
    }

    override fun findByFilePath(filePath: String): FileNode? {
        return repository.findByFilePath(filePath)
    }
}
