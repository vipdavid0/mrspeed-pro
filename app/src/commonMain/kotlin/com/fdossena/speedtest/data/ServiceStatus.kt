package com.fdossena.speedtest.data

import kotlinx.serialization.Serializable

enum class StatusLevel {
    UP, ISSUES, DOWN, UNKNOWN
}

@Serializable
data class OutageEvent(
    val timestamp: Long,
    val durationMinutes: Int,
    val type: StatusLevel
)

@Serializable
data class ServiceStatus(
    val id: String,
    val name: String,
    val url: String = "",
    val statusUrl: String? = null,
    val category: String,
    val currentStatus: StatusLevel,
    val reportCount: Int,
    val historicalReports: List<Int>,
    val outageHistory: List<OutageEvent> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isLimited: Boolean = false
)
