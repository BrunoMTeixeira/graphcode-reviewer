package com.graphcode.adapters.inbound.mcp

import com.graphcode.adapters.inbound.mcp.protocol.JsonRpcRequest
import com.graphcode.adapters.inbound.mcp.protocol.JsonRpcResponse
import com.graphcode.adapters.inbound.mcp.protocol.JsonRpcError
import com.graphcode.adapters.inbound.mcp.protocol.RpcErrorCodes
import com.graphcode.ports.inbound.BlastRadiusUseCase
import com.graphcode.ports.inbound.DeadCodeUseCase
import com.graphcode.ports.inbound.MigrationEngineUseCase
import com.graphcode.ports.inbound.NlToCypherUseCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket
import org.slf4j.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class McpSession(
    private val socket: Socket,
    private val blastRadiusUseCase: BlastRadiusUseCase,
    private val deadCodeUseCase: DeadCodeUseCase,
    private val migrationEngineUseCase: MigrationEngineUseCase,
    private val nlToCypherUseCase: NlToCypherUseCase
) {
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
                null
            }
            "ping" -> {
                JsonRpcResponse(id = request.id, result = buildJsonObject {})
            }
            "tools/list" -> {
                JsonRpcResponse(
                    id = request.id,
                    result = buildJsonObject {
                        put("tools", buildJsonArray {
                            add(buildJsonObject {
                                put("name", "graphcode_blast_radius")
                                put("description", "Calcula o raio de impacto de alterações usando algoritmos de shortest path")
                                put("inputSchema", buildJsonObject {
                                    put("type", "object")
                                    put("properties", buildJsonObject {
                                        put("qualifiedName", buildJsonObject { put("type", "string") })
                                        put("maxDepth", buildJsonObject { put("type", "integer") })
                                    })
                                    put("required", buildJsonArray { add("qualifiedName") })
                                })
                            })
                            add(buildJsonObject {
                                put("name", "graphcode_dead_code")
                                put("description", "Encontra dead code, nós isolados e dependências circulares")
                                put("inputSchema", buildJsonObject {
                                    put("type", "object")
                                    put("properties", buildJsonObject {})
                                })
                            })
                            add(buildJsonObject {
                                put("name", "graphcode_migration_context")
                                put("description", "Ordena topologicamente dependências para migração de lotes de código")
                                put("inputSchema", buildJsonObject {
                                    put("type", "object")
                                    put("properties", buildJsonObject {
                                        put("filePaths", buildJsonObject {
                                            put("type", "array")
                                            put("items", buildJsonObject { put("type", "string") })
                                        })
                                    })
                                    put("required", buildJsonArray { add("filePaths") })
                                })
                            })
                            add(buildJsonObject {
                                put("name", "graphcode_nl_to_cypher")
                                put("description", "Converte queries de linguagem natural em Cypher via LLM e executa contra a BD")
                                put("inputSchema", buildJsonObject {
                                    put("type", "object")
                                    put("properties", buildJsonObject {
                                        put("query", buildJsonObject { put("type", "string") })
                                    })
                                    put("required", buildJsonArray { add("query") })
                                })
                            })
                        })
                    }
                )
            }
            "tools/call" -> {
                try {
                    val params = request.params as? JsonObject
                    val name = params?.get("name")?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing tool name")
                    val args = params.get("arguments") as? JsonObject ?: buildJsonObject {}

                    val toolContent = when (name) {
                        "graphcode_blast_radius" -> {
                            val target = args["qualifiedName"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing qualifiedName")
                            val maxDepth = args["maxDepth"]?.jsonPrimitive?.intOrNull ?: 5
                            val res = blastRadiusUseCase.calculateImpact(target, maxDepth)
                            json.encodeToString(res)
                        }
                        "graphcode_dead_code" -> {
                            val res = deadCodeUseCase.findDeadCodeAndCycles()
                            json.encodeToString(res)
                        }
                        "graphcode_migration_context" -> {
                            val arr = args["filePaths"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                            val res = migrationEngineUseCase.getMigrationContext(arr)
                            json.encodeToString(res)
                        }
                        "graphcode_nl_to_cypher" -> {
                            val query = args["query"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing query")
                            val res = nlToCypherUseCase.executeNaturalLanguageQuery(query)
                            json.encodeToString(res)
                        }
                        else -> throw IllegalArgumentException("Unknown tool: \$name")
                    }

                    JsonRpcResponse(
                        id = request.id,
                        result = buildJsonObject {
                            put("content", buildJsonArray {
                                add(buildJsonObject {
                                    put("type", "text")
                                    put("text", toolContent)
                                })
                            })
                            put("isError", false)
                        }
                    )
                } catch (e: Exception) {
                    logger.error("Erro a executar tool", e)
                    JsonRpcResponse(
                        id = request.id,
                        result = buildJsonObject {
                            put("content", buildJsonArray {
                                add(buildJsonObject {
                                    put("type", "text")
                                    put("text", "Error: \${e.message}")
                                })
                            })
                            put("isError", true)
                        }
                    )
                }
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
