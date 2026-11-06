package com.fdossena.speedtest.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

class AndroidResProvider(private val context: Context, private val language: String) : ResProvider {
    private val localizedContext: Context by lazy {
        LocaleHelper.applyLocale(context, language)
    }

    @Composable
    override fun getString(id: String, vararg args: Any): String {
        val key = extractResourceKey(id)
        val defaultEng = Strings.get(key, *args)

        if (!language.lowercase().startsWith("en")) {
            val translated = Translations.get(key, language, *args)
            if (translated.isNotBlank() && !translated.equals(key, ignoreCase = true) && !translated.equals(defaultEng, ignoreCase = true)) {
                return translated
            }
        }

        val res = localizedContext.resources
        val resId = res.getIdentifier(key, "string", context.packageName)

        if (resId != 0) {
            val rawXmlStr = try {
                if (args.isEmpty()) {
                    res.getString(resId)
                } else {
                    res.getString(resId, *args)
                }
            } catch (_: Exception) {
                null
            }

            if (rawXmlStr != null) {
                val xmlStr = rawXmlStr.replace("&amp;", "&")
                val isUntranslated = xmlStr.equals(defaultEng, ignoreCase = true) || 
                                     xmlStr.equals(key, ignoreCase = true) || 
                                     xmlStr.equals(key.uppercase(), ignoreCase = true) ||
                                     xmlStr.equals("Idioma", ignoreCase = true)

                if (!isUntranslated) {
                    return xmlStr
                }
            }
        }

        val translated = Translations.get(key, language, *args)
        if (translated.isNotBlank() && !translated.equals(key, ignoreCase = true)) {
            return translated
        }

        return defaultEng
    }
}

@Composable
actual fun getResProvider(language: String): ResProvider = AndroidResProvider(LocalContext.current, language)
