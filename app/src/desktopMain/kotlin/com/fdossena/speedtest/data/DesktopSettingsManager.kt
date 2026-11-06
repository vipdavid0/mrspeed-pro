package com.fdossena.speedtest.data

import java.util.UUID

class DesktopSettingsManager : SettingsProvider {

    init {
        try {
            // Permanently purge legacy Java Preferences from Registry/OS to disconnect dev environment settings
            val legacyNode = java.util.prefs.Preferences.userRoot().node("com.fdossena.speedtest")
            legacyNode.removeNode()
            java.util.prefs.Preferences.userRoot().flush()
        } catch (_: Exception) {}
    }

    private fun getString(key: String, default: String = ""): String {
        val props = EncryptedLocalStore.loadProps()
        val enc = props.getProperty(key, "")
        if (enc.isEmpty()) return default
        val dec = EncryptedLocalStore.decrypt(enc)
        return if (dec.isNotEmpty()) dec else default
    }

    private fun putString(key: String, value: String?) {
        val props = EncryptedLocalStore.loadProps()
        if (value == null) {
            props.remove(key)
        } else {
            val enc = EncryptedLocalStore.encrypt(value)
            props.setProperty(key, enc)
        }
        EncryptedLocalStore.saveProps(props)
    }

    private fun getInt(key: String, default: Int): Int {
        val str = getString(key, "")
        return str.toIntOrNull() ?: default
    }

    private fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    private fun getBoolean(key: String, default: Boolean): Boolean {
        val str = getString(key, "")
        return if (str.isNotEmpty()) str.toBoolean() else default
    }

    private fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    private fun getLong(key: String, default: Long): Long {
        val str = getString(key, "")
        return str.toLongOrNull() ?: default
    }

    private fun putLong(key: String, value: Long) {
        putString(key, value.toString())
    }

    private fun getFloat(key: String, default: Float): Float {
        val str = getString(key, "")
        return str.toFloatOrNull() ?: default
    }

    private fun putFloat(key: String, value: Float) {
        putString(key, value.toString())
    }

    override var dlParallelStreams: Int
        get() = getInt("dl_parallel_streams", 3)
        set(value) = putInt("dl_parallel_streams", value)

    override var ulParallelStreams: Int
        get() = getInt("ul_parallel_streams", 3)
        set(value) = putInt("ul_parallel_streams", value)

    override var pingCount: Int
        get() = getInt("ping_count", 10)
        set(value) = putInt("ping_count", value)

    override var useMebibits: Boolean
        get() = getBoolean("use_mebibits", false)
        set(value) = putBoolean("use_mebibits", value)

    override var themeMode: Int
        get() = getInt("theme_mode", 2)
        set(value) = putInt("theme_mode", value)

    override var showLastResults: Boolean
        get() = getBoolean("show_last_results", true)
        set(value) = putBoolean("show_last_results", value)

    override var appLanguage: String
        get() = getString("app_language", "en")
        set(value) = putString("app_language", value)

    override var isFirstRun: Boolean
        get() {
            if (!EncryptedLocalStore.getSettingsFile().exists()) return true
            return getBoolean("is_first_run", true)
        }
        set(value) = putBoolean("is_first_run", value)

    override var autoNotifyNetwork: Boolean
        get() = getBoolean("auto_notify_network", true)
        set(value) = putBoolean("auto_notify_network", value)

    override var autoStartOnBoot: Boolean
        get() = getBoolean("auto_start_on_boot", true)
        set(value) = putBoolean("auto_start_on_boot", value)

    override var networkNotifyMode: Int
        get() = getInt("network_notify_mode", 0)
        set(value) = putInt("network_notify_mode", value)

    override var lastDisconnectTime: Long
        get() = getLong("last_disconnect_time", 0L)
        set(value) = putLong("last_disconnect_time", value)

    override var autoDailyTestEnabled: Boolean
        get() = getBoolean("auto_daily_test_enabled", false)
        set(value) = putBoolean("auto_daily_test_enabled", value)

    override var autoDailyTestTime: String
        get() = getString("auto_daily_test_time", "03:00")
        set(value) = putString("auto_daily_test_time", value)

    override var autoDailyTestWifiOnly: Boolean
        get() = getBoolean("auto_daily_test_wifi_only", true)
        set(value) = putBoolean("auto_daily_test_wifi_only", value)

    override var useHaptics: Boolean
        get() = getBoolean("use_haptics", false)
        set(value) = putBoolean("use_haptics", value)

    override var neonPrimaryColor: String
        get() = getString("neon_primary_color", "#00E5FF")
        set(value) = putString("neon_primary_color", value)

    override var neonSecondaryColor: String
        get() = getString("neon_secondary_color", "#5E5CE6")
        set(value) = putString("neon_secondary_color", value)

    override var detectorShowDownFirst: Boolean
        get() = getBoolean("detector_show_down_first", true)
        set(value) = putBoolean("detector_show_down_first", value)

    override var suiteAutoRefresh: Boolean
        get() = getBoolean("suite_auto_refresh", false)
        set(value) = putBoolean("suite_auto_refresh", value)

    override var isSuiteEnabled: Boolean
        get() = getBoolean("is_suite_enabled", true)
        set(value) = putBoolean("is_suite_enabled", value)

    override var linkedUid: String?
        get() = getString("linked_uid", "").takeIf { it.isNotEmpty() }
        set(value) = putString("linked_uid", value)

    override var linkedEmail: String?
        get() = getString("linked_email", "").takeIf { it.isNotEmpty() }
        set(value) = putString("linked_email", value)

    override var linkedRole: String?
        get() = getString("linked_role", "user").takeIf { it.isNotEmpty() }
        set(value) = putString("linked_role", value)

    override var refreshToken: String?
        get() = getString("refresh_token", "").takeIf { it.isNotEmpty() }
        set(value) = putString("refresh_token", value)

    override var anonymousUid: String?
        get() = getString("anonymous_uid", "").takeIf { it.isNotEmpty() }
        set(value) = putString("anonymous_uid", value)

    override var deviceId: String
        get() {
            var id = getString("device_unique_id", "")
            if (id.isEmpty()) {
                id = UUID.randomUUID().toString()
                putString("device_unique_id", id)
            }
            return id
        }
        set(value) = putString("device_unique_id", value)

    override var lastSyncTimestamp: Long
        get() = getLong("last_sync_timestamp", 0L)
        set(value) = putLong("last_sync_timestamp", value)

    override var trayIconMode: Int
        get() = getInt("tray_icon_mode", 0)
        set(value) = putInt("tray_icon_mode", value)

    override var isMultiConnectionMode: Boolean
        get() = getBoolean("is_multi_connection_mode", true)
        set(value) = putBoolean("is_multi_connection_mode", value)

    override fun hasKey(key: String): Boolean {
        return EncryptedLocalStore.loadProps().containsKey(key)
    }

    override fun updateWidgetData(dl: String, ul: String, ping: String, health: Int) {
        // No widgets on desktop
    }

    override var accessibilityFontScale: Float
        get() = getFloat("accessibility_font_scale", 1.0f)
        set(value) = putFloat("accessibility_font_scale", value)

    override var highContrastModeEnabled: Boolean
        get() = getBoolean("high_contrast_mode_enabled", false)
        set(value) = putBoolean("high_contrast_mode_enabled", value)

    override var simplifiedUiModeEnabled: Boolean
        get() = getBoolean("simplified_ui_mode_enabled", false)
        set(value) = putBoolean("simplified_ui_mode_enabled", value)
}
