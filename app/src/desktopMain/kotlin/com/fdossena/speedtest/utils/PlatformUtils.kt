package com.fdossena.speedtest.utils

import java.util.Locale
import java.awt.Toolkit
import javazoom.jl.player.Player
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import com.mr.speed.test.Res
import kotlinx.coroutines.*
import org.jetbrains.compose.resources.ExperimentalResourceApi

actual fun formatDouble(d: Double): String {
    return String.format(Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')
}

actual val isDesktop: Boolean = true

private val soundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

@OptIn(ExperimentalResourceApi::class)
actual fun playNotificationSound() {
    try {
        println("Sound: Finishing speed test. Playing alert.")
        // 1. System alert
        Toolkit.getDefaultToolkit().beep()
        
        // 2. Premium MP3 alert
        soundScope.launch {
            try {
                // Try several ways to get the bytes
                val bytes = try {
                    Res.readBytes("files/notification.mp3")
                } catch (e: Exception) {
                    null
                }
                
                if (bytes != null && bytes.isNotEmpty()) {
                    println("Sound: Notification loaded via Res API (${bytes.size} bytes)")
                    val player = Player(BufferedInputStream(ByteArrayInputStream(bytes)))
                    player.play()
                } else {
                    // Manual classpath fallback
                    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("composeResources/com.mr.speed.test/files/notification.mp3")
                        ?: Thread.currentThread().contextClassLoader.getResourceAsStream("notification.mp3")
                    
                    if (stream != null) {
                        println("Sound: Notification loaded via ClassLoader")
                        val player = Player(BufferedInputStream(stream))
                        player.play()
                    } else {
                        println("Sound: WARNING - Could not find notification.mp3")
                    }
                }
            } catch (e: Exception) {
                println("Sound: Playback ERROR: ${e.message}")
            }
        }
    } catch (e: Exception) {
        println("Sound: Toolkit beep failed")
    }
}
