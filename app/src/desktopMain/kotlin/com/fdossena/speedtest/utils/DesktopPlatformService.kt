package com.fdossena.speedtest.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fdossena.speedtest.data.RealCellTower
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioSystem

class DesktopPlatformService : PlatformService {
    override fun isTv(): Boolean = false
    override fun getDeviceModel(): String = "Desktop Windows PC"
    override fun triggerHaptic() {}
    override fun updateAppIcon(themeMode: Int): Job? = null
    override suspend fun getWifiSignalLevel(): Int = 4
    override fun getHardwareStats(): Pair<Int, Int> = Pair(15, 45)
    override fun isVpnActive(): Boolean = false

    override fun getTopBandwidthProcess(): String {
        return try {
            val os = System.getProperty("os.name")?.lowercase() ?: ""
            if (os.contains("win")) {
                "System / Chrome / Steam"
            } else {
                "systemd / NetworkManager"
            }
        } catch (_: Exception) {
            "System Process"
        }
    }

    override fun isEthernetConnected(): Boolean = true

    override fun showToast(title: String, message: String, isLightMode: Boolean) {
        try {
            DesktopToastManager.showToast(title, message, isLightMode)
            DesktopTrayManager.showToastNotification(title, message, isLightMode)
        } catch (_: Exception) {}
    }

    override fun getCompassHeading(): Float = 0f
    override fun getPreciseLocation(): Pair<Double, Double>? = Pair(42.9912, -81.2514)

    override fun announceForAccessibility(text: String) {
        try {
            println("[Self-Hosted AI Speech]: $text")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val encoded = URLEncoder.encode(text, "UTF-8")
                    val apiUrl = URL("https://mrspeed.ca/backend/api/ai_tts.php?text=$encoded")
                    val conn = apiUrl.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    if (conn.responseCode in 200..299) {
                        val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(jsonStr)
                        val audioUrlStr = json.optString("audioUrl", "")
                        if (audioUrlStr.isNotBlank()) {
                            val audioStream = URL(audioUrlStr).openStream()
                            val clip = AudioSystem.getClip()
                            val ais = AudioSystem.getAudioInputStream(BufferedInputStream(audioStream))
                            clip.open(ais)
                            clip.start()
                        }
                    }
                } catch (e: Exception) {
                    println("[AI Speech Error]: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("[Announce error]: ${e.message}")
        }
    }

    override suspend fun verifyLiveEgressIp(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.ipify.org?format=json")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    if (text.contains("\"ip\"")) {
                        text.substringAfter("\"ip\":\"").substringBefore("\"").trim()
                    } else text.trim()
                } else null
            } catch (e: Throwable) {
                null
            }
        }
    }

    @Composable
    override fun BannerAdView(modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    Color(0xFF00E5FF).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡ MR SPEED PRO DESKTOP • SUPREME HIGH-SPEED TEST NETWORK",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
