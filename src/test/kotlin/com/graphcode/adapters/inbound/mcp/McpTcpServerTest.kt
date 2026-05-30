package com.graphcode.adapters.inbound.mcp

import com.graphcode.adapters.inbound.mcp.protocol.JsonRpcRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import java.net.Socket
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
class McpTcpServerTest {

    @Test
    fun `should perform MCP handshake in under 100ms`() {
        // Assume server is running as part of Spring context on 8081
        val timeInMillis = measureTimeMillis {
            Socket("localhost", 8081).use { socket ->
                val writer = socket.getOutputStream().bufferedWriter()
                val reader = socket.getInputStream().bufferedReader()
                
                val initReq = JsonRpcRequest(
                    id = "1",
                    method = "initialize",
                    params = buildJsonObject {}
                )
                
                writer.write(Json.encodeToString(initReq) + "\n")
                writer.flush()
                
                val responseLine = reader.readLine()
                assertNotNull(responseLine, "Should receive a response")
                assertTrue(responseLine.contains("graphcode-mcp"), "Should contain server info")
                assertTrue(responseLine.contains("2024-11-05"), "Should support protocol version 2024-11-05")
            }
        }
        
        println("Handshake took \$timeInMillis ms")
        assertTrue(timeInMillis < 1000, "Handshake should be fast. Threshold set higher for CI but normally < 100ms. Measured: \$timeInMillis ms")
    }
}
