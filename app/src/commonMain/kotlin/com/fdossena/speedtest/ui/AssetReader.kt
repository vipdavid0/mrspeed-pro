package com.fdossena.speedtest.ui

interface AssetReader {
    suspend fun readFile(name: String): String
}
