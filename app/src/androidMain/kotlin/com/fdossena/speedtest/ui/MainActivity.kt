package com.fdossena.speedtest.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.os.Build
import android.Manifest
import android.app.UiModeManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fdossena.speedtest.SpeedtestApplication
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fdossena.speedtest.data.*
import com.fdossena.speedtest.ui.models.*
import com.fdossena.speedtest.utils.*
import com.fdossena.speedtest.utils.AndroidPlatformService
import com.mr.speed.test.R
import androidx.compose.runtime.*
import com.fdossena.speedtest.core.serverSelector.TestPoint
import kotlinx.serialization.json.Json

open class MainActivity : ComponentActivity() {

    private var intentState by mutableStateOf<Intent?>(null)

    override fun attachBaseContext(newBase: Context) {
        val settingsManager = SettingsManager(newBase)
        val lang = settingsManager.appLanguage
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, lang))
    }

    companion object {
        const val EXTRA_START_TEST = "EXTRA_START_TEST"
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var suiteViewModel: SuiteViewModel

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.handleNotificationPermissionGranted()
        } else {
            viewModel.handleNotificationPermissionDenied()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.startQRScanner()
        } else {
            viewModel.updateState { it.copy(authError = "Camera permission is required to scan QR codes") }
        }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return super.dispatchGenericMotionEvent(ev)
        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_HOVER_MOVE ||
            action == MotionEvent.ACTION_HOVER_ENTER ||
            action == MotionEvent.ACTION_HOVER_EXIT) {
            return true
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent received: ${intent.extras}")
        setIntent(intent)
        intentState = intent

        val reqId = intent.getStringExtra("2fa_req_id")
        if (!reqId.isNullOrEmpty()) {
            viewModel.handle2FaNotificationIntent(reqId)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            viewModel.updateState { it.copy(showLocationRationale = false) }
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fine || coarse) {
                viewModel.navigateTo(Screen.CellTowerMapper)
            } else {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (_: Exception) {}
                viewModel.navigateTo(Screen.CellTowerMapper)
            }
        }
    }

    private fun requestBatteryAndNotificationPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = getSystemService(POWER_SERVICE) as? PowerManager
                if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        requestBatteryAndNotificationPermissions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        Log.d("MainActivity", "onCreate called with intent: ${intent.extras}")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        intentState = intent
        
        setContent {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = HistoryRepositoryImpl(database.testResultDao())
            val settingsManager = SettingsManager(applicationContext)
            val syncManager = AndroidSyncManager(settingsManager)
            val serviceStatusRepository = WebServiceStatusRepository(syncManager)
            val platformService = remember { AndroidPlatformService(applicationContext, this@MainActivity) }
            platformService.currentActivity = this@MainActivity
            val assetReader = AndroidAssetReader()

            val factory = MainViewModelFactory(
                repository, settingsManager, assetReader, 
                syncManager, serviceStatusRepository, platformService
            )
            
            val suiteRepository = AndroidSuiteRepository(database.trackedDomainDao())
            val suiteFactory = SuiteViewModelFactory(suiteRepository, settingsManager)
            suiteViewModel = viewModel(factory = suiteFactory)
            
            viewModel = viewModel(factory = factory)
            val state by viewModel.uiState.collectAsState()
            val suiteState by suiteViewModel.uiState.collectAsState()
            val historyResults by viewModel.history.collectAsState(initial = emptyList())
            val serviceStatuses by viewModel.serviceStatuses.collectAsState(initial = emptyList())
            val gamingScores by viewModel.gamingScores.collectAsState(initial = emptyList())
            val ispLeaderboard by viewModel.ispLeaderboard.collectAsState(initial = emptyList())

            var appliedLanguage by remember { mutableStateOf(state.appLanguage) }
            val currentLanguage = state.appLanguage
            LaunchedEffect(currentLanguage) {
                if (currentLanguage != appliedLanguage) {
                    appliedLanguage = currentLanguage
                    LocaleHelper.applyLocale(this@MainActivity, currentLanguage)
                    recreate()
                }
            }

            LaunchedEffect(Unit) {
                syncManager.startBackgroundServices {
                    val current = viewModel.uiState.value
                    val srv = current.selectedServer ?: current.bestServer ?: current.availableServers.firstOrNull()
                    if (srv != null) {
                        viewModel.startTest(srv, "testFail_err", isRemote = true)
                    }
                }
            }

            LaunchedEffect(intentState) {
                val intent = intentState
                val langExtra = intent?.getStringExtra("EXTRA_LANG")
                if (!langExtra.isNullOrEmpty()) {
                    viewModel.updateSettings(lang = langExtra)
                }

                val reqId = intent?.getStringExtra("2fa_req_id")
                val ip = intent?.getStringExtra("2fa_ip") ?: ""
                if (!reqId.isNullOrEmpty()) {
                    viewModel.handle2FaNotificationIntent(reqId, ip)
                }

                val smsCode = intent?.getStringExtra("2fa_sms_code")
                if (!smsCode.isNullOrEmpty()) {
                    try {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("2FA Code", smsCode)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(this@MainActivity, "📱 2FA Code $smsCode copied to clipboard!", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {}
                }

                val startImmediately = intent?.getBooleanExtra("EXTRA_START_TEST", false) ?: false
                val remoteSource = intent?.getStringExtra("EXTRA_REMOTE_SOURCE") ?: "Unknown"
                
                val className = this@MainActivity.javaClass.simpleName
                val isDetectorLaunch = className.contains("Detector") || 
                                       intent?.getBooleanExtra("EXTRA_LAUNCH_DETECTOR", false) ?: false
                
                val uiModeManager = getSystemService(UI_MODE_SERVICE) as? UiModeManager
                val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
                
                val isSuiteLaunch = (className.contains("Suite") || 
                                    intent?.getBooleanExtra("EXTRA_LAUNCH_SUITE", false) ?: false) && !isTv && settingsManager.isSuiteEnabled
                
                Log.d("MainActivity", "LaunchedEffect(intentState): startImmediately=$startImmediately, from=$remoteSource, isDetector=$isDetectorLaunch")
                
                val deepLinkUri = intent?.data?.toString()
                val navigateToLeaderboard = deepLinkUri == "mrspeed://leaderboard"

                val stayOnCurrent = state.screen != Screen.Splash || isDetectorLaunch || isSuiteLaunch

                // Always try to refresh IP info when app starts or intent is received
                viewModel.initApp(
                    loadingTextRes = "init_init",
                    selectingTextRes = "init_selecting",
                    configErrorTextRes = "initFail_configError",
                    noServersTextRes = "initFail_noServers",
                    noInternetTextRes = "initFail_noInternet",
                    startTestImmediately = startImmediately,
                    testFailTextRes = "testFail_err",
                    stayOnCurrentScreen = stayOnCurrent
                )

                (applicationContext as? SpeedtestApplication)?.scheduleMrDetector()
                (applicationContext as? SpeedtestApplication)?.updateMonitoringService()

                if (state.screen == Screen.Splash) {
                    if (isDetectorLaunch) {
                        viewModel.updateState { it.copy(screen = Screen.MrDetector) }
                    } else if (isSuiteLaunch) {
                        viewModel.updateState { it.copy(screen = Screen.Suite) }
                    } else if (navigateToLeaderboard) {
                        viewModel.navigateTo(Screen.ISPLeaderboard)
                    }
                }
            }

            val requestLocationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                viewModel.updateState { it.copy(showLocationRationale = false) }
                if (fine || coarse) {
                    viewModel.navigateTo(Screen.CellTowerMapper)
                }
            }

            SpeedtestApp(
                state = state,
                platformService = platformService,
                onStartTest = { viewModel.startTest(it, "testFail_err") },
                onRetry = { 
                    viewModel.initApp(
                        "init_init",
                        "init_selecting",
                        "initFail_configError",
                        "initFail_noServers",
                        "initFail_noInternet"
                    )
                },
                onOpenPrivacy = { viewModel.navigateTo(Screen.Privacy) },
                onClosePrivacy = { viewModel.goBack() },
                onRestart = { viewModel.goBack() },
                onShare = { text ->
                    Log.d("MainActivity", "onShare triggered with text: $text")
                    if (text.isNotBlank()) {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Mr Speed Result")
                        startActivity(shareIntent)
                    }
                },
                onShareReportCard = { result ->
                    val shareText = "Mr. Speed Premium Report Card\n" +
                                   "--------------------------\n" +
                                   "ISP: ${result.ipInfo}\n" +
                                   "DOWNLOAD: ${result.downloadSpeed} Mbps\n" +
                                   "UPLOAD: ${result.uploadSpeed} Mbps\n" +
                                   "PING: ${result.ping} ms\n" +
                                   "JITTER: ${result.jitter} ms\n" +
                                   "--------------------------\n" +
                                   "Generated by Mr. Speed"
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(sendIntent, "Share Report Card"))
                },
                onLogoClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mrspeed.ca/"))
                    startActivity(intent)
                },
                onOpenHistory = { viewModel.navigateTo(Screen.History) },
                onCloseHistory = { viewModel.goBack() },
                onGoHome = { viewModel.goBack() },
                onDeleteResult = { viewModel.deleteResult(it) },
                onClearHistory = { viewModel.clearHistory() },
                onOpenSettings = { viewModel.navigateTo(Screen.Settings) },
                onCloseSettings = { viewModel.goBack() },
                onUpdateSettings = { p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19 ->
                    if (p8 == true) {
                        val pm = getSystemService(POWER_SERVICE) as? PowerManager
                        val needsBattery = (pm != null && !pm.isIgnoringBatteryOptimizations(packageName))
                        val needsNotif = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        if (needsBattery || needsNotif) {
                            viewModel.updateState { it.copy(showNotificationRationale = true) }
                        }
                    }
                    viewModel.updateSettings(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19)
                },
                onDismissConnectivityWarning = { viewModel.dismissConnectivityWarning() },
                onCompleteSetup = {
                    Log.d("MainActivity", "onCompleteSetup triggered")
                    viewModel.completeSetup(
                        "init_init",
                        "init_selecting",
                        "initFail_configError",
                        "initFail_noServers",
                        "initFail_noInternet"
                    )
                },
                onOpenAuth = { viewModel.navigateTo(Screen.Login) },
                onNavigateToSignUp = { viewModel.navigateTo(Screen.SignUp) },
                onNavigateToForgot = { viewModel.navigateTo(Screen.ForgotPassword) },
                onNavigateToLogin = { viewModel.navigateTo(Screen.Login) },
                onNavigateToProfile = { viewModel.navigateTo(Screen.Profile) },
                onSignIn = { email, pass, token -> viewModel.signIn(email, pass, token) },
                onSignUp = { email, pass, name, phone, token -> viewModel.signUp(email, pass, name, phone, token) },
                onUpdatePhone = { phone -> viewModel.updatePhone(phone) },
                onTest2FaSms = { viewModel.test2FaSms() },
                onRespond2Fa = { reqId, allow -> viewModel.respond2FaRequest(reqId, allow) },
                onDismiss2Fa = { viewModel.dismiss2FaDialog() },
                onSelect2FaTab = { viewModel.select2FaTab(it) },
                onUpdate2FaCode = { viewModel.update2FaInputCode(it) },
                onSend2FaSms = { viewModel.send2FaSmsCode() },
                onSend2FaEmailOtp = { viewModel.send2FaEmailOtp() },
                onVerify2FaCode = { viewModel.submit2FaVerificationCode() },
                onStartLoginQR = { viewModel.startLoginQRSession() },
                onStopLoginQR = { viewModel.stopLoginQRSession() },
                onSignOut = { viewModel.signOut() },
                onManualSync = { viewModel.performManualSync() },
                onResetPassword = { viewModel.resetPassword(it) },
                onSendVerificationEmail = { viewModel.sendVerificationEmail() },
                onUpdateEmail = { email, pass -> viewModel.updateEmail(email, pass) },
                onUpdatePassword = { pass, newPass -> viewModel.updatePassword(pass, newPass) },
                onDeleteAccount = { viewModel.deleteAccount() },
                onRequestNotificationPermission = {
                    viewModel.dismissNotificationRationale()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.handleNotificationPermissionGranted()
                    }
                    try {
                        val pm = getSystemService(POWER_SERVICE) as? PowerManager
                        if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                        }
                    } catch (_: Exception) {}
                },
                onOpenHeatmap = { viewModel.navigateTo(Screen.Heatmap) },
                onCloseHeatmap = { viewModel.goBack() },
                onOpenCornerMapping = { viewModel.navigateTo(Screen.CornerMapping) },
                onCloseCornerMapping = { viewModel.goBack() },
                onOpenMrDetector = { viewModel.navigateTo(Screen.MrDetector) },
                onCloseMrDetector = { viewModel.goBack() },
                onOpenGaming = { viewModel.navigateTo(Screen.GamingDashboard) },
                onOpenISPLeaderboard = { viewModel.navigateTo(Screen.ISPLeaderboard) },
                onOpenPrivacyDashboard = { viewModel.navigateTo(Screen.PrivacyDashboard) },

                onOpenStreamingDashboard = { viewModel.navigateTo(Screen.StreamingDashboard) },
                onOpenSignalFinder = { 
                    viewModel.updateWifiSignalStrength()
                    viewModel.navigateTo(Screen.SignalFinder) 
                },
                onOpenDuel = { viewModel.navigateTo(Screen.Duel) },
                onOpenARPainter = { viewModel.navigateTo(Screen.ARSignalPainter) },
                onOpenCellTower = {
                    val fine = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                    val coarse = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                        viewModel.navigateTo(Screen.CellTowerMapper)
                    } else {
                        viewModel.updateState { it.copy(showLocationRationale = true) }
                    }
                },
                onRequestLocationPermission = {
                    viewModel.updateState { it.copy(showLocationRationale = false) }
                    val fine = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                    if (fine || coarse) {
                        viewModel.navigateTo(Screen.CellTowerMapper)
                    } else {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ),
                            1001
                        )
                    }
                },
                onDismissLocationRationale = {
                    viewModel.updateState { it.copy(showLocationRationale = false) }
                },
                onOpenAmbientMonitor = { viewModel.navigateTo(Screen.AmbientMonitor) },
                onUpdateFontScale = { viewModel.updateAccessibilityFontScale(it) },
                onUpdateHighContrast = { viewModel.updateHighContrastMode(it) },
                onUpdateSimplifiedUi = { viewModel.updateSimplifiedUiMode(it) },
                onStartDuelWifi = { viewModel.startDuelWifi() },
                onStartDuelMobile = { viewModel.startDuelMobile() },
                onRunLeakTest = { viewModel.runVpnLeakTest() },

                onQRScanTriggered = {
                    Log.d("MainActivity", "QR Scan Triggered")
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.startQRScanner()
                    } else {
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onQRCodeScanned = { viewModel.handleQRCodeScanned(it) },
                onDismissQRScanner = { viewModel.dismissQRScanner() },
                onReportIssue = { viewModel.reportServiceIssue(it) },
                onSelectService = { viewModel.selectService(it) },
                onSearchServices = { viewModel.updateServiceSearch(it) },
                onAddUnlistedService = { name, url -> viewModel.addUnlistedService(name, url) },
                onVerifyService = { viewModel.verifyServiceStatus(it) },
                onToggleDownFirst = { viewModel.toggleDownFirst() },
                onDismissRationale = { viewModel.dismissNotificationRationale() },
                onDismissServerFail = { viewModel.retryTestWithNewServer() },
                onSelectServer = { viewModel.onSelectServer(it) },
                onOpenServerSelect = { viewModel.navigateTo(Screen.ServerSelect) },
                onAddDomain = { suiteViewModel.addDomain(it) },
                onRemoveDomain = { suiteViewModel.removeDomain(it) },
                onRefreshDomains = { suiteViewModel.refreshAll() },
                onRunDNSCheck = { domain, type -> suiteViewModel.runDNSCheck(domain, type) },
                onRunWhois = { query -> suiteViewModel.runWhois(query) },
                onRunPortScan = { target, ports -> suiteViewModel.runPortScan(target, ports) },
                onRunTraceroute = { target -> suiteViewModel.runTraceroute(target) },
                onRunAudit = { domain -> suiteViewModel.runWebVitalsAudit(domain) },
                onFetchDevices = { suiteViewModel.fetchLinkedDevices(syncManager) },
                onTriggerDevice = { devId -> suiteViewModel.triggerRemoteDeviceTest(syncManager, devId) },
                onOpenTool = { tool ->
                    when(tool) {
                        "dns" -> viewModel.navigateTo(Screen.DNSChecker)
                        "whois" -> viewModel.navigateTo(Screen.WhoisLookup)
                        "portscan" -> viewModel.navigateTo(Screen.PortScanner)
                        "trace" -> viewModel.navigateTo(Screen.Traceroute)
                        "heatmap" -> {
                            suiteViewModel.listenToAnomalies()
                            viewModel.navigateTo(Screen.AnomalyHeatmap)
                        }
                        "wifianalyzer" -> viewModel.navigateTo(Screen.WifiAnalyzer)
                        "outagetracker" -> viewModel.navigateTo(Screen.OutageTracker)
                        "processinspector", "process" -> viewModel.navigateTo(Screen.ProcessInspector)
                        "remotecontroller", "remote" -> viewModel.navigateTo(Screen.RemoteController)
                        "exporter", "historyexporter" -> viewModel.navigateTo(Screen.HistoryExporter)
                        "webvitals", "audit" -> viewModel.navigateTo(Screen.WebVitalsAuditor)
                        "AdminLiveChat", "adminlivechat" -> viewModel.navigateTo(Screen.AdminLiveChat)
                        "livesupportchat", "supportchat", "chat" -> viewModel.navigateTo(Screen.LiveSupportChat)
                        "suite" -> viewModel.navigateTo(Screen.Suite)
                    }
                },
                onOpenSuite = { viewModel.navigateTo(Screen.Suite) },
                suiteState = suiteState,
                serviceStatuses = serviceStatuses,
                gamingScores = gamingScores,
                ispLeaderboard = ispLeaderboard,
                floorPlanManager = viewModel.floorPlanManager,
                historyResults = historyResults
            )
        }
    }
}
