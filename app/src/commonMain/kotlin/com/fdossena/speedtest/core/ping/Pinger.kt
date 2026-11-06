package com.fdossena.speedtest.core.ping

import com.fdossena.speedtest.core.base.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

object Pinger {
    fun ping(c: Connection, path: String) = flow {
        try {
            val s = path
            while (currentCoroutineContext().isActive) {
                c.GET(s, true)
                val start = System.nanoTime()
                var chunked = false
                var ok = false
                var statusLine = ""
                val headers = mutableMapOf<String, String>()
                
                while (true) {
                    val l = c.readLineUnbuffered() ?: break
                    val line = l.trim { it <= ' ' }
                    if (statusLine.isEmpty()) statusLine = line
                    
                    if (line.contains(" 2")) ok = true
                    if (line.contains(":")) {
                        val parts = line.split(":", limit = 2)
                        headers[parts[0].trim().lowercase()] = parts[1].trim()
                    }
                    if (line.isEmpty()) break
                }
                
                if (!ok) throw Exception("Server returned $statusLine")
                
                // Consume body
                val contentLength = headers["content-length"]?.toIntOrNull()
                if (contentLength != null && contentLength > 0) {
                    val buf = ByteArray(1024)
                    var remaining = contentLength
                    val input = c.getInputStream()
                    while (remaining > 0) {
                        val toRead = minOf(remaining, buf.size)
                        val read = input?.read(buf, 0, toRead) ?: break
                        if (read == -1) break
                        remaining -= read
                    }
                } else if (headers["transfer-encoding"]?.equals("chunked", ignoreCase = true) == true) {
                    while (true) {
                        val line = c.readLineUnbuffered() ?: break
                        val size = line.trim().split(";")[0].toIntOrNull(16) ?: break
                        if (size == 0) {
                            c.readLineUnbuffered() // trailing empty line
                            break
                        }
                        var remaining = size
                        val input = c.getInputStream()
                        val buf = ByteArray(1024)
                        while (remaining > 0) {
                            val toRead = minOf(remaining, buf.size)
                            val read = input?.read(buf, 0, toRead) ?: break
                            if (read == -1) break
                            remaining -= read
                        }
                        c.readLineUnbuffered() // end of chunk
                    }
                }
                
                val t = System.nanoTime() - start
                emit(t / 2)
            }
            c.close()
        } catch (t: Throwable) {
            try {
                c.close()
            } catch (e: Throwable) {
            }
            throw t
        }
    }.flowOn(Dispatchers.IO)
}
