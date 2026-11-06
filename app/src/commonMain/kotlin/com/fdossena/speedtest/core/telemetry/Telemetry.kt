package com.fdossena.speedtest.core.telemetry

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.base.Utils
import com.fdossena.speedtest.core.config.TelemetryConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Telemetry {
    suspend fun send(
        c: Connection,
        path: String?,
        level: String,
        ispinfo: String?,
        extra: String?,
        dl: String?,
        ul: String?,
        ping: String?,
        jitter: String?,
        log: String?
    ): String? = withContext(Dispatchers.IO) {
        if (level == TelemetryConfig.LEVEL_DISABLED) {
            return@withContext null
        }
        try {
            val s = path!!
            val sb = StringBuilder()
            sb.append("ispinfo=")
            sb.append(Utils.urlEncode(ispinfo))
            sb.append("&dl=")
            sb.append(Utils.urlEncode(dl))
            sb.append("&ul=")
            sb.append(Utils.urlEncode(ul))
            sb.append("&ping=")
            sb.append(Utils.urlEncode(ping))
            sb.append("&jitter=")
            sb.append(Utils.urlEncode(jitter))
            if (level == TelemetryConfig.LEVEL_FULL) {
                sb.append("&log=")
                sb.append(Utils.urlEncode(log))
            }
            sb.append("&extra=")
            sb.append(Utils.urlEncode(extra))
            c.POST(s, false, "application/x-www-form-urlencoded", sb.length.toLong())
            val ps = c.getPrintStream()
            ps!!.print(sb.toString())
            ps.flush()
            val h = c.parseResponseHeaders()
            val transferEncoding = h["transfer-encoding"]
            if (transferEncoding != null && transferEncoding.equals("chunked", ignoreCase = true)) {
                c.readLineUnbuffered()
            }
            val data = c.readLineUnbuffered()
            data
        } catch (t: Throwable) {
            throw t
        } finally {
            try { c.close() } catch (e: Throwable) {}
        }
    }
}
