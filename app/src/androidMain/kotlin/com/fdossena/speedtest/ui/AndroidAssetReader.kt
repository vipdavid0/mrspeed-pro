package com.fdossena.speedtest.ui

import com.mr.speed.test.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

class AndroidAssetReader : AssetReader {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun readFile(name: String): String {
        return try {
            Res.readBytes("files/$name").decodeToString()
        } catch (e: Exception) {
            ""
        }
    }
}
