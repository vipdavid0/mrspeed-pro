package com.fdossena.speedtest.utils

import com.fdossena.speedtest.data.RealCellTower
import kotlinx.coroutines.Job

interface PlatformService {
    fun isTv(): Boolean
    fun getDeviceModel(): String
    fun triggerHaptic()
    fun updateAppIcon(themeMode: Int): Job?
    suspend fun getWifiSignalLevel(): Int
    fun getHardwareStats(): Pair<Int, Int> // CPU, RAM
    fun isVpnActive(): Boolean
    fun getCellularDetails(): String = "Cellular N/A"
    fun checkHardwareBottleneck(): String? = null
    fun getTopBandwidthProcess(): String? = null
    fun isEthernetConnected(): Boolean = false
    fun showToast(title: String, message: String, isLightMode: Boolean = false) {}
    fun getCompassHeading(): Float = 0f
    fun getPreciseLocation(): Pair<Double, Double>? = null
    suspend fun fetchRealCellTowers(userLat: Double = 42.9912, userLon: Double = -81.2514): List<RealCellTower> = emptyList()
    suspend fun verifyLiveEgressIp(): String? = null
    fun announceForAccessibility(text: String) {}

    @androidx.compose.runtime.Composable
    fun BannerAdView(modifier: androidx.compose.ui.Modifier) {}

    fun showTvVideoAd(onAdFinished: () -> Unit) {
        onAdFinished()
    }
}
