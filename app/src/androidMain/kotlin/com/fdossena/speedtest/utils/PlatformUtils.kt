package com.fdossena.speedtest.utils

import android.media.MediaPlayer
import android.util.Log
import com.fdossena.speedtest.SpeedtestApplication
import java.util.Locale

actual fun formatDouble(d: Double): String {
    if (d.isNaN() || d.isInfinite() || d <= 0.0) return "0"
    return try {
        String.format(Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')
    } catch (_: Exception) {
        "0"
    }
}

actual val isDesktop: Boolean = false

actual fun playNotificationSound() {
    try {
        val context = SpeedtestApplication.instance
        val afd = try { context.assets.openFd("notification.mp3") } catch (_: Exception) { null }
        if (afd != null) {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { it.release() }
        }
    } catch (e: Exception) {
        Log.e("Sound", "Failed to play sound: ${e.message}")
    }
}
