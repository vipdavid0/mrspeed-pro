package com.fdossena.speedtest.utils

import androidx.compose.runtime.Composable

class DesktopResProvider(private val language: String) : ResProvider {
    @Composable
    override fun getString(id: String, vararg args: Any): String {
        val key = extractResourceKey(id)
        val translated = Translations.get(key, language, *args)
        if (translated.isNotBlank()) return translated
        return Strings.get(key, *args)
    }
}

@Composable
actual fun getResProvider(language: String): ResProvider = DesktopResProvider(language)
