package com.fdossena.speedtest.data

data class RealCellTower(
    val cellId: Long,
    val mcc: Int,
    val mnc: Int,
    val lac: Int,
    val type: String, // "5G NR Sub-6", "4G LTE", "5G mmWave"
    val rsrpDbm: Int,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val distanceKm: Double = 0.0,
    val bearingDeg: Float = 0f,
    val isActive: Boolean = false,
    val bandName: String = "LTE / 5G Band"
)
