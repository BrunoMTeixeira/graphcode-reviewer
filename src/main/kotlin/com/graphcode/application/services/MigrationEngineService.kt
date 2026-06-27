package com.graphcode.application.services

import com.graphcode.ports.inbound.MigrationEngineUseCase
import com.graphcode.ports.inbound.MigrationFileContext
import com.graphcode.ports.inbound.MigrationLoteResult
import com.graphcode.ports.outbound.MigrationQueryPort
import org.springframework.stereotype.Service
import kotlin.system.measureTimeMillis

@Service
class MigrationEngineService(
    private val migrationQueryPort: MigrationQueryPort
) : MigrationEngineUseCase {

    override suspend fun getMigrationContext(filePaths: List<String>): MigrationLoteResult {
        var sortedContexts: List<MigrationFileContext>
        
        val timeInMillis = measureTimeMillis {
            val contexts = migrationQueryPort.getSubgraphContext(filePaths)
            sortedContexts = topologicalSort(contexts)
        }
        
        return MigrationLoteResult(
            executionTimeMs = timeInMillis,
            topologicalOrder = sortedContexts
        )
    }
    
    // Algoritmo de Kahn para Topological Sort
    private fun topologicalSort(contexts: List<MigrationFileContext>): List<MigrationFileContext> {
        val inDegree = mutableMapOf<String, Int>()
        val graph = mutableMapOf<String, MutableList<String>>()
        val contextMap = contexts.associateBy { it.filePath }
        
        // Inicializar
        contexts.forEach { ctx ->
            inDegree[ctx.filePath] = 0
            graph[ctx.filePath] = mutableListOf()
        }
        
        // Construir grafo (A depende de B -> B precisa ser refatorado primeiro)
        // Logo, a aresta vai de B (dependência) para A (dependente)
        contexts.forEach { ctx ->
            ctx.dependencies.forEach { dep ->
                if (graph.containsKey(dep)) {
                    graph[dep]?.add(ctx.filePath)
                    inDegree[ctx.filePath] = (inDegree[ctx.filePath] ?: 0) + 1
                }
            }
        }
        
        val queue = ArrayDeque<String>()
        inDegree.filterValues { it == 0 }.keys.forEach { queue.add(it) }
        
        val sortedList = mutableListOf<MigrationFileContext>()
        
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            contextMap[current]?.let { sortedList.add(it) }
            
            graph[current]?.forEach { neighbor ->
                inDegree[neighbor] = (inDegree[neighbor] ?: 0) - 1
                if (inDegree[neighbor] == 0) {
                    queue.add(neighbor)
                }
            }
        }
        
        // Se houver ciclos, os nós restantes não entram no sortedList. 
        // Adicionamos os restantes no final por segurança, caso a topologia tenha ciclos irremediáveis.
        if (sortedList.size != contexts.size) {
            val missing = contexts.filter { !sortedList.contains(it) }
            sortedList.addAll(missing)
        }
        
        return sortedList
    }
}
