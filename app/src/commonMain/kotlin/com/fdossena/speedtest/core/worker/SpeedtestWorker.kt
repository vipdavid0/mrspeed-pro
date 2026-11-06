package com.fdossena.speedtest.core.worker

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.base.Utils
import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.config.TelemetryConfig
import com.fdossena.speedtest.core.download.DownloadStream
import com.fdossena.speedtest.core.getIP.GetIP
import com.fdossena.speedtest.core.log.Logger
import com.fdossena.speedtest.core.ping.PingStream
import com.fdossena.speedtest.core.serverSelector.TestPoint
import com.fdossena.speedtest.core.telemetry.Telemetry
import com.fdossena.speedtest.core.upload.UploadStream
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class SpeedtestWorker(
    private val externalScope: CoroutineScope,
    private val backend: TestPoint,
    config: SpeedtestConfig?,
    telemetryConfig: TelemetryConfig?
) {
    private val config: SpeedtestConfig = config ?: SpeedtestConfig()
    private val telemetryConfig: TelemetryConfig = telemetryConfig ?: TelemetryConfig()
    
    private val _dlFlow = MutableStateFlow(Pair(0.0, 0.0))
    val dlFlow = _dlFlow.asStateFlow()
    
    private val _ulFlow = MutableStateFlow(Pair(0.0, 0.0))
    val ulFlow = _ulFlow.asStateFlow()
    
    private val _pingFlow = MutableStateFlow(Triple(0.0, 0.0, 0.0))
    val pingFlow = _pingFlow.asStateFlow()

    private val _loadedPingFlow = MutableStateFlow(Pair(0.0, 0.0)) // DL Ping, UL Ping
    val loadedPingFlow = _loadedPingFlow.asStateFlow()
    
    private val _ipFlow = MutableStateFlow("")
    val ipFlow = _ipFlow.asStateFlow()
    
    private val _testIdFlow = MutableStateFlow("")
    val testIdFlow = _testIdFlow.asStateFlow()

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()

    private val _onEndFlow = MutableSharedFlow<Unit>()
    val onEndFlow = _onEndFlow.asSharedFlow()

    private val _phaseFlow = MutableStateFlow<TestPhase>(TestPhase.NONE)
    val phaseFlow = _phaseFlow.asStateFlow()

    enum class TestPhase { NONE, IP, PING, DOWNLOAD, UPLOAD }

    private var dl = -1.0
    private var ul = -1.0
    private var ping = -1.0
    private var jitter = -1.0
    private var ipIsp = ""
    private val log = Logger()
    private var isAborted = false

    fun abort() {
        isAborted = true
    }

    val dlDataUsed: Long
        get() = streamsDl?.sumOf { it.downloaded.value } ?: 0L
    
    val ulDataUsed: Long
        get() = streamsUl?.sumOf { it.uploaded.value } ?: 0L

    private var streamsDl: List<DownloadStream>? = null
    private var streamsUl: List<UploadStream>? = null

    suspend fun runTest() = coroutineScope {
        log.l("Test started")
        try {
            for (t in config.test_order.toCharArray()) {
                if (!isActive || isAborted) break
                when (t) {
                    '_' -> delay(1000)
                    'I' -> { _phaseFlow.value = TestPhase.IP; getIP() }
                    'D' -> { _phaseFlow.value = TestPhase.DOWNLOAD; dlTest() }
                    'U' -> { _phaseFlow.value = TestPhase.UPLOAD; ulTest() }
                    'P' -> { _phaseFlow.value = TestPhase.PING; pingTest() }
                }
            }
        } catch (e: CancellationException) {
            log.l("Test cancelled")
            throw e
        } catch (t: Throwable) {
            log.l("Test error: $t")
            _errorFlow.emit(t.toString())
        } finally {
            _phaseFlow.value = TestPhase.NONE
            withContext(NonCancellable) {
                try {
                    sendTelemetry()
                } catch (t: Throwable) {
                }
                _onEndFlow.emit(Unit)
            }
        }
    }

    private suspend fun getIP() {
        val start = System.currentTimeMillis()
        try {
            val url = URL("https://mrspeed.ca/isp_service.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            ipIsp = text
            val processed = try {
                val json = Json.parseToJsonElement(text).jsonObject
                json["processedString"]?.jsonPrimitive?.content ?: text
            } catch (t: Throwable) {
                text
            }
            log.l("GetIP: $processed (took ${System.currentTimeMillis() - start}ms)")
            _ipFlow.value = processed
        } catch (t: Throwable) {
            log.l("GetIP: FAILED (took ${System.currentTimeMillis() - start}ms)")
            if (config.errorHandlingMode == SpeedtestConfig.ONERROR_FAIL) {
                throw t
            }
        }
    }

    private suspend fun dlTest() = coroutineScope {
        val start = System.currentTimeMillis()
        _dlFlow.value = Pair(0.0, 0.0)
        
        val dlPinger = launch {
            backgroundPing { p ->
                _loadedPingFlow.update { it.copy(first = p) }
            }
        }

        val streams = List(config.dl_parallelStreams) { index ->
            DownloadStream(
                this,
                backend.server,
                backend.dlURL,
                config.dl_ckSize,
                config.errorHandlingMode,
                config.dl_connectTimeout,
                config.dl_soTimeout,
                config.dl_recvBuffer,
                config.dl_sendBuffer,
                log
            ).also {
                launch {
                    delay(index * config.dl_streamDelay.toLong())
                    it.start()
                }
            }
        }
        streamsDl = streams

        var graceTimeDone = false
        var startT = System.currentTimeMillis()
        var bonusT: Long = 0
        
        while (isActive) {
            val t = (System.currentTimeMillis() - startT).toDouble()
            if (!graceTimeDone && t >= config.dl_graceTime * 1000) {
                graceTimeDone = true
                streams.forEach { it.resetCounter() }
                startT = System.currentTimeMillis()
                continue
            }
            
            val limitT = (config.time_dl_max * 1000).toDouble()
            if (t + bonusT >= limitT) {
                break
            }
            
            var totDownloaded = 0L
            streams.forEach { totDownloaded += it.downloaded.value }
            var speed = totDownloaded / ((if (t < 100) 100.0 else t) / 1000.0)
            if (config.time_auto) {
                val b = (2.5 * speed) / 100000.0
                bonusT += (if (b > 200) 200.0 else b).toLong()
            }
            val progress = (t + bonusT) / limitT
            speed = (speed * 8 * config.overheadCompensationFactor) / (if (config.useMebibits) 1048576.0 else 1000000.0)
            dl = speed
            
            _dlFlow.value = Pair(if(graceTimeDone) dl else 0.0, if (progress > 1) 1.0 else progress)
            
            delay(100)
        }
        streams.forEach { it.stop() }
        streams.forEach { it.join() }
        dlPinger.cancel()
        
        log.l("Download: $dl (took ${System.currentTimeMillis() - start}ms)")
        _dlFlow.value = Pair(dl, 1.0)
    }

    private suspend fun ulTest() = coroutineScope {
        val start = System.currentTimeMillis()
        _ulFlow.value = Pair(0.0, 0.0)
        
        val ulPinger = launch {
            backgroundPing { p ->
                _loadedPingFlow.update { it.copy(second = p) }
            }
        }

        val streams = List(config.ul_parallelStreams) { index ->
            UploadStream(
                this,
                backend.server,
                backend.ulURL,
                config.ul_ckSize,
                config.errorHandlingMode,
                config.ul_connectTimeout,
                config.ul_soTimeout,
                config.ul_recvBuffer,
                config.ul_sendBuffer,
                log
            ).also {
                launch {
                    delay(index * config.ul_streamDelay.toLong())
                    it.start()
                }
            }
        }
        streamsUl = streams

        var graceTimeDone = false
        var startT = System.currentTimeMillis()
        var bonusT: Long = 0
        
        while (isActive) {
            val t = (System.currentTimeMillis() - startT).toDouble()
            if (!graceTimeDone && t >= config.ul_graceTime * 1000) {
                graceTimeDone = true
                streams.forEach { it.resetCounter() }
                startT = System.currentTimeMillis()
                continue
            }
            
            val limitT = (config.time_ul_max * 1000).toDouble()
            if (t + bonusT >= limitT) {
                break
            }
            
            var totUploaded = 0L
            streams.forEach { totUploaded += it.uploaded.value }
            var speed = totUploaded / ((if (t < 100) 100.0 else t) / 1000.0)
            if (config.time_auto) {
                val b = (2.5 * speed) / 100000.0
                bonusT += (if (b > 200) 200.0 else b).toLong()
            }
            val progress = (t + bonusT) / limitT
            speed = (speed * 8 * config.overheadCompensationFactor) / (if (config.useMebibits) 1048576.0 else 1000000.0)
            ul = speed
            
            _ulFlow.value = Pair(if(graceTimeDone) ul else 0.0, if (progress > 1) 1.0 else progress)
            
            delay(100)
        }
        streams.forEach { it.stop() }
        streams.forEach { it.join() }
        ulPinger.cancel()
        
        log.l("Upload: $ul (took ${System.currentTimeMillis() - start}ms)")
        _ulFlow.value = Pair(ul, 1.0)
    }

    private suspend fun backgroundPing(onPing: (Double) -> Unit) {
        val ps = PingStream(
            externalScope,
            backend.server,
            backend.pingURL,
            100,
            config.errorHandlingMode,
            config.ping_connectTimeout,
            config.ping_soTimeout,
            config.ping_recvBuffer,
            config.ping_sendBuffer,
            log
        )
        try {
            ps.pongs.collect { ns ->
                onPing(ns / 1000000.0)
                delay(500)
            }
        } catch (e: Exception) {
        } finally {
            ps.stop()
        }
    }

    private suspend fun pingTest() = coroutineScope {
        val start = System.currentTimeMillis()
        _pingFlow.value = Triple(0.0, 0.0, 0.0)
        
        val ps = PingStream(
            this,
            backend.server,
            backend.pingURL,
            config.count_ping,
            config.errorHandlingMode,
            config.ping_connectTimeout,
            config.ping_soTimeout,
            config.ping_recvBuffer,
            config.ping_sendBuffer,
            log
        )
        
        var minPing = Double.MAX_VALUE
        var prevPing = -1.0
        var counter = 0
        
        try {
            ps.pongs.take(config.count_ping).collect { ns ->
                counter++
                val ms = ns / 1000000.0
                if (ms < minPing) minPing = ms
                ping = minPing
                if (prevPing == -1.0) {
                    jitter = 0.0
                } else {
                    val j = Math.abs(ms - prevPing)
                    jitter = if (j > jitter) jitter * 0.3 + j * 0.7 else jitter * 0.8 + j * 0.2
                }
                prevPing = ms
                val progress = counter / config.count_ping.toDouble()
                _pingFlow.value = Triple(ping, jitter, if (progress > 1) 1.0 else progress)
            }
        } finally {
            ps.stop()
        }
        
        val safePing = if (ping == Double.MAX_VALUE || ping < 0.0 || ping.isNaN() || ping.isInfinite()) 0.0 else ping
        val safeJitter = if (jitter < 0.0 || jitter.isNaN() || jitter.isInfinite()) 0.0 else jitter
        log.l("Ping: $safePing $safeJitter (took ${System.currentTimeMillis() - start}ms)")
        _pingFlow.value = Triple(safePing, safeJitter, 1.0)
    }

    private suspend fun sendTelemetry() {
        if (telemetryConfig.telemetryLevel == TelemetryConfig.LEVEL_DISABLED) return
        val server = telemetryConfig.server
        if (server.isNullOrEmpty()) return
        try {
            val c = Connection.create(server, 3000, 3000, -1, -1)
            val data = Telemetry.send(
                c,
                telemetryConfig.path,
                telemetryConfig.telemetryLevel,
                ipIsp,
                config.telemetry_extra,
                if (dl == -1.0 || dl.isNaN() || dl.isInfinite()) "" else String.format(Locale.ENGLISH, "%.2f", dl),
                if (ul == -1.0 || ul.isNaN() || ul.isInfinite()) "" else String.format(Locale.ENGLISH, "%.2f", ul),
                if (ping == -1.0 || ping == Double.MAX_VALUE || ping.isNaN() || ping.isInfinite()) "" else String.format(Locale.ENGLISH, "%.2f", ping),
                if (jitter == -1.0 || jitter.isNaN() || jitter.isInfinite()) "" else String.format(Locale.ENGLISH, "%.2f", jitter),
                log.getLog()
            )
            if (data?.startsWith("id") == true) {
                _testIdFlow.value = data.split(" ")[1]
            }
        } catch (t: Throwable) {
            log.l("Telemetry failed: $t")
        }
    }
}
