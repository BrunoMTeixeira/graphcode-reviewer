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
        // Aproveitamos a lógica ACID de gravação em cascata do Spring Data Neo4j
        // Ao gravar o FileNode, ele automaticamente grava as Classes e Funções associadas 
        // por causa das marcações @Relationship no nosso Domínio!
        return repository.save(fileNode)
    }

    override fun findByFilePath(filePath: String): FileNode? {
        return repository.findByFilePath(filePath)
    }
}
