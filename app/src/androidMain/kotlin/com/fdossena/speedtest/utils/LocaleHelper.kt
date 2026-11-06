package com.fdossena.speedtest.utils

import android.content.Context
import android.content.res.Configuration
import java.util.*

object LocaleHelper {
    fun applyLocale(context: Context, language: String): Context {
        val locale = when {
            language.contains("-r") -> {
                val parts = language.split("-r")
                Locale(parts[0], parts[1])
            }
            language.contains("-") -> {
                val parts = language.split("-")
                Locale(parts[0], parts[1].uppercase())
            }
            else -> Locale(language)
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        
        return context.createConfigurationContext(config)
    }
}
