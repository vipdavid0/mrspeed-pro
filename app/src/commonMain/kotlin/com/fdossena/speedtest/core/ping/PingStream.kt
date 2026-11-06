package com.fdossena.speedtest.core.ping

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.log.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PingStream(
    private val scope: CoroutineScope,
    private val server: String,
    private val path: String,
    private val pings: Int,
    private val errorHandlingMode: String = SpeedtestConfig.ONERROR_ATTEMPT_RESTART,
    private val connectTimeout: Int,
    private val soTimeout: Int,
    private val recvBuffer: Int,
    private val sendBuffer: Int,
    private val log: Logger?
) {
    private val _pongs = MutableSharedFlow<Long>()
    val pongs = _pongs.asSharedFlow()
    
    private var remainingPings = if (pings < 1) 1 else pings
    private var job: Job? = null

    init {
        start()
    }

    private fun start() {
        job = scope.launch {
            while (isActive && remainingPings > 0) {
                var c: Connection? = null
                try {
                    c = Connection.create(server, connectTimeout, soTimeout, recvBuffer, sendBuffer)
                    Pinger.ping(c, path).collect { ns ->
                        _pongs.emit(ns)
                        if (--remainingPings <= 0) {
                            c.close()
                            return@collect
                        }
                    }
                } catch (e: CancellationException) {
                    c?.close()
                    throw e
                } catch (t: Throwable) {
                    log?.l("A pinger died: $t")
                    c?.close()
                    if (errorHandlingMode == SpeedtestConfig.ONERROR_FAIL) {
                        throw t
                    }
                    if (errorHandlingMode == SpeedtestConfig.ONERROR_ATTEMPT_RESTART || errorHandlingMode == SpeedtestConfig.ONERROR_MUST_RESTART) {
                        delay(100)
                        continue
                    }
                    break
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    suspend fun join() {
        job?.join()
    }
}
