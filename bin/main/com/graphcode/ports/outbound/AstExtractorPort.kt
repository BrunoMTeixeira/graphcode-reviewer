package com.graphcode.ports.outbound

import com.graphcode.domain.models.FileNode

/**
 * Porta de Saída (Outbound Port) que define o contrato para extração de AST.
 * Isola a lógica de negócio (Application/Domain) da biblioteca concreta que 
 * faz o parsing (Tree-sitter).
 */
interface AstExtractorPort {
    /**
     * Lê o conteúdo de um ficheiro e extrai o Grafo de Propriedades de Código.
     * 
     * @param fileContent O código-fonte em formato texto.
     * @param filePath O caminho físico do ficheiro no repositório.
     * @return Um FileNode contendo a hierarquia de nós extraídos (Classes, Funções, etc).
     */
    fun extract(fileContent: String, filePath: String): FileNode
}
