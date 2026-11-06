package com.fdossena.speedtest.core.download

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.base.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

object Downloader {
    private const val BUFFER_SIZE = 65536 // Increased from 16KB for better high-speed throughput on TV/Firesticks

    fun download(c: Connection, path: String, ckSize: Int) = flow {
        val actualCkSize = if (ckSize < 1) 1 else ckSize
        try {
            var s = path
            s += Utils.url_sep(s) + "ckSize=" + actualCkSize
            var lastProgressEvent = System.currentTimeMillis()
            val input = c.getInputStream() ?: throw Exception("No input stream")
            val buf = ByteArray(BUFFER_SIZE)
            var totDownloaded: Long = 0
            
            while (currentCoroutineContext().isActive) {
                // Send request
                c.GET(s, true)
                
                // Read response headers
                val h = c.parseResponseHeaders()
                
                // Read response body
                val contentLength = h["content-length"]?.toLongOrNull()
                if (contentLength != null) {
                    var remaining = contentLength
                    while (remaining > 0 && currentCoroutineContext().isActive) {
                        val toRead = minOf(remaining, buf.size.toLong()).toInt()
                        val read = input.read(buf, 0, toRead)
                        if (read == -1) break
                        remaining -= read
                        totDownloaded += read
                        
                        val now = System.currentTimeMillis()
                        if (now - lastProgressEvent > 200) {
                            lastProgressEvent = now
                            emit(totDownloaded)
                        }
                    }
                    if (remaining > 0) break 
                } else if (h["transfer-encoding"]?.equals("chunked", ignoreCase = true) == true) {
                    while (currentCoroutineContext().isActive) {
                        val line = c.readLineUnbuffered() ?: break
                        val sizeString = line.trim().split(";")[0]
                        if (sizeString.isEmpty()) continue
                        val size = sizeString.toIntOrNull(16) ?: break
                        if (size == 0) {
                            c.readLineUnbuffered() // trailing empty line
                            break
                        }
                        var remaining = size.toLong()
                        while (remaining > 0 && currentCoroutineContext().isActive) {
                            val toRead = minOf(remaining, buf.size.toLong()).toInt()
                            val read = input.read(buf, 0, toRead)
                            if (read == -1) break
                            remaining -= read
                            totDownloaded += read
                            
                            val now = System.currentTimeMillis()
                            if (now - lastProgressEvent > 200) {
                                lastProgressEvent = now
                                emit(totDownloaded)
                            }
                        }
                        c.readLineUnbuffered() // end of chunk
                        if (remaining > 0) break
                    }
                } else {
                    // No length info, read until stream closed (breaks keep-alive)
                    while (currentCoroutineContext().isActive) {
                        val read = input.read(buf)
                        if (read == -1) break
                        totDownloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastProgressEvent > 200) {
                            lastProgressEvent = now
                            emit(totDownloaded)
                        }
                    }
                    break 
                }
            }
            emit(totDownloaded)
        } catch (t: Throwable) {
            throw t
        } finally {
            try { c.close() } catch (e: Throwable) {}
        }
    }.flowOn(Dispatchers.IO)
}
