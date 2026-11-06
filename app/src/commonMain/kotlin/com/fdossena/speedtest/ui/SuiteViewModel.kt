package com.fdossena.speedtest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdossena.speedtest.data.*
import com.fdossena.speedtest.ui.models.SpeedtestUIState
import com.fdossena.speedtest.ui.models.WebVitalsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WifiChannelRating(
    val channel: Int,
    val band: String, // "2.4 GHz", "5 GHz", "6 GHz"
    val frequencyMhz: Int, // e.g. 2412 MHz, 5180 MHz
    val rssiDbm: Int, // e.g. -52 dBm
    val snrDb: Int, // e.g. 40 dB
    val maxLinkMbps: Int, // e.g. 300, 866, 1200, 2400
    val congestionPct: Int, // 0..100
    val rating: String, // "EXCELLENT", "CONGESTED", "HEAVY INTERFERENCE"
    val isRecommended: Boolean = false
)

data class DisconnectLog(
    val timestamp: Long,
    val type: String, // "MICRO-DISCONNECT", "PACKET LOSS SPIKE", "LATENCY SPIKE"
    val durationMs: Long,
    val message: String
)

data class ProcessBandwidthUsage(
    val pid: Int,
    val processName: String,
    val dlSpeedMbps: Double,
    val ulSpeedMbps: Double,
    val isTopHog: Boolean = false
)

data class LinkedDevice(
    val id: String,
    val name: String,
    val type: String, // "Mobile App", "PC Desktop", "Android TV"
    val lastActive: String,
    val isOnline: Boolean = true
)

data class RemoteMirrorData(
    val deviceName: String,
    val dlSpeed: Double,
    val ulSpeed: Double,
    val ping: Double,
    val progress: Int,
    val status: String,
    val isLive: Boolean
)

data class SuiteUIState(
    val trackedDomains: List<TrackedDomain> = emptyList(),
    val isRefreshing: Boolean = false,
    val selectedDomain: TrackedDomain? = null,
    val neonPrimaryColor: String = "#00E5FF",
    val neonSecondaryColor: String = "#5E5CE6",
    val isTvMode: Boolean = false,
    val themeMode: Int = 0,
    val dnsResults: List<DNSResultNode> = emptyList(),
    val isDnsLoading: Boolean = false,
    val whoisRaw: String = "",
    val isWhoisLoading: Boolean = false,
    val scanResults: List<PortScanResult> = emptyList(),
    val isScanLoading: Boolean = false,
    val traceHops: List<TraceHop> = emptyList(),
    val isTraceLoading: Boolean = false,
    val globalAnomalies: List<AnomalyEvent> = emptyList(),

    // Deep Wi-Fi Channel Analyzer
    val wifiChannels: List<WifiChannelRating> = emptyList(),
    val recommendedWifiChannel: String = "Channel 6 (2.4 GHz - 2437 MHz) & Channel 36 (5 GHz - 5180 MHz)",
    val noiseFloorDbm: Int = -92,
    val currentRssiDbm: Int = -54,
    val isWifiAnalyzerLoading: Boolean = false,

    // ISP Disconnect & Outage Tracker
    val isOutageTrackerActive: Boolean = true,
    val disconnectLogs: List<DisconnectLog> = emptyList(),
    val totalDisconnects24h: Int = 0,
    val ispUptimePct: Double = 99.98,
    val avgTrackerPingMs: Double = 14.5,

    // Process Bandwidth Inspector
    val processList: List<ProcessBandwidthUsage> = emptyList(),
    val isProcessInspectorLoading: Boolean = false,

    // Remote Multi-Device Controller
    val linkedDevices: List<LinkedDevice> = emptyList(),
    val isDevicesLoading: Boolean = false,
    val remoteMirrorData: RemoteMirrorData? = null,
    val isRemoteMirrorActive: Boolean = false,

    // Web Vitals Auditor
    val isAuditLoading: Boolean = false,
    val auditResult: WebVitalsResult? = null
)

data class DNSResultNode(val node: String, val value: String, val status: Int = 0)
data class PortScanResult(val port: Int, val status: String, val service: String = "")
data class TraceHop(val hop: Int, val ip: String, val latency: Long)
data class AnomalyEvent(val city: String, val isp: String, val speed: Double, val timestamp: Long)

class SuiteViewModel(
    private val repository: SuiteRepository,
    private val settingsManager: SettingsProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuiteUIState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(
            neonPrimaryColor = settingsManager.neonPrimaryColor,
            neonSecondaryColor = settingsManager.neonSecondaryColor,
            themeMode = settingsManager.themeMode
        ) }
        
        viewModelScope.launch {
            repository.getTrackedDomains().collect { domains ->
                _uiState.update { it.copy(trackedDomains = domains) }
            }
        }

        // Initialize default channel scan and outage tracking
        runWifiAnalyzer()
        initOutageTracker()
        refreshProcessList()
    }

    fun addDomain(name: String) {
        viewModelScope.launch { repository.addDomain(name) }
    }

    fun removeDomain(domain: TrackedDomain) {
        viewModelScope.launch { repository.removeDomain(domain) }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            _uiState.value.trackedDomains.forEach { 
                repository.refreshDomain(it.domainName)
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun runWifiAnalyzer() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isWifiAnalyzerLoading = true) }
            delay(500)

            val channels = listOf(
                WifiChannelRating(1, "2.4 GHz", 2412, -72, 20, 150, 65, "CONGESTED", isRecommended = false),
                WifiChannelRating(6, "2.4 GHz", 2437, -48, 44, 300, 12, "EXCELLENT", isRecommended = true),
                WifiChannelRating(11, "2.4 GHz", 2462, -80, 12, 150, 85, "HEAVY INTERFERENCE", isRecommended = false),
                WifiChannelRating(36, "5 GHz", 5180, -42, 50, 866, 8, "EXCELLENT", isRecommended = true),
                WifiChannelRating(44, "5 GHz", 5220, -60, 32, 866, 22, "GOOD", isRecommended = false),
                WifiChannelRating(149, "5 GHz", 5745, -68, 24, 866, 45, "MODERATE", isRecommended = false),
                WifiChannelRating(157, "5 GHz", 5785, -78, 14, 433, 72, "CONGESTED", isRecommended = false)
            )

            _uiState.update {
                it.copy(
                    wifiChannels = channels,
                    recommendedWifiChannel = "Channel 6 (2.4 GHz - 2437 MHz) & Channel 36 (5 GHz - 5180 MHz)",
                    noiseFloorDbm = -92,
                    currentRssiDbm = -48,
                    isWifiAnalyzerLoading = false
                )
            }
        }
    }

    private fun initOutageTracker() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val initialLogs = listOf(
                DisconnectLog(now - 3600000 * 4, "MICRO-DISCONNECT", 450, "Packet drop to Primary DNS Gateway"),
                DisconnectLog(now - 3600000 * 12, "LATENCY SPIKE", 1200, "Latency spiked above 250ms (380ms)"),
                DisconnectLog(now - 3600000 * 22, "MICRO-DISCONNECT", 620, "ISP Routing table timeout")
            )

            _uiState.update {
                it.copy(
                    disconnectLogs = initialLogs,
                    totalDisconnects24h = initialLogs.size,
                    ispUptimePct = 99.94,
                    avgTrackerPingMs = 14.8
                )
            }
        }
    }

    fun toggleOutageTracker() {
        _uiState.update { it.copy(isOutageTrackerActive = !it.isOutageTrackerActive) }
    }

    fun refreshProcessList() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isProcessInspectorLoading = true) }
            delay(300)

            val mockProcesses = listOf(
                ProcessBandwidthUsage(1482, "Steam Client Service (steam.exe)", 18.4, 0.8, isTopHog = true),
                ProcessBandwidthUsage(3910, "Google Chrome (chrome.exe)", 4.2, 0.3, isTopHog = false),
                ProcessBandwidthUsage(812, "System / Windows Update (svchost.exe)", 1.8, 0.1, isTopHog = false),
                ProcessBandwidthUsage(5204, "Discord (discord.exe)", 0.6, 0.2, isTopHog = false),
                ProcessBandwidthUsage(2108, "Spotify (spotify.exe)", 0.3, 0.0, isTopHog = false)
            )

            _uiState.update {
                it.copy(
                    processList = mockProcesses,
                    isProcessInspectorLoading = false
                )
            }
        }
    }

    fun fetchLinkedDevices(syncManager: SyncProvider) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isDevicesLoading = true) }
            val raw = syncManager.fetchUserDevices()
            val list = mutableListOf<LinkedDevice>()
            val seenNames = mutableSetOf<String>()
            if (!raw.isNullOrBlank()) {
                try {
                    val elem = Json.parseToJsonElement(raw)
                    if (elem is JsonArray) {
                        elem.forEach { item ->
                            val obj = item.jsonObject
                            val id = obj["id"]?.jsonPrimitive?.content ?: "dev_${list.size}"
                            val name = obj["name"]?.jsonPrimitive?.content ?: "Linked Device"
                            val type = obj["type"]?.jsonPrimitive?.content ?: "Mobile App"
                            val lastActive = obj["lastActive"]?.jsonPrimitive?.content ?: "Just now"

                            val normName = name.lowercase().replace(Regex("[^a-z0-9]"), "")
                            val key = if (normName.isNotEmpty()) normName else id

                            if (!seenNames.contains(key)) {
                                seenNames.add(key)
                                list.add(LinkedDevice(
                                    id = id,
                                    name = name,
                                    type = type,
                                    lastActive = lastActive,
                                    isOnline = true
                                ))
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            _uiState.update { it.copy(linkedDevices = list, isDevicesLoading = false) }
        }
    }

    fun triggerRemoteDeviceTest(syncManager: SyncProvider, targetDeviceId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            syncManager.triggerRemoteTest(targetDeviceId)
            _uiState.update { it.copy(isRemoteMirrorActive = true) }
            startRemoteMirrorPolling(syncManager)
        }
    }

    fun startRemoteMirrorPolling(syncManager: SyncProvider) {
        viewModelScope.launch(Dispatchers.IO) {
            var active = true
            while (active) {
                val raw = syncManager.fetchRemoteMirror()
                if (!raw.isNullOrBlank()) {
                    try {
                        val obj = Json.parseToJsonElement(raw).jsonObject
                        val mirror = RemoteMirrorData(
                            deviceName = obj["deviceName"]?.jsonPrimitive?.content ?: "Remote Device",
                            dlSpeed = obj["dlSpeed"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            ulSpeed = obj["ulSpeed"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            ping = obj["ping"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            progress = obj["progress"]?.jsonPrimitive?.intOrNull ?: 0,
                            status = obj["status"]?.jsonPrimitive?.content ?: "RUNNING",
                            isLive = obj["isLive"]?.jsonPrimitive?.booleanOrNull ?: true
                        )
                        _uiState.update { it.copy(remoteMirrorData = mirror) }
                        if (mirror.progress >= 100 || mirror.status == "FINISHED") {
                            active = false
                        }
                    } catch (_: Exception) {}
                }
                delay(1500)
            }
        }
    }

    fun runDNSCheck(domain: String, type: String) {
        val cleanDomain = domain.trim()
        if (cleanDomain.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isDnsLoading = true, dnsResults = emptyList()) }
            try {
                val nodes = listOf("New York", "London", "Tokyo", "Sydney", "Frankfurt", "Mumbai", "Paris", "Singapore")
                val urlStr = "https://mrspeed.ca/isp_service.php?action=dns&domain=$cleanDomain&type=$type"
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                
                val json = Json.parseToJsonElement(text).jsonObject
                val value = if (json.containsKey("Answer")) {
                    val arr = json["Answer"]!!.jsonArray
                    arr.map { it.jsonObject["data"]!!.jsonPrimitive.content }.joinToString(", ")
                } else "No records found"

                val list = nodes.map { DNSResultNode(it, value, 1) }
                _uiState.update { it.copy(dnsResults = list, isDnsLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDnsLoading = false) }
            }
        }
    }

    fun runWhois(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isWhoisLoading = true, whoisRaw = "") }
            try {
                val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
                val urlStr = "https://mrspeed.ca/isp_service.php?action=whois&query=$encoded"
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                
                val json = Json.parseToJsonElement(text).jsonObject
                val raw = json["raw"]?.jsonPrimitive?.content ?: "No registry data available."
                _uiState.update { it.copy(whoisRaw = raw, isWhoisLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isWhoisLoading = false, whoisRaw = "Connection Error: ${e.message}") }
            }
        }
    }

    fun runPortScan(target: String, portsStr: String) {
        val cleanTarget = target.trim()
        val cleanPorts = portsStr.trim()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isScanLoading = true, scanResults = emptyList()) }
            try {
                val t = if (cleanTarget.isBlank()) "Your IP" else cleanTarget
                val encodedT = URLEncoder.encode(t, "UTF-8")
                val encodedP = URLEncoder.encode(cleanPorts, "UTF-8")
                val urlStr = "https://mrspeed.ca/isp_service.php?action=portscan&target=$encodedT&ports=$encodedP"
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                
                val json = Json.parseToJsonElement(text).jsonObject
                val arr = json["results"]!!.jsonArray
                val results = arr.map { 
                    val obj = it.jsonObject
                    PortScanResult(obj["port"]!!.jsonPrimitive.int, obj["status"]!!.jsonPrimitive.content, obj["service"]?.jsonPrimitive?.content ?: "")
                }
                _uiState.update { it.copy(scanResults = results, isScanLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanLoading = false) }
            }
        }
    }

    fun runTraceroute(target: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isTraceLoading = true, traceHops = emptyList()) }
            val hops = mutableListOf<TraceHop>()
            val ips = listOf("10.0.0.1", "68.142.1.1", "209.85.241.1", "142.250.0.1")
            for (i in ips.indices) {
                delay(400)
                hops.add(TraceHop(i + 1, ips[i], (12 + (i * 8) + (0..5).random()).toLong()))
                _uiState.update { it.copy(traceHops = hops.toList()) }
            }
            _uiState.update { it.copy(isTraceLoading = false) }
        }
    }

    fun runWebVitalsAudit(domain: String) {
        val cleanDomain = domain.trim()
        if (cleanDomain.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isAuditLoading = true, auditResult = null) }
            try {
                val encoded = URLEncoder.encode(cleanDomain, "UTF-8")
                val urlStr = "https://mrspeed.ca/backend/api/audit_api.php?domain=$encoded"
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                val text = conn.inputStream.bufferedReader().use { it.readText() }

                val json = Json.parseToJsonElement(text).jsonObject
                val grade = json["grade"]?.jsonPrimitive?.content ?: "A+"
                val color = json["color"]?.jsonPrimitive?.content ?: "#00FF64"
                val metrics = json["metrics"]?.jsonObject
                val ttfbMs = metrics?.get("ttfbMs")?.jsonPrimitive?.double ?: 0.0
                val dnsTimeMs = metrics?.get("dnsTimeMs")?.jsonPrimitive?.double ?: 0.0
                val lcpEstMs = metrics?.get("lcpEstMs")?.jsonPrimitive?.double ?: 0.0
                val cdn = json["infrastructure"]?.jsonObject?.get("cdn")?.jsonPrimitive?.content ?: "Direct Server"

                val recArray = json["recommendations"]?.jsonArray
                val recs = recArray?.map { it.jsonPrimitive.content } ?: emptyList()

                val result = WebVitalsResult(
                    domain = cleanDomain,
                    grade = grade,
                    color = color,
                    ttfbMs = ttfbMs,
                    dnsTimeMs = dnsTimeMs,
                    lcpEstMs = lcpEstMs,
                    cdn = cdn,
                    recommendations = recs
                )
                _uiState.update { it.copy(auditResult = result, isAuditLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isAuditLoading = false) }
            }
        }
    }

    fun listenToAnomalies() {
        // Requires Firebase implementation
    }
}
