package com.fdossena.speedtest.data

interface SettingsProvider {
    var dlParallelStreams: Int
    var ulParallelStreams: Int
    var pingCount: Int
    var useMebibits: Boolean
    var themeMode: Int // 0: System, 1: Light, 2: Dark
    var showLastResults: Boolean
    var appLanguage: String // "en", "es", "fr", "it", etc.
    var isFirstRun: Boolean
    var autoNotifyNetwork: Boolean
    var networkNotifyMode: Int // 0: Only New, 1: Reconnect after delay
    var lastDisconnectTime: Long
    var autoDailyTestEnabled: Boolean
    var autoDailyTestTime: String // Format "HH:mm"
    var autoDailyTestWifiOnly: Boolean
    var useHaptics: Boolean
    var neonPrimaryColor: String // Hex string
    var neonSecondaryColor: String // Hex string
    var detectorShowDownFirst: Boolean
    var suiteAutoRefresh: Boolean
    var isSuiteEnabled: Boolean
    var linkedUid: String?
    var linkedEmail: String?
    var linkedRole: String?
    var refreshToken: String?
    var anonymousUid: String?
    var deviceId: String
    var lastSyncTimestamp: Long
    var trayIconMode: Int // 0: Download Speed, 1: Upload Speed, 2: Ping, 3: App Icon
    var autoStartOnBoot: Boolean
    var isMultiConnectionMode: Boolean
    
    var accessibilityFontScale: Float
    var highContrastModeEnabled: Boolean
    var simplifiedUiModeEnabled: Boolean
    
    fun hasKey(key: String): Boolean
    fun updateWidgetData(dl: String, ul: String, ping: String, health: Int)
}
