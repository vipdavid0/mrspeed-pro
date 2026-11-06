package com.fdossena.speedtest.utils

import androidx.compose.runtime.Composable

fun extractResourceKey(rawId: String): String {
    var key = rawId.trim()
    if (key.startsWith("StringResource(")) {
        key = key.substringAfter("StringResource(").substringBefore(")")
    }
    if (key.contains("key=")) {
        key = key.substringAfter("key=").substringBefore(",").substringBefore(")")
    } else if (key.contains("id=string:")) {
        key = key.substringAfter("id=string:").substringBefore(",").substringBefore(")")
    } else if (key.contains("string:")) {
        key = key.substringAfter("string:").substringBefore(",").substringBefore(")")
    } else if (key.contains(".")) {
        key = key.substringAfterLast(".")
    }
    return key.trim()
}

interface ResProvider {
    @Composable
    fun getString(id: String, vararg args: Any): String
}

@Composable
expect fun getResProvider(language: String = "en"): ResProvider
