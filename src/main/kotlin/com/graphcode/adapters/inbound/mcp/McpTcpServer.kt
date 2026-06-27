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

import com.graphcode.ports.inbound.BlastRadiusUseCase
import com.graphcode.ports.inbound.DeadCodeUseCase
import com.graphcode.ports.inbound.MigrationEngineUseCase
import com.graphcode.ports.inbound.NlToCypherUseCase

@Component
class McpTcpServer(
    private val blastRadiusUseCase: BlastRadiusUseCase,
    private val deadCodeUseCase: DeadCodeUseCase,
    private val migrationEngineUseCase: MigrationEngineUseCase,
    private val nlToCypherUseCase: NlToCypherUseCase
) {
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
                    val job = async {
                        McpSession(
                            socket = clientSocket,
                            blastRadiusUseCase = blastRadiusUseCase,
                            deadCodeUseCase = deadCodeUseCase,
                            migrationEngineUseCase = migrationEngineUseCase,
                            nlToCypherUseCase = nlToCypherUseCase
                        ).handle()
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
