package com.fdossena.speedtest.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DriveTestLog(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val downloadSpeed: Double,
    val uploadSpeed: Double,
    val ping: Double,
    val networkType: String, // 5G, 4G, Wi-Fi
    val signalStrength: Int, // 0-100%
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isDarkSpot: Boolean = downloadSpeed < 5.0
)

object DriveTestManager {
    private val logs = mutableListOf<DriveTestLog>()
    private val _logsFlow = MutableStateFlow<List<DriveTestLog>>(emptyList())
    val logsFlow: StateFlow<List<DriveTestLog>> = _logsFlow.asStateFlow()

    fun logDriveTest(
        dl: Double,
        ul: Double,
        ping: Double,
        netType: String,
        signal: Int,
        lat: Double = 0.0,
        lng: Double = 0.0
    ) {
        val entry = DriveTestLog(
            downloadSpeed = dl,
            uploadSpeed = ul,
            ping = ping,
            networkType = netType,
            signalStrength = signal,
            latitude = lat,
            longitude = lng
        )
        synchronized(logs) {
            logs.add(0, entry)
            if (logs.size > 100) logs.removeAt(logs.size - 1)
            _logsFlow.value = logs.toList()
        }
    }

    fun getDarkSpotsCount(): Int {
        return synchronized(logs) { logs.count { it.isDarkSpot } }
    }
}
