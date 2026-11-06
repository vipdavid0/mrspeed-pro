package com.fdossena.speedtest.core.upload

import com.fdossena.speedtest.core.base.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.util.*

object Uploader {
    private const val BUFFER_SIZE = 65536 // Increased from 16KB for better high-speed throughput on TV/Firesticks

    fun upload(c: Connection, path: String, ckSize: Int) = flow {
        val garbage = ByteArray(BUFFER_SIZE)
        val r = Random(System.nanoTime())
        r.nextBytes(garbage)
        val ckBytes = (ckSize * 1048576).toLong()
        
        try {
            val s = path
            var lastProgressEvent = System.currentTimeMillis()
            val out = c.getOutputStream() ?: throw Exception("No output stream")
            val input = c.getInputStream() ?: throw Exception("No input stream")
            var totUploaded: Long = 0
            
            while (currentCoroutineContext().isActive) {
                // Send request headers
                c.POST(s, true, "application/octet-stream", ckBytes)
                
                // Send request body
                var bytesSent = 0L
                while (bytesSent < ckBytes && currentCoroutineContext().isActive) {
                    val toSend = minOf(garbage.size.toLong(), ckBytes - bytesSent).toInt()
                    out.write(garbage, 0, toSend)
                    bytesSent += toSend
                    totUploaded += toSend
                    
                    val now = System.currentTimeMillis()
                    if (now - lastProgressEvent > 200) {
                        lastProgressEvent = now
                        emit(totUploaded)
                    }
                }
                out.flush()
                
                // Read response headers
                val h = c.parseResponseHeaders()
                
                // Consume response body
                val contentLength = h["content-length"]?.toIntOrNull()
                if (contentLength != null && contentLength > 0) {
                    val buf = ByteArray(1024)
                    var remaining = contentLength
                    while (remaining > 0) {
                        val toRead = minOf(remaining, buf.size)
                        val read = input.read(buf, 0, toRead)
                        if (read == -1) break
                        remaining -= read
                    }
                } else if (h["transfer-encoding"]?.equals("chunked", ignoreCase = true) == true) {
                    while (true) {
                        val line = c.readLineUnbuffered() ?: break
                        val sizeString = line.trim().split(";")[0]
                        if (sizeString.isEmpty()) continue
                        val size = sizeString.toIntOrNull(16) ?: break
                        if (size == 0) {
                            c.readLineUnbuffered()
                            break
                        }
                        var remaining = size
                        val buf = ByteArray(1024)
                        while (remaining > 0) {
                            val toRead = minOf(remaining, buf.size)
                            val read = input.read(buf, 0, toRead)
                            if (read == -1) break
                            remaining -= read
                        }
                        c.readLineUnbuffered()
                    }
                }
            }
            emit(totUploaded)
        } catch (t: Throwable) {
            throw t
        } finally {
            try { c.close() } catch (e: Throwable) {}
        }
    }.flowOn(Dispatchers.IO)
}
