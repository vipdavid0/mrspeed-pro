package com.fdossena.speedtest.utils

import java.io.File
import kotlin.system.exitProcess

object DesktopDataWipe {

    fun purgeAllApplicationDataAndExit() {
        println("[DataWipe] Initiating complete uninstallation & deep data wipe...")

        val os = System.getProperty("os.name")?.lowercase() ?: ""

        try {
            // 1. Wipe Data Directories across C:\Program Files\MrSpeed, C:\ProgramData\MrSpeed, %LOCALAPPDATA%\MrSpeed, ~/.local/share/MrSpeed, ~/Library/Application Support/MrSpeed
            val dataDirs = listOf(
                File("C:/Program Files/MrSpeed/data"),
                File("C:/ProgramData/MrSpeed/data"),
                File(System.getenv("LOCALAPPDATA") ?: "", "MrSpeed"),
                File(System.getProperty("user.home"), ".local/share/MrSpeed"),
                File(System.getProperty("user.home"), ".config/MrSpeed"),
                File(System.getProperty("user.home"), "Library/Application Support/MrSpeed")
            )

            dataDirs.forEach { dir ->
                if (dir.exists()) {
                    dir.deleteRecursively()
                    println("[DataWipe] Deleted data directory: ${dir.absolutePath}")
                }
            }

            // 2. Platform-Specific Shortcut & Registry Cleanup
            if (os.contains("win")) {
                try {
                    val removeCmd = arrayOf("powershell.exe", "-Command", 
                        "Remove-ItemProperty -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run' -Name 'MrSpeedPRO' -ErrorAction SilentlyContinue; " +
                        "\$d = [System.Environment]::GetFolderPath('Desktop'); " +
                        "\$s = Join-Path ([System.Environment]::GetFolderPath('StartMenu')) 'Programs\\Mr Speed PRO'; " +
                        "Remove-Item \"\$d\\Mr Speed Desktop.lnk\" -ErrorAction SilentlyContinue; " +
                        "Remove-Item \"\$d\\Mr Pro Network Tools.lnk\" -ErrorAction SilentlyContinue; " +
                        "Remove-Item \"\$s\" -Recurse -Force -ErrorAction SilentlyContinue;"
                    )
                    val p = Runtime.getRuntime().exec(removeCmd)
                    p.waitFor()
                } catch (_: Exception) {}
            } else if (os.contains("linux") || os.contains("unix")) {
                val homeDir = System.getProperty("user.home") ?: ""
                if (homeDir.isNotEmpty()) {
                    listOf(
                        File(homeDir, ".local/bin/mrspeed"),
                        File(homeDir, ".local/bin/mrspeed-protools"),
                        File(homeDir, ".local/share/applications/mrspeed-desktop.desktop"),
                        File(homeDir, ".local/share/applications/mrspeed-protools.desktop"),
                        File(homeDir, "Desktop/mrspeed-desktop.desktop"),
                        File(homeDir, "Desktop/mrspeed-protools.desktop")
                    ).forEach { f -> if (f.exists()) f.delete() }
                }
            } else if (os.contains("mac") || os.contains("darwin")) {
                val homeDir = System.getProperty("user.home") ?: ""
                if (homeDir.isNotEmpty()) {
                    listOf(
                        File(homeDir, ".local/bin/mrspeed"),
                        File(homeDir, ".local/bin/mrspeed-protools"),
                        File(homeDir, "Library/Preferences/com.fdossena.speedtest.plist")
                    ).forEach { f -> if (f.exists()) f.delete() }
                }
            }

            println("[DataWipe] Data wipe complete. Exiting process.")
        } catch (e: Exception) {
            println("[DataWipe] Error during data wipe: ${e.message}")
        }

        exitProcess(0)
    }
}
