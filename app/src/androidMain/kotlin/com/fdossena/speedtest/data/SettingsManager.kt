package com.fdossena.speedtest.data

import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.UUID

class SettingsManager(private val context: Context) : SettingsProvider {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override var neonPrimaryColor: String
        get() = prefs.getString("neon_primary_color", "#00E5FF") ?: "#00E5FF"
        set(value) = prefs.edit().putString("neon_primary_color", value).apply()

    override var neonSecondaryColor: String
        get() = prefs.getString("neon_secondary_color", "#5E5CE6") ?: "#5E5CE6"
        set(value) = prefs.edit().putString("neon_secondary_color", value).apply()

    override var detectorShowDownFirst: Boolean
        get() = prefs.getBoolean("detector_show_down_first", true)
        set(value) = prefs.edit().putBoolean("detector_show_down_first", value).apply()

    override var suiteAutoRefresh: Boolean
        get() = prefs.getBoolean("suite_auto_refresh", false)
        set(value) = prefs.edit().putBoolean("suite_auto_refresh", value).apply()

    override var isSuiteEnabled: Boolean
        get() = prefs.getBoolean("is_suite_enabled", true)
        set(value) = prefs.edit().putBoolean("is_suite_enabled", value).apply()

    override var linkedUid: String?
        get() = prefs.getString("linked_uid", null)
        set(value) = prefs.edit().putString("linked_uid", value).apply()

    override var linkedEmail: String?
        get() = prefs.getString("linked_email", null)
        set(value) = prefs.edit().putString("linked_email", value).apply()

    override var linkedRole: String?
        get() = prefs.getString("linked_role", "user")
        set(value) = prefs.edit().putString("linked_role", value).apply()

    override var autoStartOnBoot: Boolean
        get() = prefs.getBoolean("auto_start_on_boot", false)
        set(value) = prefs.edit().putBoolean("auto_start_on_boot", value).apply()

    override var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit().putString("refresh_token", value).apply()

    override var anonymousUid: String?
        get() = prefs.getString("anonymous_uid", null)
        set(value) = prefs.edit().putString("anonymous_uid", value).apply()

    override var deviceId: String
        get() {
            var id = prefs.getString("device_unique_id", null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("device_unique_id", id).apply()
            }
            return id
        }
        set(value) = prefs.edit().putString("device_unique_id", value).apply()

    override var lastSyncTimestamp: Long
        get() = prefs.getLong("last_sync_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_sync_timestamp", value).apply()

    override var useHaptics: Boolean
        get() = prefs.getBoolean("use_haptics", true)
        set(value) = prefs.edit().putBoolean("use_haptics", value).apply()

    override fun hasKey(key: String): Boolean {
        return prefs.contains(key)
    }

    override fun updateWidgetData(dl: String, ul: String, ping: String, health: Int) {
        prefs.edit()
            .putString("last_widget_dl", dl)
            .putString("last_widget_ul", ul)
            .putString("last_widget_ping", ping)
            .putInt("last_widget_health", health)
            .apply()
    }

    override var autoDailyTestWifiOnly: Boolean
        get() = prefs.getBoolean("auto_daily_test_wifi_only", true)
        set(value) = prefs.edit().putBoolean("auto_daily_test_wifi_only", value).apply()

    override var autoDailyTestEnabled: Boolean
        get() = prefs.getBoolean("auto_daily_test_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_daily_test_enabled", value).apply()

    override var autoDailyTestTime: String
        get() = prefs.getString("auto_daily_test_time", "03:00") ?: "03:00"
        set(value) = prefs.edit().putString("auto_daily_test_time", value).apply()

    override var autoNotifyNetwork: Boolean
        get() = prefs.getBoolean("auto_notify_network", true)
        set(value) = prefs.edit().putBoolean("auto_notify_network", value).apply()

    override var networkNotifyMode: Int
        get() = prefs.getInt("network_notify_mode", 0)
        set(value) = prefs.edit().putInt("network_notify_mode", value).apply()

    override var lastDisconnectTime: Long
        get() = prefs.getLong("last_disconnect_time", 0)
        set(value) = prefs.edit().putLong("last_disconnect_time", value).apply()

    override var isFirstRun: Boolean
        get() = prefs.getBoolean("is_first_run", true)
        set(value) = prefs.edit().putBoolean("is_first_run", value).apply()

    override var appLanguage: String
        get() = prefs.getString("app_language", "en") ?: "en"
        set(value) = prefs.edit().putString("app_language", value).apply()

    override var showLastResults: Boolean
        get() = prefs.getBoolean("show_last_results", true)
        set(value) = prefs.edit().putBoolean("show_last_results", value).apply()

    override var dlParallelStreams: Int
        get() = prefs.getInt("dl_parallel_streams", 3)
        set(value) = prefs.edit().putInt("dl_parallel_streams", value).apply()

    override var ulParallelStreams: Int
        get() = prefs.getInt("ul_parallel_streams", 3)
        set(value) = prefs.edit().putInt("ul_parallel_streams", value).apply()

    override var pingCount: Int
        get() = prefs.getInt("ping_count", 10)
        set(value) = prefs.edit().putInt("ping_count", value).apply()

    override var useMebibits: Boolean
        get() = prefs.getBoolean("use_mebibits", false)
        set(value) = prefs.edit().putBoolean("use_mebibits", value).apply()

    override var themeMode: Int
        get() {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
            val defaultTheme = if (isTv) 1 else 2 // Light for TV, Dark for others
            return prefs.getInt("theme_mode", defaultTheme)
        }
        set(value) { prefs.edit().putInt("theme_mode", value).commit() }

    override var trayIconMode: Int
        get() = prefs.getInt("tray_icon_mode", 0)
        set(value) = prefs.edit().putInt("tray_icon_mode", value).apply()

    override var isMultiConnectionMode: Boolean
        get() = prefs.getBoolean("is_multi_connection_mode", true)
        set(value) = prefs.edit().putBoolean("is_multi_connection_mode", value).apply()



    override var accessibilityFontScale: Float
        get() = prefs.getFloat("accessibility_font_scale", 1.0f)
        set(value) = prefs.edit().putFloat("accessibility_font_scale", value).apply()

    override var highContrastModeEnabled: Boolean
        get() = prefs.getBoolean("high_contrast_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("high_contrast_mode_enabled", value).apply()

    override var simplifiedUiModeEnabled: Boolean
        get() = prefs.getBoolean("simplified_ui_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("simplified_ui_mode_enabled", value).apply()

}
