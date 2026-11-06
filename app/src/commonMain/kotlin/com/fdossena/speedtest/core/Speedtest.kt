package com.fdossena.speedtest.core

import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.config.TelemetryConfig
import com.fdossena.speedtest.core.serverSelector.ServerSelector
import com.fdossena.speedtest.core.serverSelector.TestPoint
import com.fdossena.speedtest.core.worker.SpeedtestWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*

class Speedtest(private val scope: CoroutineScope) {
    init {
        println("Speedtest: Initialized with scope $scope")
    }
    private val servers = ArrayList<TestPoint>()
    private var selectedServer: TestPoint? = null
    private var config = SpeedtestConfig()
    private var telemetryConfig = TelemetryConfig()
    var state = 0
        private set
    private val mutex = Any()
    private var originalExtra = ""

    private val _workerFlow = MutableStateFlow<SpeedtestWorker?>(null)
    val workerFlow: StateFlow<SpeedtestWorker?> = _workerFlow.asStateFlow()

    fun setSpeedtestConfig(config: SpeedtestConfig) {
        synchronized(mutex) {
            if (state != 0) throw IllegalStateException("Cannot change config while running")
            this.config = config
            originalExtra = config.telemetry_extra
        }
    }

    fun getSpeedtestConfig(): SpeedtestConfig {
        synchronized(mutex) {
            return config.clone()
        }
    }

    fun setTelemetryConfig(telemetryConfig: TelemetryConfig) {
        synchronized(mutex) {
            if (state != 0) throw IllegalStateException("Cannot change telemetry config while running")
            this.telemetryConfig = telemetryConfig
        }
    }

    fun addTestPoint(t: TestPoint) {
        synchronized(mutex) {
            servers.add(t)
        }
    }

    fun addTestPoints(t: Array<TestPoint>) {
        synchronized(mutex) {
            for (tp in t) servers.add(tp)
        }
    }

    fun loadServerList(urlOrJson: String): Boolean {
        return try {
            if (urlOrJson.startsWith("http")) {
                 scope.launch {
                     val data = ServerListLoader.read(urlOrJson)
                     if (data != null) {
                         val json = Json { ignoreUnknownKeys = true }
                         val points = json.decodeFromString<List<TestPoint>>(data)
                         addTestPoints(points.toTypedArray())
                     }
                 }
                 true
            } else {
                val json = Json { ignoreUnknownKeys = true }
                val points = json.decodeFromString<List<TestPoint>>(urlOrJson)
                addTestPoints(points.toTypedArray())
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private object ServerListLoader {
        suspend fun read(url: String): String? = withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                BufferedReader(InputStreamReader(connection.getInputStream())).use { it.readText() }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getTestPoints(): Array<TestPoint> {
        synchronized(mutex) {
            return servers.toTypedArray()
        }
    }

    suspend fun selectServer(): TestPoint? {
        val currentServers = synchronized(mutex) { servers.toTypedArray() }
        if (currentServers.isEmpty()) return null
        val best = ServerSelector.select(currentServers, config.ping_connectTimeout, config.ping_soTimeout)
        synchronized(mutex) {
            selectedServer = best
        }
        return best
    }

    fun setSelectedServer(t: TestPoint?) {
        synchronized(mutex) {
            selectedServer = t
        }
    }

    fun getSelectedServer(): TestPoint? {
        synchronized(mutex) {
            return selectedServer
        }
    }

    private var testJob: Job? = null
    fun start() {
        synchronized(mutex) {
            if (state != 0) return
            val srv = selectedServer ?: return
            state = 1
            _workerFlow.value = null
            testJob = scope.launch(Dispatchers.Default) {
                val w = SpeedtestWorker(this, srv, config, telemetryConfig)
                _workerFlow.value = w
                w.runTest()
                state = 0
            }
        }
    }

    fun abort() {
        synchronized(mutex) {
            testJob?.cancel()
            _workerFlow.value?.abort()
            state = 0
        }
    }

    fun getShareURL(testId: String): String? {
        if (telemetryConfig.telemetryLevel == TelemetryConfig.LEVEL_DISABLED || telemetryConfig.shareURL == null || testId.isEmpty()) return null
        return try {
            telemetryConfig.shareURL?.replace("%id%", testId)
        } catch (e: Exception) {
            null
        }
    }
}
