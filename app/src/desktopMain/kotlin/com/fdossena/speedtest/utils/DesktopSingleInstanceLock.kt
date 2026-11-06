package com.fdossena.speedtest.utils

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import kotlin.system.exitProcess

object DesktopSingleInstanceLock {

    private var fileChannel: FileChannel? = null
    private var fileLock: FileLock? = null

    fun acquireLockOrExit() {
        try {
            val programFilesDir = File("C:/Program Files/MrSpeed/data")
            val programDataDir = File("C:/ProgramData/MrSpeed/data")
            val localAppDataDir = File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "MrSpeed/data")

            val dir = when {
                programFilesDir.exists() || programFilesDir.mkdirs() -> programFilesDir
                programDataDir.exists() || programDataDir.mkdirs() -> programDataDir
                else -> localAppDataDir.apply { mkdirs() }
            }

            val lockFile = File(dir, "mrspeed_app.lock")
            val randomAccessFile = RandomAccessFile(lockFile, "rw")
            fileChannel = randomAccessFile.channel

            fileLock = fileChannel?.tryLock()

            if (fileLock == null) {
                println("[SingleInstanceLock] Another instance of Mr Speed PRO is already running. Terminating duplicate process.")
                exitProcess(0)
            }

            Runtime.getRuntime().addShutdownHook(Thread {
                releaseLock()
            })
        } catch (e: Exception) {
            println("[SingleInstanceLock] Single instance lock check: ${e.message}")
        }
    }

    private fun releaseLock() {
        try {
            fileLock?.release()
            fileChannel?.close()
        } catch (_: Exception) {}
    }
}
