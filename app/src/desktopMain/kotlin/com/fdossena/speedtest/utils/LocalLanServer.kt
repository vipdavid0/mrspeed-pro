package com.fdossena.speedtest.utils

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors

object LocalLanServer {
    private var httpServer: HttpServer? = null
    var isRunning = false
        private set
    var serverPort = 8080
        private set

    fun startServer(port: Int = 8080) {
        if (isRunning) return
        try {
            serverPort = port
            httpServer = HttpServer.create(InetSocketAddress("0.0.0.0", port), 0).apply {
                createContext("/backend/garbage.php") { exchange ->
                    val response = ByteArray(10 * 1024 * 1024) // 10MB chunk
                    exchange.sendResponseHeaders(200, response.size.toLong())
                    exchange.responseBody.use { it.write(response) }
                }
                createContext("/backend/empty.php") { exchange ->
                    exchange.requestBody.readBytes()
                    val resp = "OK"
                    exchange.sendResponseHeaders(200, resp.length.toLong())
                    exchange.responseBody.use { it.write(resp.toByteArray()) }
                }
                createContext("/backend/getIP.php") { exchange ->
                    val resp = "{\"ip\":\"127.0.0.1\"}"
                    exchange.sendResponseHeaders(200, resp.length.toLong())
                    exchange.responseBody.use { it.write(resp.toByteArray()) }
                }
                executor = Executors.newCachedThreadPool()
                start()
            }
            isRunning = true
            println("LocalLanServer started successfully on port $port")
        } catch (e: Exception) {
            println("LocalLanServer start error: ${e.message}")
        }
    }

    fun stopServer() {
        try {
            httpServer?.stop(0)
            httpServer = null
            isRunning = false
            println("LocalLanServer stopped")
        } catch (_: Exception) {}
    }
}
