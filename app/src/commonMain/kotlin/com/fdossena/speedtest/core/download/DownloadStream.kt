package com.fdossena.speedtest.core.download

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.log.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DownloadStream(
    private val scope: CoroutineScope,
    private val server: String,
    private val path: String,
    private val ckSize: Int,
    private val errorHandlingMode: String = SpeedtestConfig.ONERROR_ATTEMPT_RESTART,
    private val connectTimeout: Int,
    private val soTimeout: Int,
    private val recvBuffer: Int,
    private val sendBuffer: Int,
    private val log: Logger?
) {
    private val _downloaded = MutableStateFlow(0L)
    val downloaded = _downloaded.asStateFlow()
    
    private var previouslyDownloaded = 0L
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            while (isActive) {
                var c: Connection? = null
                try {
                    c = Connection.create(server, connectTimeout, soTimeout, recvBuffer, sendBuffer)
                    Downloader.download(c, path, ckSize).collect { current ->
                        _downloaded.value = previouslyDownloaded + current
                    }
                    break 
                } catch (e: CancellationException) {
                    c?.close()
                    throw e
                } catch (t: Throwable) {
                    log?.l("A downloader died: $t")
                    c?.close()
                    if (errorHandlingMode == SpeedtestConfig.ONERROR_FAIL) {
                        throw t
                    }
                    if (errorHandlingMode == SpeedtestConfig.ONERROR_ATTEMPT_RESTART || errorHandlingMode == SpeedtestConfig.ONERROR_MUST_RESTART) {
                        previouslyDownloaded = _downloaded.value
                        delay(500)
                        continue
                    }
                    break
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun resetCounter() {
        previouslyDownloaded = 0
        _downloaded.value = 0
    }

    suspend fun join() {
        job?.join()
    }
}
