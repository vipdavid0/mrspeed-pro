package com.fdossena.speedtest.ui.models

import com.fdossena.speedtest.core.serverSelector.TestPoint
import com.fdossena.speedtest.data.*

enum class TwoFactorTab {
    PUSH,
    TOTP,
    SMS,
    WHATSAPP,
    TELEGRAM,
    EMAIL,
    RECOVERY
}

data class SpeedtestUIState(
    val screen: Screen = Screen.Splash,
    val testRunning: Boolean = false,
    val testFinished: Boolean = false,
    val dlSpeed: Double = 0.0,
    val ulSpeed: Double = 0.0,
    val ping: Double = 0.0,
    val jitter: Double = 0.0,
    val dlProgress: Double = 0.0,
    val ulProgress: Double = 0.0,
    val pingProgress: Double = 0.0,
    val ipInfo: String = "Detecting...",
    val availableServers: List<TestPoint> = emptyList(),
    val selectedServer: TestPoint? = null,
    val secondaryServer: TestPoint? = null,
    val bestServer: TestPoint? = null,
    val isMultiConnectionMode: Boolean = true,
    val themeMode: Int = 0,
    val appLanguage: String = "en",
    val userEmail: String? = null,
    val userPhone: String? = null,
    val userRole: String = "user",
    val userPlan: String = "free",
    val isProUser: Boolean = false,
    val isAdminUser: Boolean = false,
    val isUserAnonymous: Boolean = true,
    val authError: String? = null,
    val authSuccessMessage: String? = null,
    val authLoading: Boolean = false,
    val testErrorRes: String? = null,
    val showQRScanner: Boolean = false,
    val show2FaDialog: Boolean = false,
    val pending2FaReqId: String = "",
    val pending2FaUid: String = "",
    val pending2FaIp: String = "",
    val selected2FaTab: TwoFactorTab = TwoFactorTab.PUSH,
    val twoFactorInputCode: String = "",
    val twoFactorError: String? = null,
    val twoFactorSuccessMsg: String? = null,
    val twoFactorLoading: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val loginQRData: String? = null,
    val qrExpiresIn: Int = 0,
    val initTextRes: String? = null,
    val failTextRes: String? = null,
    val failText: String = "",
    val showDl: Boolean = true,
    val showUl: Boolean = true,
    val showPing: Boolean = true,
    val showIp: Boolean = true,
    val showPrivacy: Boolean = true,
    val shareUrl: String? = null,
    val isDlActive: Boolean = false,
    val isUlActive: Boolean = false,
    val isPingActive: Boolean = false,
    val dlDataUsed: Long = 0,
    val ulDataUsed: Long = 0,
    val showLastResults: Boolean = true,
    val showConnectivityWarning: Boolean = false,
    val autoNotifyNetwork: Boolean = true,
    val autoStartOnBoot: Boolean = true,
    val networkNotifyMode: Int = 0, 
    val autoDailyTestEnabled: Boolean = false,
    val autoDailyTestTime: String = "03:00",
    val autoDailyTestWifiOnly: Boolean = true,
    val useHaptics: Boolean = true,
    val useMebibits: Boolean = false,
    val dlParallelStreams: Int = 3,
    val ulParallelStreams: Int = 3,
    val pingCount: Int = 10,
    val neonPrimaryColor: String = "#00E5FF",
    val neonSecondaryColor: String = "#5E5CE6",
    val isIgnoringBatteryOptimizations: Boolean = false,
    val previousScreen: Screen? = null,
    val isSetupLoading: Boolean = false,
    val completingSetupAfterPermission: Boolean = false,
    val dlHistory: List<Double> = emptyList(),
    val ulHistory: List<Double> = emptyList(),
    val serviceSearchQuery: String = "",
    val selectedService: ServiceStatus? = null,
    val showDownFirst: Boolean = true,
    val showNotificationRationale: Boolean = false,
    val disabledServers: Set<String> = emptySet(),
    val showServerFailAlert: Boolean = false,
    val connectionHealth: ConnectionHealth = ConnectionHealth(),
    val dlLoadedPing: Double = 0.0,
    val ulLoadedPing: Double = 0.0,
    val serverStats: Map<String, ServerStat> = emptyMap(),
    val isTvMode: Boolean = false,
    val suiteAutoRefresh: Boolean = false,
    val isSuiteEnabled: Boolean = true,
    val bufferbloatGrade: String = "N/A",
    val wifiSignalStrength: Int = 0,
    val duelWifiResult: TestResult? = null,
    val duelMobileResult: TestResult? = null,
    val duelPhase: DuelPhase = DuelPhase.Idle,
    val platformComparison: Map<String, TestResult?> = emptyMap(),
    val userDevices: List<UserDevice> = emptyList(),
    val cpuUsage: Int = 0,
    val pendingAdminChatSessionId: String? = null,
    val ramUsage: Int = 0,
    val hardwareWarning: String? = null,
    val isVpnActive: Boolean = false,
    val dnsLeakStatus: String = "CHECKING...",
    val isSyncing: Boolean = false,
    val accessibilityFontScale: Float = 1.0f,
    val highContrastModeEnabled: Boolean = false,
    val simplifiedUiModeEnabled: Boolean = false,

    val isRemoteTest: Boolean = false,
    val trayIconMode: Int = 0,
    val showLocationRationale: Boolean = false,
    val isAuditLoading: Boolean = false,
    val auditResult: WebVitalsResult? = null,
    val realIp: String? = null,
    val realIsp: String? = null,
    val realLocation: String? = null
)

data class UserDevice(
    val id: String = "",
    val name: String = "",
    val type: String = "Mobile App",
    val lastActive: String = "Just now"
)



data class WebVitalsResult(
    val domain: String = "",
    val grade: String = "A+",
    val color: String = "#00FF64",
    val ttfbMs: Double = 0.0,
    val dnsTimeMs: Double = 0.0,
    val lcpEstMs: Double = 0.0,
    val cdn: String = "Direct Server",
    val recommendations: List<String> = emptyList()
)

enum class Screen {
    Splash, Setup, Init, Ready, Test, History, Settings, Privacy, Login, SignUp, ForgotPassword, Profile, ServerSelect, Heatmap, CornerMapping, StatusDetail, MrDetector, GamingDashboard, ISPLeaderboard, PrivacyDashboard, Fail, StreamingDashboard, SignalFinder, Duel, Suite, DNSChecker, WhoisLookup, PortScanner, Traceroute, AnomalyHeatmap, ARSignalPainter, CellTowerMapper, AmbientMonitor, Register, Account, WifiAnalyzer, OutageTracker, ProcessInspector, RemoteController, HistoryExporter, WebVitalsAuditor, AdminLiveChat, LiveSupportChat
}

enum class DuelPhase {
    Idle, TestingWifi, WaitingForMobile, TestingMobile, Finished
}

data class ConnectionHealth(
    val gamingScore: Int = 0,
    val streamingScore: Int = 0,
    val callScore: Int = 0
)
