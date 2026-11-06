package com.fdossena.speedtest.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class WebServiceStatusRepository(
    syncManager: SyncProvider
) : ServiceStatusRepository {
    private val _statuses = MutableStateFlow<List<ServiceStatus>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: Flow<Boolean> = _isSyncing.asStateFlow()

    @Volatile override var isTesting: Boolean = false
    private val apiBaseUrl = "https://mrspeed.ca/backend/api"

    init {
        syncManager.hashCode()
        loadInitialLocalFallback()
        scope.launch {
            refreshStatuses()
        }
    }

    private fun loadInitialLocalFallback() {
        try {
            val stream = javaClass.classLoader.getResourceAsStream("top_500_services.json")
                ?: javaClass.classLoader.getResourceAsStream("assets/top_500_services.json")
                ?: javaClass.classLoader.getResourceAsStream("resources/top_500_services.json")
            if (stream != null) {
                val text = stream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                val fallbackList = mutableListOf<ServiceStatus>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name", "Service")
                    val cat = obj.optString("category", "Web Service")
                    val url = obj.optString("url", "https://${name.lowercase().replace(" ", "")}.com")
                    val statusStr = obj.optString("status", "UP")
                    val reports = obj.optInt("reports", obj.optInt("reportCount", 0))
                    fallbackList.add(ServiceStatus(
                        id = obj.optString("id", name.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }),
                        name = name,
                        url = url,
                        statusUrl = obj.optString("statusUrl", url),
                        category = cat,
                        currentStatus = when(statusStr.uppercase()) {
                            "DOWN" -> StatusLevel.DOWN
                            "ISSUES" -> StatusLevel.ISSUES
                            else -> StatusLevel.UP
                        },
                        reportCount = reports,
                        historicalReports = emptyList()
                    ))
                }
                if (fallbackList.isNotEmpty()) {
                    _statuses.value = fallbackList
                }
            }
        } catch (_: Exception) {}
    }

    override fun getServiceStatuses(): Flow<List<ServiceStatus>> = _statuses.asStateFlow()

    private suspend fun executeRequest(urlString: String, method: String = "GET", body: String? = null): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpsURLConnection
                conn.requestMethod = method
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "MrSpeedPro/1.0 (Desktop)")
                
                if (body != null && (method == "POST" || method == "PUT")) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }

                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
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
                val item = array.optJSONObject(i) ?: continue
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
            // Silently handle
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
            val reachable = address.isReachable(1550)
            if (!reachable) return StatusLevel.DOWN

            val port = if (targetUrl.startsWith("https")) 443 else 80
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, port), 1500)
                return StatusLevel.UP
            }
        } catch (e: Exception) {
            return StatusLevel.DOWN
        }
    }

    private fun generateMockStatus(name: String, url: String, category: String, id: String, statusString: String = "UP", reportCount: Int = 0): ServiceStatus {
        val status = try { StatusLevel.valueOf(statusString) } catch (e: Exception) { StatusLevel.UP }
        val history = List(12) { 15 + (Math.random() * 25).toInt() }
        val isAdult = isAdultContent(name) || isAdultContent(url)

        return ServiceStatus(
            id = id,
            name = if (isAdult) "[Restricted Service]" else name,
            url = if (isAdult) "" else url,
            statusUrl = null,
            category = category,
            currentStatus = status,
            reportCount = reportCount,
            historicalReports = history,
            isLimited = isAdult
        )
    }

    override suspend fun reportIssue(serviceId: String) {}
    override suspend fun addCustomService(name: String, url: String) {
        val id = name.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }
        val status = ServiceStatus(id = id, name = name, url = url, category = "Custom", currentStatus = StatusLevel.UP, reportCount = 1, historicalReports = listOf(20))
        _statuses.value = _statuses.value + status
    }
}
