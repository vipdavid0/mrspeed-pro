package com.fdossena.speedtest.ui

import com.fdossena.speedtest.utils.Translations
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageTranslationTest {

    private val allLanguageCodes = listOf(
        "en", "es", "fr", "de", "it", "pt", "ru", "hi", "ar", "zh", "zh-tw", "ja", "ko",
        "nl", "tr", "pl", "uk", "sv", "no", "fi", "da", "cs", "el", "ro", "hu", "th",
        "vi", "id", "ms", "bn", "fa", "he", "ur", "sw", "fil", "af", "sq", "am", "hy",
        "az", "eu", "be", "bs", "bg", "ca", "hr", "et", "gl", "ka", "gu", "ha", "is",
        "ig", "ga", "jv", "kn", "kk", "km", "lo", "lv", "lt", "mk", "mg", "ml", "mr",
        "mn", "ne", "ps", "pa", "sr", "si", "sk", "sl", "so", "su", "ta", "te", "uz",
        "cy", "xh", "yi", "yo", "zu"
    )

    private val testStringKeys = listOf(
        "setup_title", "setup_subtitle", "setup_choose_language", "setup_choose_theme",
        "setup_continue", "start", "test_dl", "test_ul", "history_title", "settings_title",
        "settings_preferences", "notif_title", "settings_haptic", "settings_pro_suite",
        "settings_use_mebibits", "settings_show_last_results", "settings_theme_visuals",
        "settings_theme_mode", "settings_primary_neon", "settings_scheduled_tasks",
        "auto_daily_test", "settings_wifi_only", "settings_suite_auto_refresh",
        "settings_engine_config", "auth_title", "server_select_title", "feature_detector",
        "feature_gaming", "feature_tower", "feature_heatmap", "feature_duel",
        "feature_ranking", "feature_shield", "feature_scanqr", "theme_system",
        "theme_light", "theme_dark", "common_home", "common_retest", "MANUAL SIGN IN",
        "SAVE", "CANCEL"
    )

    @Test
    fun testAll83LanguagesHaveValidTranslations() {
        var testedCount = 0
        allLanguageCodes.forEach { lang ->
            testStringKeys.forEach { key ->
                val result = Translations.get(key, lang)
                assertNotNull("Translation for key '$key' in language '$lang' should not be null", result)
                assertTrue("Translation for key '$key' in language '$lang' should not be empty", result!!.isNotBlank())
                testedCount++
            }
        }
        org.junit.Assert.assertEquals(allLanguageCodes.size * testStringKeys.size, testedCount)
    }
}
