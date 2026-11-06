package com.fdossena.speedtest.utils

import kotlinx.coroutines.*
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject

object DesktopAutoUpdater {
    private const val CURRENT_VERSION_CODE = 123
    private const val VERSION_CHECK_URL = "https://www.mrspeed.ca/backend/api/app_version.php"

    fun startAutoUpdateChecker(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            // Check shortly after startup (15 seconds)
            delay(15_000)
            while (isActive) {
                try {
                    checkForUpdatesAndApply()
                } catch (e: Exception) {
                    println("[AutoUpdater] Check failed: ${e.message}")
                }
                // Check every 1 hour continuously
                delay(3600_000)
            }
        }
    }

    private suspend fun checkForUpdatesAndApply() {
        val url = URL(VERSION_CHECK_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        val response = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        if (json.optBoolean("success")) {
            val remoteCode = json.optInt("version_code", CURRENT_VERSION_CODE)
            val downloadUrl = json.optString("download_url", "")

            if (remoteCode > CURRENT_VERSION_CODE && downloadUrl.isNotEmpty()) {
                println("[AutoUpdater] New remote version $remoteCode available. Downloading update...")
                downloadAndApplyUpdate(downloadUrl)
            }
        }
    }

    private fun downloadAndApplyUpdate(downloadUrl: String) {
        try {
            val tempDir = System.getProperty("java.io.tmpdir") ?: "."
            val updateFile = File(tempDir, "mrspeed_update_bin")

            // Download new update binary / package
            URL(downloadUrl).openStream().use { input ->
                updateFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (updateFile.exists() && updateFile.length() > 1024) {
                println("[AutoUpdater] Update downloaded successfully. Rebooting app autonomously...")

                val currentBin = try {
                    File(DesktopAutoUpdater::class.java.protectionDomain.codeSource.location.toURI()).absolutePath
                } catch (_: Exception) {
                    ""
                }

                if (currentBin.isNotEmpty()) {
                    val os = System.getProperty("os.name")?.lowercase() ?: ""
                    val isAppImage = currentBin.endsWith(".appimage", ignoreCase = true)

                    if (os.contains("win")) {
                        val scriptFile = File(tempDir, "mrspeed_update.bat")
                        scriptFile.writeText(
                            "@echo off\n" +
                            "timeout /t 3 /nobreak > nul\n" +
                            "copy /y \"${updateFile.absolutePath}\" \"$currentBin\"\n" +
                            "start \"\" java -jar \"$currentBin\"\n" +
                            "del \"%~f0\"\n"
                        )
                        Runtime.getRuntime().exec(arrayOf("cmd.exe", "/c", "start", "/min", scriptFile.absolutePath))
                    } else {
                        // Linux & macOS Unix autonomous updater script
                        val finalUpdateFile = if (isAppImage) {
                            val appImageFile = File(tempDir, "MrSpeed.AppImage")
                            updateFile.renameTo(appImageFile)
                            appImageFile.setExecutable(true)
                            appImageFile
                        } else {
                            updateFile
                        }

                        val scriptFile = File(tempDir, "mrspeed_update.sh")
                        scriptFile.writeText(
                            "#!/bin/bash\n" +
                            "sleep 3\n" +
                            "cp -f \"${finalUpdateFile.absolutePath}\" \"$currentBin\"\n" +
                            "chmod +x \"$currentBin\"\n" +
                            (if (isAppImage) "\"$currentBin\" &" else "java -jar \"$currentBin\" &") + "\n" +
                            "rm -- \"\$0\"\n"
                        )
                        scriptFile.setExecutable(true)
                        Runtime.getRuntime().exec(arrayOf("bash", scriptFile.absolutePath))
                    }

                    // Exit current instance without human help
                    kotlin.system.exitProcess(0)
                }
            }
        } catch (e: Exception) {
            println("[AutoUpdater] Apply failed: ${e.message}")
        }
    }
}
