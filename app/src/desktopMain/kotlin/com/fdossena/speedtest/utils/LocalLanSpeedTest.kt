package com.fdossena.speedtest.utils

import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object LocalLanSpeedTest {
    private var serverSocket: ServerSocket? = null
    var isRunning = false
        private set
    var serverPort = 8089
        private set

    fun getLocalIpAddress(): String {
        return try {
            InetAddress.getLocalHost().hostAddress ?: "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    fun startLanServer(port: Int = 8089) {
        if (isRunning) return
        serverPort = port
        thread(isDaemon = true, name = "LocalLanServerThread") {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                println("[LocalLAN] LAN Speed Test Server listening on port $port (IP: ${getLocalIpAddress()})")
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    thread(isDaemon = true) {
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                println("[LocalLAN] Server error: ${e.message}")
            } finally {
                isRunning = false
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val output: OutputStream = socket.getOutputStream()
            val buffer = ByteArray(64 * 1024)
            for (i in 0 until 200) {
                output.write(buffer)
            }
            output.flush()
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    fun stopLanServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        println("[LocalLAN] Server stopped.")
    }
}
