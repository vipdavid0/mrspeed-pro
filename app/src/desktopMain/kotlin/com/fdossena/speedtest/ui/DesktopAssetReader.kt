package com.fdossena.speedtest.ui

import java.io.File
import java.nio.charset.StandardCharsets
import com.mr.speed.test.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

class DesktopAssetReader : AssetReader {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun readFile(name: String): String {
        val cleanName = name.removePrefix("/")
        
        // 1. Direct ClassLoader Search (Instant for Desktop resources & JAR)
        val loaders = listOf(
            Thread.currentThread().contextClassLoader,
            this::class.java.classLoader,
            ClassLoader.getSystemClassLoader()
        )
        val loaderPaths = listOf(
            cleanName,
            "files/$cleanName",
            "composeResources/com.mr.speed.test/files/$cleanName"
        )
        for (loader in loaders) {
            for (p in loaderPaths) {
                try {
                    val stream = loader?.getResourceAsStream(p) ?: loader?.getResourceAsStream("/$p")
                    if (stream != null) {
                        val text = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                        if (text.isNotEmpty()) return text
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Relative & Absolute File Search (Local IDE Development)
        val devSearchPaths = listOf(
            cleanName,
            "files/$cleanName",
            "app/src/desktopMain/resources/$cleanName",
            "app/src/commonMain/composeResources/files/$cleanName",
            "app/src/androidMain/assets/$cleanName",
            "src/commonMain/composeResources/files/$cleanName",
            "E:/My-Speed-Test-App/app/src/desktopMain/resources/$cleanName",
            "E:/My-Speed-Test-App/app/src/commonMain/composeResources/files/$cleanName"
        )
        for (path in devSearchPaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val text = file.readText(StandardCharsets.UTF_8)
                    if (text.isNotEmpty()) return text
                }
            } catch (_: Exception) {}
        }

        // 3. Compose Resources API Fallback
        try {
            val bytes = Res.readBytes("files/$cleanName")
            if (bytes.isNotEmpty()) {
                return String(bytes, StandardCharsets.UTF_8)
            }
        } catch (_: Exception) {}

        println("[WARN] DesktopAssetReader: Could not locate asset '$cleanName'")
        return ""
    }
}
