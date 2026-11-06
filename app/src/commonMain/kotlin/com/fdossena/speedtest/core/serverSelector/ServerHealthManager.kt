package com.fdossena.speedtest.core.serverSelector

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import kotlin.math.pow

data class ServerHealth(
    val serverId: String,
    val consecutiveFailures: Int = 0,
    val disabledUntilTimestamp: Long = 0L,
    val totalSuccesses: Int = 0,
    val totalFailures: Int = 0,
    val lastPing: Float = -1f,
    val lastDlSpeed: Double = 0.0,
    val lastUlSpeed: Double = 0.0,
    val lastFailureReason: String? = null
) {
    fun isDisabled(now: Long = System.currentTimeMillis()): Boolean {
        return now < disabledUntilTimestamp
    }

    fun remainingDisabledSeconds(now: Long = System.currentTimeMillis()): Long {
        return if (now < disabledUntilTimestamp) (disabledUntilTimestamp - now) / 1000L else 0L
    }

    /**
     * Calculates a composite rating score with DOWNLOAD & UPLOAD speeds as primary priority.
     */
    fun calculateScore(): Double {
        var score = 1000.0

        // PRIMARY PRIORITY: Download & Upload Throughput (Mbps)
        if (lastDlSpeed > 0.0 || lastUlSpeed > 0.0) {
            score += (lastDlSpeed * 100.0) + (lastUlSpeed * 50.0)
        } else {
            score += 2500.0 // Default base throughput score for un-tested servers
        }

        // Secondary modifier: Latency / Ping
        if (lastPing > 0f) {
            score += (1000.0 / lastPing.coerceAtLeast(1f)) * 5.0
        } else {
            score -= 100.0
        }

        // Historical success rate bonus
        val total = totalSuccesses + totalFailures
        if (total > 0) {
            val successRatio = totalSuccesses.toDouble() / total.toDouble()
            score += (successRatio * 500.0)
        }

        // Exponential failure penalty
        if (consecutiveFailures > 0) {
            score -= (consecutiveFailures * 2000.0)
        }

        // Heavy penalty if currently disabled
        if (isDisabled()) {
            score -= 1000000.0
        }

        return score
    }
}

object ServerHealthManager {
    private const val BASE_DISABLE_MINUTES = 5L // Base 5-minute penalty
    private const val MAX_DISABLE_MINUTES = 1440L // Maximum penalty cap: 24 hours

    private val healthMap = mutableMapOf<String, ServerHealth>()
    private val _healthFlow = MutableStateFlow<Map<String, ServerHealth>>(emptyMap())
    val healthFlow: StateFlow<Map<String, ServerHealth>> = _healthFlow.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }

    private fun getServerKeys(server: TestPoint): List<String> = listOf(server.id.toString(), server.name)

    /**
     * Syncs local server health with global state from backend MySQL database.
     */
    fun syncGlobalHealthJson(jsonString: String?) {
        if (jsonString.isNullOrEmpty()) return
        try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val serversObj = root["servers"]?.jsonObject ?: return
            synchronized(healthMap) {
                serversObj.forEach { (key, elem) ->
                    val obj = elem.jsonObject
                    val disabledUntil = obj["disabledUntilTimestamp"]?.jsonPrimitive?.longOrNull ?: 0L
                    val consecutiveFailures = obj["consecutiveFailures"]?.jsonPrimitive?.intOrNull ?: 0
                    val totalSuccesses = obj["totalSuccesses"]?.jsonPrimitive?.intOrNull ?: 0
                    val totalFailures = obj["totalFailures"]?.jsonPrimitive?.intOrNull ?: 0
                    val lastPing = obj["lastPing"]?.jsonPrimitive?.floatOrNull ?: -1f
                    val lastDl = obj["lastDlSpeed"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val lastUl = obj["lastUlSpeed"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val reason = obj["lastFailureReason"]?.jsonPrimitive?.contentOrNull
                    val serverName = obj["serverName"]?.jsonPrimitive?.contentOrNull

                    val existing = healthMap[key] ?: ServerHealth(serverId = key)
                    val updated = existing.copy(
                        consecutiveFailures = maxOf(existing.consecutiveFailures, consecutiveFailures),
                        disabledUntilTimestamp = maxOf(existing.disabledUntilTimestamp, disabledUntil),
                        totalSuccesses = maxOf(existing.totalSuccesses, totalSuccesses),
                        totalFailures = maxOf(existing.totalFailures, totalFailures),
                        lastPing = if (lastPing > 0f) lastPing else existing.lastPing,
                        lastDlSpeed = if (lastDl > 0.0) lastDl else existing.lastDlSpeed,
                        lastUlSpeed = if (lastUl > 0.0) lastUl else existing.lastUlSpeed,
                        lastFailureReason = reason ?: existing.lastFailureReason
                    )
                    healthMap[key] = updated
                    if (!serverName.isNullOrEmpty()) {
                        healthMap[serverName] = updated
                    }
                }
                _healthFlow.value = healthMap.toMap()
            }
        } catch (e: Exception) {
            println("[ServerHealth] Failed to parse global health JSON: ${e.message}")
        }
    }

    /**
     * Records a successful speed test or ping for a server.
     * Resets failure streaks and restores the server to active status.
     */
    fun recordSuccess(server: TestPoint, ping: Float = server.ping, dlSpeed: Double = 0.0, ulSpeed: Double = 0.0) {
        synchronized(healthMap) {
            val keys = getServerKeys(server)
            val primaryKey = keys.first()
            val current = healthMap[primaryKey] ?: healthMap[server.name] ?: ServerHealth(serverId = primaryKey)
            val updatedPing = if (ping > 0f) ping else current.lastPing
            val updatedDl = if (dlSpeed > 0.0) dlSpeed else current.lastDlSpeed
            val updatedUl = if (ulSpeed > 0.0) ulSpeed else current.lastUlSpeed

            val updated = current.copy(
                consecutiveFailures = 0, // Reset failure count on success
                disabledUntilTimestamp = 0L, // Instantly restore active state
                totalSuccesses = current.totalSuccesses + 1,
                lastPing = updatedPing,
                lastDlSpeed = updatedDl,
                lastUlSpeed = updatedUl,
                lastFailureReason = null
            )
            keys.forEach { healthMap[it] = updated }
            _healthFlow.value = healthMap.toMap()
            println("[ServerHealth] Server '${server.name}' (ID: $primaryKey) succeeded. Failure streak cleared.")
        }
    }

    /**
     * Records a test failure or ping timeout for a server.
     * Disables the server for X minutes.
     * Repeated consecutive failures exponentially extend the disable duration!
     */
    fun recordFailure(server: TestPoint, reason: String = "Test Failed") {
        synchronized(healthMap) {
            val keys = getServerKeys(server)
            val primaryKey = keys.first()
            val current = healthMap[primaryKey] ?: healthMap[server.name] ?: ServerHealth(serverId = primaryKey)
            val newFailures = current.consecutiveFailures + 1

            // Exponential backoff: Base * 2^(newFailures - 1)
            val multiplier = 2.0.pow((newFailures - 1).coerceAtMost(10).toDouble()).toLong()
            val disableMinutes = (BASE_DISABLE_MINUTES * multiplier).coerceAtMost(MAX_DISABLE_MINUTES)
            val disableDurationMs = disableMinutes * 60 * 1000L
            val disabledUntil = System.currentTimeMillis() + disableDurationMs

            val updated = current.copy(
                consecutiveFailures = newFailures,
                disabledUntilTimestamp = disabledUntil,
                totalFailures = current.totalFailures + 1,
                lastFailureReason = reason
            )
            keys.forEach { healthMap[it] = updated }
            _healthFlow.value = healthMap.toMap()
            println("[ServerHealth] Server '${server.name}' (ID: $primaryKey) failed (Streak: $newFailures). Disabled for $disableMinutes mins.")
        }
    }

    /**
     * Returns whether a server is currently temporarily disabled.
     */
    fun isServerDisabled(server: TestPoint): Boolean {
        return synchronized(healthMap) {
            val health = healthMap[server.id.toString()] ?: healthMap[server.name]
            health?.isDisabled() ?: false
        }
    }

    /**
     * Returns a set of all currently disabled server IDs and names.
     */
    fun getDisabledServerIds(): Set<String> {
        val now = System.currentTimeMillis()
        return synchronized(healthMap) {
            healthMap.filter { it.value.isDisabled(now) }.keys.toSet()
        }
    }

    /**
     * Rates and ranks all servers from Best to Worst based on composite rating score.
     */
    fun getRankedServers(servers: List<TestPoint>): List<TestPoint> {
        return synchronized(healthMap) {
            servers.sortedByDescending { server ->
                val health = healthMap[server.id.toString()] ?: healthMap[server.name] ?: ServerHealth(serverId = server.id.toString(), lastPing = server.ping)
                health.calculateScore()
            }
        }
    }

    /**
     * Returns the top 5 highest speed active servers based on performance rating.
     */
    fun getTop5SpeedServers(servers: List<TestPoint>): List<TestPoint> {
        val active = synchronized(healthMap) {
            servers.filter { !isServerDisabled(it) }
        }
        val candidate = if (active.isNotEmpty()) active else servers
        return getRankedServers(candidate).take(5)
    }

    /**
     * Selects a server at random ONLY from the best (top-performing, healthy) servers pool.
     * Automatically excludes currently disabled servers.
     */
    fun selectBestRandomServer(servers: List<TestPoint>): TestPoint? {
        if (servers.isEmpty()) return null

        // Exclude currently disabled servers
        var activeServers = synchronized(healthMap) {
            servers.filter { !isServerDisabled(it) }
        }

        // Fallback to all servers if every single server is disabled
        if (activeServers.isEmpty()) {
            activeServers = servers
        }

        // Rank active servers from best to worst
        val ranked = getRankedServers(activeServers)

        // Select from the top tier best servers pool (top 25% or up to 5 best servers)
        val poolSize = (ranked.size * 0.25).toInt().coerceIn(1, 5)
        val bestPool = ranked.take(poolSize)

        return bestPool.randomOrNull() ?: ranked.firstOrNull()
    }

    /**
     * Retrieves health record for a specific server.
     */
    fun getHealth(server: TestPoint): ServerHealth {
        return synchronized(healthMap) {
            healthMap[server.id.toString()] ?: healthMap[server.name] ?: ServerHealth(serverId = server.id.toString(), lastPing = server.ping)
        }
    }
}
