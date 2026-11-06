package com.fdossena.speedtest.core

import android.content.Context
import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.config.TelemetryConfig
import com.fdossena.speedtest.core.serverSelector.ServerHealthManager
import com.fdossena.speedtest.core.serverSelector.TestPoint
import com.fdossena.speedtest.core.worker.SpeedtestWorker
import com.fdossena.speedtest.data.*
import com.fdossena.speedtest.ui.AndroidAssetReader
import com.fdossena.speedtest.utils.NetworkHelper
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

class SpeedtestManager(
    private val context: Context,
    private val repository: HistoryRepository,
    private val syncManager: SyncProvider,
    private val settings: SettingsProvider
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val assetReader = AndroidAssetReader()
    private val networkHelper = NetworkHelper(context)

    suspend fun runSilentTest(
        customSource: String? = null,
        onProgress: ((String, Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        if (!networkHelper.isInternetAvailable()) {
            onProgress?.invoke("No Internet Available", 0)
            return@withContext
        }

        try {
            onProgress?.invoke("Initializing Engine...", 5)
            val json = Json { ignoreUnknownKeys = true }
            
            val configStr = assetReader.readFile("SpeedtestConfig.json")
            val config = try {
                json.decodeFromString<SpeedtestConfig>(configStr)
            } catch (e: Exception) {
                SpeedtestConfig()
            }
            
            val telemetryStr = assetReader.readFile("TelemetryConfig.json")
            val telemetryConfig = try {
                json.decodeFromString<TelemetryConfig>(telemetryStr)
            } catch (e: Exception) {
                TelemetryConfig()
            }

            val st = Speedtest(scope)
            st.setSpeedtestConfig(config)
            st.setTelemetryConfig(telemetryConfig)

            val serverListStr = assetReader.readFile("ServerList.json")
            st.loadServerList(serverListStr)

            onProgress?.invoke("Selecting Best Server...", 10)
            val best = st.selectServer() ?: return@withContext
            st.setSelectedServer(best)
            
            config.dl_parallelStreams = settings.dlParallelStreams
            config.ul_parallelStreams = settings.ulParallelStreams
            config.count_ping = settings.pingCount
            config.useMebibits = settings.useMebibits

            val completionDeferred = CompletableDeferred<Unit>()
            
            var dlSpeed = 0.0
            var ulSpeed = 0.0
            var ping = 0.0
            var jitter = 0.0
            var dlData = 0L
            var ulData = 0L
            var testId = ""

            st.start()
            
            val job = launch {
                st.workerFlow.collect { worker ->
                    if (worker == null) return@collect
                    
                    launch { 
                        worker.dlFlow.collect { (s, p) -> 
                            dlSpeed = s
                            dlData = worker.dlDataUsed
                            onProgress?.invoke("Downloading... ${String.format(Locale.ENGLISH, "%.1f", s)} Mbps", (15 + (p * 40)).toInt())
                        } 
                    }
                    launch { 
                        worker.ulFlow.collect { (s, p) -> 
                            ulSpeed = s
                            ulData = worker.ulDataUsed
                            onProgress?.invoke("Uploading... ${String.format(Locale.ENGLISH, "%.1f", s)} Mbps", (55 + (p * 40)).toInt())
                        } 
                    }
                    launch { 
                        worker.pingFlow.collect { (p, j, pr) -> 
                            ping = p
                            jitter = j
                            onProgress?.invoke("Pinging... ${p.toInt()}ms", (10 + (pr * 5)).toInt())
                        } 
                    }
                    launch { worker.testIdFlow.collect { id -> testId = id } }
                    launch { worker.onEndFlow.collect { completionDeferred.complete(Unit) } }
                    launch { worker.errorFlow.collect { completionDeferred.completeExceptionally(Exception(it)) } }
                }
            }

            try {
                completionDeferred.await()
                ServerHealthManager.recordSuccess(best, dlSpeed = dlSpeed, ulSpeed = ulSpeed)
            } catch (e: Exception) {
                ServerHealthManager.recordFailure(best, reason = "Background Test Error: ${e.message}")
                throw e
            } finally {
                job.cancel()
                st.abort()
            }

            val result = TestResult(
                downloadSpeed = dlSpeed,
                uploadSpeed = ulSpeed,
                ping = ping,
                jitter = jitter,
                serverName = best.name,
                timestamp = System.currentTimeMillis(),
                testId = testId,
                downloadDataUsed = dlData,
                uploadDataUsed = ulData,
                sponsorName = best.sponsorName,
                source = customSource ?: "Automated Daily Routine"
            )
            
            repository.insert(result)
            syncManager.uploadResult(result)
            onProgress?.invoke("Test Complete: ${String.format(Locale.ENGLISH, "%.1f", dlSpeed)} Mbps", 100)
            
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress?.invoke("Test Failed: ${e.message}", 0)
        }
    }
}
