package com.fdossena.speedtest.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class MrDetectorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val apiBaseUrl = "https://mrspeed.ca/backend/api"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Small jitter to spread load across multiple app instances worldwide
            delay((0..3000).random().toLong())

            // 1. Fetch 15 candidate services from SQL database not checked in the last 50 minutes
            val candidatesResp = executeRequest("$apiBaseUrl/detector.php?action=get_candidates", "GET")
            if (candidatesResp != null && candidatesResp.startsWith("[")) {
                val array = JSONArray(candidatesResp)
                val probeResults = JSONArray()

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val id = item.optString("id", "")
                    val name = item.optString("name", "")
                    val url = item.optString("url", "")
                    val target = if (url.isNotBlank()) url else name

                    if (id.isNotEmpty() && target.isNotEmpty()) {
                        val isUp = probeService(target)
                        val statusStr = if (isUp) "UP" else "DOWN"
                        probeResults.put(JSONObject().apply {
                            put("id", id)
                            put("status", statusStr)
                        })
                    }
                }

                // 2. Sync probe results back to central database globally
                if (probeResults.length() > 0) {
                    executeRequest("$apiBaseUrl/detector.php?action=sync_probes", "POST", probeResults.toString())
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private val trustAllSslFactory: SSLSocketFactory by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        sslContext.socketFactory
    }

    private fun probeService(urlString: String): Boolean {
        val domain = urlString.replace(Regex("^https?://"), "").substringBefore("/").substringBefore(":").trim()
        if (domain.isBlank()) return false

        // 1. Check DNS Resolution
        try {
            val address = InetAddress.getByName(domain)
            if (address.hostAddress.isNullOrBlank()) return false
        } catch (e: Exception) {
            return false
        }

        fun tryConnect(targetUrlStr: String): Int {
            return try {
                val url = URL(targetUrlStr)
                val conn = url.openConnection() as HttpURLConnection
                if (conn is HttpsURLConnection) {
                    conn.sslSocketFactory = trustAllSslFactory
                    conn.hostnameVerifier = HostnameVerifier { _, _ -> true }
                }
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                conn.setRequestProperty("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"")
                conn.requestMethod = "GET"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.instanceFollowRedirects = true

                conn.responseCode
            } catch (e: Exception) {
                0
            }
        }

        var code = tryConnect("https://$domain")
        if (code == 0) {
            code = tryConnect("http://$domain")
        }

        if (code in 100..499) return true

        // 2. Socket Fallback (Port 443 & Port 80)
        try {
            val socket443 = Socket()
            socket443.connect(InetSocketAddress(domain, 443), 3000)
            socket443.close()
            return true
        } catch (e: Exception) {}

        try {
            val socket80 = Socket()
            socket80.connect(InetSocketAddress(domain, 80), 3000)
            socket80.close()
            return true
        } catch (e: Exception) {}

        return false
    }

    private fun executeRequest(urlStr: String, method: String, body: String? = null): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method.uppercase()
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            if (conn.responseCode in 200..299) {
                val reader = conn.inputStream.bufferedReader()
                val charArray = CharArray(65536)
                val numRead = reader.read(charArray, 0, 65536)
                if (numRead > 0) String(charArray, 0, numRead) else null
            } else null
        } catch (e: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
