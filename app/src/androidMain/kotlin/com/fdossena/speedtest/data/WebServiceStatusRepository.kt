package com.fdossena.speedtest.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.random.Random

class WebServiceStatusRepository(
    private val syncManager: SyncProvider
) : ServiceStatusRepository {
    private val _statuses = MutableStateFlow<List<ServiceStatus>>(emptyList())
    private val _isSyncing = MutableStateFlow(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val apiBaseUrl = "https://mrspeed.ca/backend/api"
    @Volatile override var isTesting: Boolean = false

    override fun getServiceStatuses(): Flow<List<ServiceStatus>> = _statuses.asStateFlow()
    override val isSyncing: Flow<Boolean> = _isSyncing.asStateFlow()

    companion object {
        val staticServices = listOf(
            "Google" to "google.com",
            "YouTube" to "youtube.com",
            "Netflix" to "netflix.com",
            "Facebook" to "facebook.com",
            "Instagram" to "instagram.com",
            "Twitter / X" to "twitter.com",
            "Amazon" to "amazon.com",
            "WhatsApp" to "whatsapp.com",
            "Discord" to "discord.com",
            "Spotify" to "spotify.com",
            "Steam" to "steampowered.com",
            "PlayStation Network" to "playstation.com",
            "Xbox Live" to "xbox.com",
            "Reddit" to "reddit.com",
            "Twitch" to "twitch.tv",
            "TikTok" to "tiktok.com",
            "Zoom" to "zoom.us",
            "Microsoft Teams" to "teams.microsoft.com",
            "GitHub" to "github.com",
            "LinkedIn" to "linkedin.com"
        )
    }

    init {
        _statuses.value = staticServices.map { (name, url) -> generateMockStatus(name, url) }

        // Asynchronous cloud polling & distributed 13-minute probe worker
        scope.launch {
            var lastProbeTime = 0L
            while (isActive) {
                if (!isTesting) {
                    refreshStatuses()
                    val now = System.currentTimeMillis()
                    // Every 13 minutes (780,000 ms), run distributed crowd-sourced probing
                    if (now - lastProbeTime >= 780000L) {
                        lastProbeTime = now
                        runDistributedProbes()
                    }
                }
                delay(30000) // Poll UI list every 30 seconds
            }
        }
    }

    private suspend fun runDistributedProbes() = withContext(Dispatchers.IO) {
        try {
            // Get 15 candidate services that haven't been checked in the last 50 minutes
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
                        val status = probeServiceStatus(target)
                        val statusStr = when (status) {
                            StatusLevel.ISSUES -> "ISSUES"
                            StatusLevel.DOWN -> "DOWN"
                            else -> "UP"
                        }
                        probeResults.put(JSONObject().apply {
                            put("id", id)
                            put("status", statusStr)
                        })
                    }
                }

                // Sync probe results back to central database globally
                if (probeResults.length() > 0) {
                    executeRequest("$apiBaseUrl/detector.php?action=sync_probes", "POST", probeResults.toString())
                }
            }
        } catch (e: Throwable) {
            println("[DEBUG] WebServiceStatusRepo: distributed probe error: ${e.message}")
        }
    }

    private suspend fun refreshStatuses() = withContext(Dispatchers.IO) {
        if (isTesting) return@withContext
        try {
            _isSyncing.value = true
            val response = executeRequest("$apiBaseUrl/detector.php", "GET")
            if (isTesting || response == null) return@withContext

            val jsonTrimmed = response.trim()
            val array = try {
                JSONArray(jsonTrimmed)
            } catch (_: Exception) {
                val foundArray = JSONArray()
                try {
                    val jsonObj = JSONObject(jsonTrimmed)
                    if (jsonObj.has("documents")) {
                        val docs = jsonObj.getJSONArray("documents")
                        for (i in 0 until docs.length()) {
                            foundArray.put(docs.getJSONObject(i))
                        }
                    } else if (jsonObj.has("services")) {
                        val srvs = jsonObj.getJSONArray("services")
                        for (i in 0 until srvs.length()) {
                            foundArray.put(srvs.getJSONObject(i))
                        }
                    } else {
                        val keys = jsonObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = jsonObj.opt(key)
                            if (value is JSONArray) {
                                for (i in 0 until value.length()) {
                                    foundArray.put(value.get(i))
                                }
                            } else if (value is JSONObject) {
                                foundArray.put(value)
                            }
                        }
                    }
                } catch (_: Exception) {}
                foundArray
            }

            if (isTesting) return@withContext
            val newList = mutableListOf<ServiceStatus>()

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val doc = item.optJSONObject("document")
                    val fields = doc?.optJSONObject("fields") ?: item

                    val name = fields.optJSONObject("name")?.optString("stringValue") 
                        ?: item.optString("name", "Unknown")
                    val url = fields.optJSONObject("url")?.optString("stringValue") 
                        ?: item.optString("url", "")
                    val category = fields.optJSONObject("category")?.optString("stringValue") 
                        ?: item.optString("category", "Web Service")
                    val statusStr = fields.optJSONObject("currentStatus")?.optString("stringValue") 
                        ?: item.optString("status", "UP")
                    val reportCount = fields.optJSONObject("reportCount")?.optString("integerValue")?.toIntOrNull()
                        ?: fields.optJSONObject("reportCount")?.optInt("integerValue")
                        ?: item.optInt("report_count", 0)
                    val id = item.optString("id", name.lowercase().replace(" ", "").filter { it.isLetterOrDigit() })

                    newList.add(
                        generateMockStatus(
                            name = name,
                            url = url,
                            category = category,
                            id = id,
                            statusString = statusStr,
                            reportCount = reportCount
                        )
                    )
                }

                if (newList.isNotEmpty()) {
                    _statuses.value = newList.sortedByDescending { it.reportCount }
                }
        } catch (e: Throwable) {
            println("[DEBUG] WebServiceStatusRepo: refresh error: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    override suspend fun seedInitialServices(jsonString: String?) {
        withContext(Dispatchers.IO) {
            try {
                if (jsonString != null && jsonString.length > 100) {
                    val jsonTrimmed = jsonString.trim()
                    val array = JSONArray()
                    try {
                        if (jsonTrimmed.startsWith("[")) {
                            val srcArr = JSONArray(jsonTrimmed)
                            for (i in 0 until srcArr.length()) {
                                array.put(srcArr.get(i))
                            }
                        } else if (jsonTrimmed.startsWith("{")) {
                            val jsonObj = JSONObject(jsonTrimmed)
                            if (jsonObj.has("services")) {
                                val srvs = jsonObj.getJSONArray("services")
                                for (i in 0 until srvs.length()) {
                                    array.put(srvs.getJSONObject(i))
                                }
                            } else {
                                val keys = jsonObj.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    val value = jsonObj.opt(key)
                                    if (value is JSONArray) {
                                        for (i in 0 until value.length()) {
                                            array.put(value.get(i))
                                        }
                                    } else if (value is JSONObject) {
                                        if (value.has("name") || value.has("url")) {
                                            array.put(value)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    val newList = mutableListOf<ServiceStatus>()
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val name = obj.optString("name", "")
                        if (name.isEmpty()) continue
                        val url = obj.optString("url", "")
                        val category = obj.optString("category", "Web Service")
                        val id = name.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }
                        newList.add(generateMockStatus(name = name, url = url, category = category, id = id))
                    }
                    if (newList.size > _statuses.value.size) {
                        _statuses.value = newList
                    }
                }
                refreshStatuses()
            } catch (_: Exception) {}
        }
    }

    private fun isAdultContent(domain: String): Boolean {
        val clean = domain.lowercase().trim()
        val adultKeywords = listOf("porn", "xxx", "sex", "xvideos", "pornhub", "xhamster", "redtube", "youporn", "brazzers", "onlyfans", "chaturbate", "strip", "erotic", "adult", "nsfw", "hentai", "camgirls")
        return adultKeywords.any { clean.contains(it) }
    }

    private fun probeServiceStatus(targetUrl: String): StatusLevel {
        val domain = targetUrl.replace(Regex("^https?://"), "").substringBefore("/").substringBefore(":").trim()
        if (domain.isBlank()) return StatusLevel.DOWN

        try {
            val address = InetAddress.getByName(domain)
            if (address.hostAddress.isNullOrBlank()) return StatusLevel.DOWN
        } catch (e: Exception) {
            return StatusLevel.DOWN
        }

        fun tryConnect(targetUrlStr: String): Int {
            return try {
                val url = URL(targetUrlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
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

        if (code in 100..499) return StatusLevel.UP
        if (code in 500..599) return StatusLevel.ISSUES

        try {
            val socket443 = Socket()
            socket443.connect(InetSocketAddress(domain, 443), 3000)
            socket443.close()
            return StatusLevel.UP
        } catch (e: Exception) {}

        try {
            val socket80 = Socket()
            socket80.connect(InetSocketAddress(domain, 80), 3000)
            socket80.close()
            return StatusLevel.UP
        } catch (e: Exception) {}

        return StatusLevel.UP
    }

    override suspend fun addCustomService(name: String, url: String) {
        withContext(Dispatchers.IO) {
            val target = if (url.isNotBlank()) url else name

            if (isAdultContent(target) || isAdultContent(name)) {
                println("[DEBUG] WebServiceStatusRepo: Adult content blocked for $target")
                return@withContext
            }

            val probeStatus = probeServiceStatus(target)
            if (probeStatus == StatusLevel.DOWN) {
                println("[DEBUG] WebServiceStatusRepo: Non-existent domain blocked for $target")
                return@withContext
            }

            val id = name.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }
            val statusString = when (probeStatus) {
                StatusLevel.ISSUES -> "ISSUES"
                else -> "UP"
            }
            val newCustom = generateMockStatus(name = name, url = url, category = "User Added", id = id, statusString = statusString, reportCount = 0)

            val currentList = _statuses.value.toMutableList()
            currentList.removeAll { it.id == id }
            currentList.add(0, newCustom)
            _statuses.value = currentList

            val body = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("name", JSONObject().put("stringValue", name))
                    put("url", JSONObject().put("stringValue", url))
                    put("category", JSONObject().put("stringValue", "User Added"))
                    put("currentStatus", JSONObject().put("stringValue", statusString))
                    put("reportCount", JSONObject().put("integerValue", "0"))
                })
            }.toString()

            executeRequest("$apiBaseUrl/detector.php?path=status_services/$id", "POST", body)
            refreshStatuses()
        }
    }

    override suspend fun reportIssue(serviceId: String) {
        withContext(Dispatchers.IO) {
            executeRequest("$apiBaseUrl/detector.php?path=status_services/$serviceId&action=report", "POST")
            refreshStatuses()
        }
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
                conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }

            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun generateMockStatus(
        name: String,
        url: String,
        category: String = "Web Service",
        id: String? = null,
        statusString: String? = null,
        reportCount: Int = 0
    ): ServiceStatus {
        val level = when (statusString) {
            "UP" -> StatusLevel.UP
            "DOWN" -> StatusLevel.DOWN
            "ISSUES" -> StatusLevel.ISSUES
            else -> StatusLevel.UP
        }

        val outageHistory = mutableListOf<OutageEvent>()
        if (level != StatusLevel.UP) {
            val now = System.currentTimeMillis()
            outageHistory.add(
                OutageEvent(
                    timestamp = now - 3600000,
                    durationMinutes = Random.nextInt(15, 120),
                    type = level
                )
            )
        }

        return ServiceStatus(
            id = id ?: name.lowercase().replace(" ", "").filter { it.isLetterOrDigit() },
            name = name,
            url = url,
            statusUrl = null,
            category = category,
            currentStatus = level,
            reportCount = reportCount,
            historicalReports = List(24) { 0 },
            outageHistory = outageHistory.sortedByDescending { it.timestamp },
            isLimited = false
        )
    }
}
