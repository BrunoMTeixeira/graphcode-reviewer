package com.graphcode.domain.services

import com.graphcode.ports.inbound.MigrationFileContext

object TopologicalSortService {

    fun sort(contexts: List<MigrationFileContext>): List<MigrationFileContext> {
        val inDegree = mutableMapOf<String, Int>()
        val graph = mutableMapOf<String, MutableList<String>>()
        val contextMap = contexts.associateBy { it.filePath }
        
        contexts.forEach { ctx ->
            inDegree[ctx.filePath] = 0
            graph[ctx.filePath] = mutableListOf()
        }
        
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
        
        if (sortedList.size != contexts.size) {
            val missing = contexts.filter { !sortedList.contains(it) }
            sortedList.addAll(missing)
        }
        
        return sortedList
    }
}
