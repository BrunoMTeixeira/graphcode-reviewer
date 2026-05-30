package com.graphcode.adapters.inbound.mcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.ServerSocket
import java.net.SocketException
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Component
class McpTcpServer {
    private val logger = LoggerFactory.getLogger(McpTcpServer::class.java)
    private var serverSocket: ServerSocket? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    @PostConstruct
    fun startServer() {
        serverScope.launch {
            try {
                serverSocket = ServerSocket(8081)
                logger.info("MCP TCP Server escutando na porta 8081")
                
                acceptConnections()
            } catch (e: Exception) {
                logger.error("Falha ao iniciar MCP TCP Server", e)
            }
        }
    }

    private suspend fun acceptConnections() = coroutineScope {
        val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()
        
        while (isActive) {
            try {
                val clientSocket = withContext(Dispatchers.IO) {
                    serverSocket?.accept()
                }
                
                if (clientSocket != null) {
                    // Requisito: concorrência com async/awaitAll
                    val job = async {
                        McpSession(clientSocket).handle()
                    }
                    jobs.add(job)
                    
                    // Limpeza periódica de jobs terminados
                    jobs.removeAll { it.isCompleted }
                }
            } catch (e: SocketException) {
                if (!isActive) break
                logger.error("Erro no accept do server socket", e)
            } catch (e: Exception) {
                logger.error("Erro inesperado no servidor MCP", e)
            }
        }
        
        // Assegura que todas as sessões fecham
        jobs.awaitAll()
    }

    @PreDestroy
    fun stopServer() {
        logger.info("Encerrando MCP TCP Server...")
        serverScope.cancel()
        serverSocket?.close()
    }
}
