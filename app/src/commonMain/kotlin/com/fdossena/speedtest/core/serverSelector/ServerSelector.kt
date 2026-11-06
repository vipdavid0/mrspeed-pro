package com.fdossena.speedtest.core.serverSelector

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.ping.Pinger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.*

object ServerSelector {
    private const val PARALLELISM = 12
    private const val PINGS = 3
    private const val SLOW_THRESHOLD = 500

    suspend fun select(servers: Array<TestPoint>, connectTimeout: Int, soTimeout: Int): TestPoint? = withTimeoutOrNull(8000L) {
        supervisorScope {
            val semaphore = Semaphore(PARALLELISM)
            
            // Filter out disabled servers unless all servers are currently disabled
            val disabledIds = ServerHealthManager.getDisabledServerIds()
            val candidateServers = servers.filter { it.id.toString() !in disabledIds }.ifEmpty { servers.toList() }.toTypedArray()

            val jobs = candidateServers.map { tp ->
                launch {
                    semaphore.withPermit {
                        var c: Connection? = null
                        try {
                            c = Connection.create(tp.server, connectTimeout.coerceAtMost(2500), soTimeout.coerceAtMost(2500), -1, -1)
                            var bestPing = -1f
                            
                            withTimeout(2500L) {
                                Pinger.ping(c, tp.pingURL).take(PINGS).collect { ns ->
                                    val p = ns / 1000000f
                                    if (bestPing == -1f || p < bestPing) bestPing = p
                                    if (p >= SLOW_THRESHOLD) {
                                        cancel("Slow server")
                                    }
                                }
                            }
                            tp.ping = bestPing
                            if (bestPing > 0f) {
                                ServerHealthManager.recordSuccess(tp, ping = bestPing)
                            }
                        } catch (e: Exception) {
                            ServerHealthManager.recordFailure(tp, reason = "Ping Timeout/Error")
                        } finally {
                            c?.close()
                        }
                    }
                }
            }
            jobs.joinAll()
            
            // Select randomly ONLY from the top-performing, lowest-latency best servers
            ServerHealthManager.selectBestRandomServer(candidateServers.toList())
        }
    } ?: ServerHealthManager.selectBestRandomServer(servers.toList()) ?: servers.firstOrNull()
}
