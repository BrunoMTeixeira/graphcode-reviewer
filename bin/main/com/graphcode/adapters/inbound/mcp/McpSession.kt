package com.graphcode.adapters.inbound.mcp

import com.graphcode.adapters.inbound.mcp.protocol.JsonRpcRequest
import com.graphcode.adapters.inbound.mcp.protocol.JsonRpcResponse
import com.graphcode.adapters.inbound.mcp.protocol.JsonRpcError
import com.graphcode.adapters.inbound.mcp.protocol.RpcErrorCodes
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket
import org.slf4j.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class McpSession(private val socket: Socket) {
    private val logger = LoggerFactory.getLogger(McpSession::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    val sessionId: String = java.util.UUID.randomUUID().toString()

    suspend fun handle() = withContext(Dispatchers.IO) {
        logger.info("Nova sessão MCP iniciada: \$sessionId")
        socket.use { s ->
            val reader = BufferedReader(s.inputStream.reader(Charsets.UTF_8))
            val writer = PrintWriter(s.outputStream.writer(Charsets.UTF_8), true)

            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue
                
                try {
                    val request = json.decodeFromString<JsonRpcRequest>(line)
                    val response = processRequest(request)
                    if (response != null) {
                        writer.println(json.encodeToString(response))
                    }
                } catch (e: Exception) {
                    logger.error("Erro a processar request na sessão \$sessionId", e)
                    val errorResponse = JsonRpcResponse(
                        id = null,
                        error = JsonRpcError(
                            code = RpcErrorCodes.PARSE_ERROR,
                            message = "Parse error: \${e.message}"
                        )
                    )
                    writer.println(json.encodeToString(errorResponse))
                }
            }
        }
        logger.info("Sessão MCP encerrada: \$sessionId")
    }

    private suspend fun processRequest(request: JsonRpcRequest): JsonRpcResponse? {
        // Implementação inicial: handshake de inicialização MCP
        return when (request.method) {
            "initialize" -> {
                JsonRpcResponse(
                    id = request.id,
                    result = buildJsonObject {
                        put("protocolVersion", "2024-11-05")
                        put("serverInfo", buildJsonObject {
                            put("name", "graphcode-mcp")
                            put("version", "1.0.0")
                        })
                        put("capabilities", buildJsonObject {
                            put("tools", buildJsonObject {
                                put("listChanged", true)
                            })
                        })
                    }
                )
            }
            "notifications/initialized" -> {
                // Notificação não tem resposta
                null
            }
            "ping" -> {
                JsonRpcResponse(id = request.id, result = buildJsonObject {})
            }
            else -> {
                JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcError(
                        code = RpcErrorCodes.METHOD_NOT_FOUND,
                        message = "Method \${request.method} not found"
                    )
                )
            }
        }
    }
}
