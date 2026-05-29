package com.graphcode.ports.outbound

import com.graphcode.domain.models.FileNode

/**
 * Porta de Saída (Outbound Port) para persistência do Grafo de Código.
 * Esta interface pertence à camada de Aplicação e não sabe nada sobre Neo4j ou bases de dados.
 */
interface SaveGraphRepositoryPort {
    /**
     * Guarda ou atualiza o subgrafo correspondente a um ficheiro analisado.
     * O armazenamento deve ser transacional e seguir uma lógica "upsert" baseada no path.
     */
    fun saveFileGraph(fileNode: FileNode): FileNode
    
    /**
     * Procura um ficheiro guardado pelo seu caminho exato.
     */
    fun findByFilePath(filePath: String): FileNode?
}
