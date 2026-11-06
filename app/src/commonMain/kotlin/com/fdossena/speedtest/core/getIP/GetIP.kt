package com.fdossena.speedtest.core.getIP

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.base.Utils
import com.fdossena.speedtest.core.config.SpeedtestConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

object GetIP {
    suspend fun getIP(
        c: Connection,
        path: String,
        isp: Boolean,
        distance: String?
    ): String = withContext(Dispatchers.IO) {
        if (!(distance == null || distance == SpeedtestConfig.DISTANCE_KM || distance == SpeedtestConfig.DISTANCE_MILES || distance == SpeedtestConfig.DISTANCE_NO)) {
            throw IllegalArgumentException("Distance must be null, mi, km or no")
        }
        try {
            var s = path
            if (isp) {
                s += Utils.url_sep(s) + "isp=true"
                if (distance != SpeedtestConfig.DISTANCE_NO) {
                    s += Utils.url_sep(s) + "distance=" + distance
                }
            }
            c.GET(s, true)
            val h = c.parseResponseHeaders()
            val input = c.getInputStream() ?: throw Exception("No input stream")
            val isr = InputStreamReader(input, "utf-8")
            
            val contentLength = h["content-length"]?.toIntOrNull()
            val result = if (contentLength != null) {
                //standard encoding
                if (contentLength < 0) throw Exception("Invalid content-length")
                val buf = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val r = isr.read(buf, totalRead, contentLength - totalRead)
                    if (r == -1) break
                    totalRead += r
                }
                String(buf, 0, totalRead)
            } else if (h["transfer-encoding"]?.equals("chunked", ignoreCase = true) == true) {
                //chunked encoding
                val sb = StringBuilder()
                while (true) {
                    val line = c.readLineUnbuffered() ?: break
                    val parts = line.trim().split(";")
                    if (parts.isEmpty() || parts[0].isEmpty()) continue
                    val chunkSize = parts[0].toIntOrNull(16) ?: break
                    if (chunkSize == 0) {
                        c.readLineUnbuffered() // trailing empty line
                        break
                    }
                    val buf = CharArray(chunkSize)
                    var totalRead = 0
                    while (totalRead < chunkSize) {
                        val r = isr.read(buf, totalRead, chunkSize - totalRead)
                        if (r == -1) break
                        totalRead += r
                    }
                    sb.append(buf, 0, totalRead)
                    c.readLineUnbuffered() // end of chunk
                }
                sb.toString()
            } else {
                // Unknown length, read until EOF
                isr.readText()
            }
            result
        } catch (t: Throwable) {
            throw t
        } finally {
            try { c.close() } catch (e: Throwable) {}
        }
    }
}
