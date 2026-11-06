package com.fdossena.speedtest.ui

import com.fdossena.speedtest.data.HistoryRepository
import com.fdossena.speedtest.data.SettingsProvider
import com.fdossena.speedtest.data.TestResult
import com.fdossena.speedtest.utils.Translations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeHistoryRepository : HistoryRepository {
    override val allResults: Flow<List<TestResult>> = flowOf(emptyList())
    override suspend fun insert(testResult: TestResult) {}
    override suspend fun delete(testResult: TestResult) {}
    override suspend fun deleteAll() {}
    override suspend fun exists(timestamp: Long): Boolean = false
    override suspend fun getLastResult(): TestResult? = null
}

class FakeSettingsProvider : SettingsProvider {
    override var dlParallelStreams: Int = 3
    override var ulParallelStreams: Int = 3
    override var pingCount: Int = 10
    override var useMebibits: Boolean = false
    override var themeMode: Int = 0
    override var showLastResults: Boolean = true
    override var appLanguage: String = "en"
    override var isFirstRun: Boolean = false
    override var autoNotifyNetwork: Boolean = true
    override var networkNotifyMode: Int = 0
    override var lastDisconnectTime: Long = 0L
    override var autoDailyTestEnabled: Boolean = false
    override var autoDailyTestTime: String = "03:00"
    override var autoDailyTestWifiOnly: Boolean = true
    override var useHaptics: Boolean = true
    override var neonPrimaryColor: String = "#00E5FF"
    override var neonSecondaryColor: String = "#5E5CE6"
    override var detectorShowDownFirst: Boolean = false
    override var suiteAutoRefresh: Boolean = true
    override var isSuiteEnabled: Boolean = true
    override var linkedUid: String? = null
    override var linkedEmail: String? = null
    override var refreshToken: String? = null
    override var anonymousUid: String? = null
    override var deviceId: String = "test-device"
    override var lastSyncTimestamp: Long = 0L
    override var trayIconMode: Int = 0
    override var autoStartOnBoot: Boolean = true
    override var isMultiConnectionMode: Boolean = true
    override var linkedRole: String? = "user"
    override var accessibilityFontScale: Float = 1.0f
    override var highContrastModeEnabled: Boolean = false
    override var simplifiedUiModeEnabled: Boolean = false


    override fun hasKey(key: String): Boolean = true
    override fun updateWidgetData(dl: String, ul: String, ping: String, health: Int) {}
}

class LanguageTranslationUnitTest {

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
                org.junit.Assert.assertNotNull("Translation for key '$key' in language '$lang' should not be null", result)
                org.junit.Assert.assertTrue("Translation for key '$key' in language '$lang' should not be empty", result!!.isNotBlank())
                testedCount++
            }
        }
        assertEquals(allLanguageCodes.size * testStringKeys.size, testedCount)
    }
}
