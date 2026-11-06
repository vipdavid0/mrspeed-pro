package com.fdossena.speedtest.core.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.HashMap
import java.util.Locale
import javax.net.ssl.SSLSocketFactory

class Connection(
    val url: String,
    val connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT,
    val soTimeout: Int = DEFAULT_SO_TIMEOUT,
    val recvBuffer: Int = -1,
    val sendBuffer: Int = -1
) {
    var socket: Socket? = null
        private set
    var host: String? = null
        private set
    var port = 0
        private set
    var basePath = ""
        private set
    var mode = MODE_NOT_SET
        private set

    companion object {
        private const val MODE_NOT_SET = 0
        private const val MODE_HTTP = 1
        private const val MODE_HTTPS = 2

        private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        private val LOCALE = "en-US" // Simplified for multiplatform

        private const val DEFAULT_CONNECT_TIMEOUT = 5000
        private const val DEFAULT_SO_TIMEOUT = 10000

        suspend fun create(
            url: String,
            connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT,
            soTimeout: Int = DEFAULT_SO_TIMEOUT,
            recvBuffer: Int = -1,
            sendBuffer: Int = -1
        ): Connection = withContext(Dispatchers.IO) {
            val cTimeout = if (connectTimeout <= 0) 3000 else connectTimeout.coerceAtMost(3000)
            val sTimeout = if (soTimeout <= 0) 3000 else soTimeout.coerceAtMost(3000)
            Connection(url, cTimeout, sTimeout, recvBuffer, sendBuffer).apply {
                connect()
            }
        }
    }

    private fun connect() {
        var tryHTTP = false
        var tryHTTPS = false
        val effectiveConnectTimeout = if (connectTimeout <= 0) 3000 else connectTimeout.coerceAtMost(3000)
        val effectiveSoTimeout = if (soTimeout <= 0) 3000 else soTimeout.coerceAtMost(3000)

        if (url.startsWith("http://")) {
            tryHTTP = true
            try {
                val u = URL(url)
                host = u.host
                port = u.port
                basePath = u.path
            } catch (t: Throwable) {
                throw IllegalArgumentException("Malformed URL (HTTP)")
            }
        } else if (url.startsWith("https://")) {
            tryHTTPS = true
            try {
                val u = URL(url)
                host = u.host
                port = u.port
                basePath = u.path
            } catch (t: Throwable) {
                throw IllegalArgumentException("Malformed URL (HTTPS)")
            }
        } else if (url.startsWith("//")) {
            tryHTTP = true
            tryHTTPS = true
            try {
                val u = URL("http:$url")
                host = u.host
                port = u.port
                basePath = u.path
            } catch (t: Throwable) {
                throw IllegalArgumentException("Malformed URL (HTTP/HTTPS)")
            }
        } else {
            throw IllegalArgumentException("Malformed URL (Unknown or unspecified protocol)")
        }
        
        if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length - 1)
        
        try {
            if (mode == MODE_NOT_SET && tryHTTPS) {
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val rawSocket = Socket()
                rawSocket.tcpNoDelay = true
                rawSocket.connect(InetSocketAddress(host, if (port == -1) 443 else port), effectiveConnectTimeout)
                socket = factory.createSocket(rawSocket, host, if (port == -1) 443 else port, true)
                mode = MODE_HTTPS
            }
        } catch (t: Throwable) {
        }
        try {
            if (mode == MODE_NOT_SET && tryHTTP) {
                socket = Socket()
                socket!!.tcpNoDelay = true
                socket!!.connect(InetSocketAddress(host, if (port == -1) 80 else port), effectiveConnectTimeout)
                mode = MODE_HTTP
            }
        } catch (t: Throwable) {
        }
        
        if (mode == MODE_NOT_SET) throw IllegalStateException("Failed to connect to $host")
        
        try {
            socket?.soTimeout = effectiveSoTimeout
        } catch (t: Throwable) {
        }
        if (recvBuffer > 0) {
            try {
                socket!!.receiveBufferSize = recvBuffer
            } catch (t: Throwable) {
            }
        }
        if (sendBuffer > 0) {
            try {
                socket!!.sendBufferSize = sendBuffer
            } catch (t: Throwable) {
            }
        }
    }

    fun getInputStream(): InputStream? {
        return try {
            socket?.getInputStream()
        } catch (t: Throwable) {
            null
        }
    }

    fun getOutputStream(): OutputStream? {
        return try {
            socket?.getOutputStream()
        } catch (t: Throwable) {
            null
        }
    }

    private var ps: PrintStream? = null
    fun getPrintStream(): PrintStream? {
        if (ps == null) {
            try {
                ps = PrintStream(getOutputStream()!!, false, "utf-8")
            } catch (t: Throwable) {
                ps = null
            }
        }
        return ps
    }

    suspend fun GET(path: String, keepAlive: Boolean) = withContext(Dispatchers.IO) {
        var p = path
        try {
            if (!p.startsWith("/")) p = "/$p"
            p = basePath + p
            val ps = getPrintStream() ?: throw Exception("Not connected")
            ps.print("GET $p HTTP/1.1\r\n")
            ps.print("Host: $host${if (port != -1 && port != 80 && port != 443) ":$port" else ""}\r\n")
            ps.print("User-Agent: $USER_AGENT\r\n")
            ps.print("Referer: $url/\r\n")
            ps.print("Origin: ${if(mode == MODE_HTTPS) "https" else "http"}://$host\r\n")
            ps.print("Connection: " + (if (keepAlive) "keep-alive" else "close") + "\r\n")
            ps.print("Accept-Encoding: identity\r\n")
            ps.print("Accept-Language: $LOCALE\r\n")
            ps.print("\r\n")
            ps.flush()
        } catch (t: Throwable) {
            throw Exception("Failed to send GET request: ${t.message}")
        }
    }

    suspend fun POST(path: String, keepAlive: Boolean, contentType: String?, contentLength: Long) = withContext(Dispatchers.IO) {
        var p = path
        try {
            if (!p.startsWith("/")) p = "/$p"
            p = basePath + p
            val ps = getPrintStream() ?: throw Exception("Not connected")
            ps.print("POST $p HTTP/1.1\r\n")
            ps.print("Host: $host${if (port != -1 && port != 80 && port != 443) ":$port" else ""}\r\n")
            ps.print("User-Agent: $USER_AGENT\r\n")
            ps.print("Referer: $url/\r\n")
            ps.print("Origin: ${if(mode == MODE_HTTPS) "https" else "http"}://$host\r\n")
            ps.print("Connection: " + (if (keepAlive) "keep-alive" else "close") + "\r\n")
            ps.print("Accept-Encoding: identity\r\n")
            if (LOCALE != null) ps.print("Accept-Language: $LOCALE\r\n")
            if (contentType != null) ps.print("Content-Type: $contentType\r\n")
            ps.print("Content-Encoding: identity\r\n")
            if (contentLength >= 0) ps.print("Content-Length: $contentLength\r\n")
            ps.print("\r\n")
            ps.flush()
        } catch (t: Throwable) {
            throw Exception("Failed to send POST request: ${t.message}")
        }
    }

    suspend fun readLineUnbuffered(): String? = withContext(Dispatchers.IO) {
        try {
            val `in` = getInputStream() ?: return@withContext null
            val sb = StringBuilder()
            while (true) {
                val c = `in`.read()
                if (c == -1) break
                val char = c.toChar()
                sb.append(char)
                if (char == '\n') break
            }
            if (sb.isEmpty()) null else sb.toString()
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun parseResponseHeaders(): HashMap<String, String> = withContext(Dispatchers.IO) {
        try {
            withTimeout(if (soTimeout <= 0) 5000L else soTimeout.toLong()) {
                val ret = HashMap<String, String>()
                var statusLine: String
                while (true) {
                    val line = readLineUnbuffered() ?: throw Exception("Empty response")
                    if (line.contains(" 100 Continue")) {
                        readLineUnbuffered()
                        continue
                    }
                    statusLine = line
                    break
                }

                if (!statusLine.contains(" 2") && !statusLine.contains(" 3")) {
                    throw Exception("Server returned ${statusLine.trim()}")
                }
                while (true) {
                    val s = readLineUnbuffered()
                    if (s == null || s.trim().isEmpty()) break
                    if (s.contains(":")) {
                        val parts = s.split(":", limit = 2)
                        ret[parts[0].trim().lowercase()] = parts[1].trim()
                    }
                }
                ret
            }
        } catch (t: Throwable) {
            throw Exception(t.message ?: "Failed to parse headers")
        }
    }

    fun close() {
        try {
            socket?.close()
        } catch (t: Throwable) {
        }
        socket = null
        ps = null
    }
}
