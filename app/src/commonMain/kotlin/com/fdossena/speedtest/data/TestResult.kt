package com.fdossena.speedtest.data

import kotlinx.serialization.Serializable

@Serializable
data class TestResult(
    val id: Int = 0,
    val downloadSpeed: Double,
    val uploadSpeed: Double,
    val ping: Double,
    val jitter: Double,
    val serverName: String,
    val timestamp: Long,
    val testId: String? = null,
    val downloadDataUsed: Long = 0,
    val uploadDataUsed: Long = 0,
    val sponsorName: String? = null,
    val ipInfo: String? = null,
    val source: String = "Unknown"
)

@Serializable
data class ServerStat(
    val id: String,
    val name: String,
    val avgDl: Double,
    val avgUl: Double,
    val avgPing: Double,
    val testCount: Long
)
