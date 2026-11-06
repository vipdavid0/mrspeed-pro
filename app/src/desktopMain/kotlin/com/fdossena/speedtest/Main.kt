package com.fdossena.speedtest

import androidx.compose.ui.window.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.fdossena.speedtest.ui.*
import com.fdossena.speedtest.ui.components.DesktopDual300x50AdRow
import com.fdossena.speedtest.data.*
import com.fdossena.speedtest.ui.models.*
import com.fdossena.speedtest.installer.CustomIObitStyleInstaller
import com.fdossena.speedtest.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject
import org.json.JSONArray
import java.awt.Toolkit
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.ColorPainter
import java.lang.Long
import java.util.concurrent.TimeUnit

fun main() = application {
    val settingsManager = remember { DesktopSettingsManager() }
    val syncManager = remember { WebSyncManager(settingsManager) }
    val serviceStatusRepository = remember { WebServiceStatusRepository(syncManager) }
    val platformService = remember { DesktopPlatformService() }
    val assetReader = remember { DesktopAssetReader() }
    
    val repository = remember { DesktopHistoryRepository(syncManager) }
    val suiteRepository = remember { DesktopSuiteRepository() }

    val viewModel = remember {
        MainViewModel(
            repository, settingsManager, assetReader, 
            syncManager, serviceStatusRepository, platformService
        )
    }

    val suiteViewModel = remember { SuiteViewModel(suiteRepository, settingsManager) }
    
    val trayState = rememberTrayState()
    var isWindowVisible by remember { mutableStateOf(true) }

    // Check launched CLI arguments
    val args = System.getProperty("sun.java.command", "").split(" ")
    LaunchedEffect(Unit) {
        if (args.contains("--silent")) {
            isWindowVisible = false
        }
        if (args.contains("--suite")) {
            viewModel.navigateTo(Screen.Suite)
        }
    }

    val scope = rememberCoroutineScope()

    val state by viewModel.uiState.collectAsState()
    val isLightMode = state.themeMode == 1 || (state.themeMode == 0 && !isSystemInDarkTheme())

    // Automatically create/update shortcuts & icons on Desktop and Start Menu
    LaunchedEffect(isLightMode) {
        withContext(Dispatchers.IO) {
            DesktopShortcutManager.createOrUpdateShortcuts(isLightMode)
        }
    }

    val dynamicTrayPainter = remember(isLightMode) {
        try {
            val img = DesktopTrayManager.getLogoImage(isLightMode)
            BitmapPainter(img.toComposeImageBitmap())
        } catch (_: Exception) {
            ColorPainter(if (isLightMode) Color(0xFFFF9F0A) else Color(0xFF00E5FF))
        }
    }

    LaunchedEffect(Unit) {
        DesktopAutoUpdater.startAutoUpdateChecker(scope)
        DesktopTrayManager.initializeTray(
            onRunQuickTest = {
                val srv = viewModel.uiState.value.selectedServer 
                    ?: viewModel.uiState.value.bestServer 
                    ?: viewModel.uiState.value.availableServers.firstOrNull()
                if (srv != null) {
                    DesktopTrayManager.showToastNotification(
                        "Quick Speed Test Started", 
                        "Running silent speed test on ${srv.name}...",
                        isLightMode = isLightMode
                    )
                    viewModel.startTest(srv, "testFail_err", isRemote = true)
                } else {
                    DesktopTrayManager.showToastNotification(
                        "Quick Speed Test Error", 
                        "No test servers available.",
                        isLightMode = isLightMode
                    )
                }
            },
            onOpenDashboard = {
                isWindowVisible = true
            },
            isLightMode = isLightMode
        )
    }

    LaunchedEffect(state.dlSpeed, state.ulSpeed, state.ping, state.trayIconMode, isLightMode) {
        DesktopTrayManager.updateMetric(state.dlSpeed, state.ulSpeed, state.ping, state.trayIconMode, isLightMode = isLightMode)
    }

    // Background polling for admin incoming chat session requests
    var ignoredSessionId by remember { mutableStateOf("") }
    LaunchedEffect(state.isAdminUser, state.userEmail) {
        if (state.isAdminUser) {
            val adminEmail = state.userEmail ?: ""
            while (isActive) {
                withContext(Dispatchers.IO) {
                    try {
                        val url = URL("https://mrspeed.ca/backend/api/livechat.php?action=admin_get_sessions&status=AGENT_REQUESTED&admin_uid=$adminEmail")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(text)
                        if (json.optBoolean("success")) {
                            val arr = json.optJSONArray("sessions") ?: JSONArray()
                            var validSess: JSONObject? = null
                            for (j in 0 until arr.length()) {
                                val item = arr.getJSONObject(j)
                                val sId = item.optString("session_id", "")
                                if (sId != ignoredSessionId) {
                                    validSess = item
                                    break
                                }
                            }
                            
                            if (validSess != null) {
                                val sessId = validSess.optString("session_id", "")
                                val uName = validSess.optString("user_name", "Visitor")
                                val lastMsg = validSess.optString("last_message", "Wants to chat with an agent")

                                withContext(Dispatchers.Main) {
                                    DesktopToastManager.showToast(
                                        title = "💬 NEW CHAT SESSION REQUEST",
                                        message = "$uName: $lastMsg",
                                        isLightMode = isLightMode,
                                        actionLabel = "JOIN SESSION ➤",
                                        onAction = {
                                            ignoredSessionId = sessId
                                            isWindowVisible = true
                                            viewModel.openAdminChatSession(sessId)
                                            CoroutineScope(Dispatchers.IO).launch {
                                                try {
                                                    val takeoverUrl = URL("https://mrspeed.ca/backend/api/livechat.php")
                                                    val tConn = takeoverUrl.openConnection() as HttpURLConnection
                                                    tConn.requestMethod = "POST"
                                                    tConn.setRequestProperty("Content-Type", "application/json")
                                                    tConn.doOutput = true
                                                    val body = JSONObject().put("action", "admin_takeover").put("session_id", sessId).put("admin_uid", adminEmail)
                                                    tConn.outputStream.use { it.write(body.toString().toByteArray()) }
                                                    tConn.inputStream.bufferedReader().use { it.readText() }
                                                } catch (_: Exception) {}
                                            }
                                        },
                                        onDismiss = {
                                            ignoredSessionId = sessId
                                        },
                                        persistent = true
                                    )
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    if (DesktopToastManager.activeToast.value?.title?.contains("CHAT") == true) {
                                        DesktopToastManager.clearToast()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("[DEBUG] Admin chat poll error: ${e.message}")
                    }
                }
                delay(5000)
            }
        }
    }

    Tray(
        state = trayState,
        icon = dynamicTrayPainter,
        tooltip = "Mr Speed PRO | Desktop",
        menu = {
            Item("Open Mr Speed PRO", onClick = { isWindowVisible = true })
            Item("Run Quick Speed Test (Toast Only)", onClick = {
                val srv = viewModel.uiState.value.selectedServer 
                    ?: viewModel.uiState.value.bestServer 
                    ?: viewModel.uiState.value.availableServers.firstOrNull()
                if (srv != null) {
                    DesktopTrayManager.showToastNotification(
                        "Quick Speed Test Started", 
                        "Running silent speed test on ${srv.name}...",
                        isLightMode = isLightMode
                    )
                    viewModel.startTest(srv, "testFail_err", isRemote = true)
                } else {
                    DesktopTrayManager.showToastNotification(
                        "Quick Speed Test Error", 
                        "No test servers available.",
                        isLightMode = isLightMode
                    )
                }
            })
            Item("Start Local LAN Router Speed Server", onClick = {
                scope.launch {
                    LocalLanSpeedTest.startLanServer()
                }
            })
            Item("Mr Pro Network Suite", onClick = { 
                isWindowVisible = true
                viewModel.navigateTo(Screen.Suite) 
            })
            Item("Exit", onClick = { 
                try {
                    Runtime.getRuntime().exec(arrayOf("powershell", "-ExecutionPolicy", "Bypass", "-Command", "Start-Process taskkill -ArgumentList '/f /im openvpn.exe /t' -Verb RunAs -WindowStyle Hidden"))
                    Runtime.getRuntime().exec(arrayOf("taskkill", "/f", "/im", "openvpn.exe"))
                } catch (_: Exception) {}
                exitApplication() 
            })
        }
    )

    if (isWindowVisible) {
        val windowState = rememberWindowState(width = 1280.dp, height = 900.dp)
        Window(
            onCloseRequest = { 
                try {
                    Runtime.getRuntime().exec(arrayOf("powershell", "-ExecutionPolicy", "Bypass", "-Command", "Start-Process taskkill -ArgumentList '/f /im openvpn.exe /t' -Verb RunAs -WindowStyle Hidden"))
                    Runtime.getRuntime().exec(arrayOf("taskkill", "/f", "/im", "openvpn.exe"))
                } catch (_: Exception) {}
                isWindowVisible = false 
            },
            title = "Mr Speed PRO | Desktop",
            state = windowState,
            undecorated = true
        ) {
            val suiteState by suiteViewModel.uiState.collectAsState()
            val historyResults by repository.allResults.collectAsState(initial = emptyList())
            val serviceStatuses by viewModel.serviceStatuses.collectAsState(initial = emptyList())
            val gamingScores by viewModel.gamingScores.collectAsState()
            val ispLeaderboard by viewModel.ispLeaderboard.collectAsState()
            val defaultPrimary = if (isLightMode) Color(0xFFFF9F0A) else Color(0xFF00E5FF)
            val pColor = if ((state.neonPrimaryColor == "#00E5FF" || state.neonPrimaryColor.isEmpty()) && isLightMode) {
                Color(0xFFFF9F0A)
            } else {
                state.neonPrimaryColor.toColor().takeOrElse { defaultPrimary }
            }

            val transparentCursor = remember {
                try {
                    val img = java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                    java.awt.Toolkit.getDefaultToolkit().createCustomCursor(img, java.awt.Point(0, 0), "transparent_cursor")
                } catch (_: Exception) {
                    null
                }
            }

            DisposableEffect(window) {
                val originalCursor = window.cursor
                if (transparentCursor != null) {
                    fun applyCursorRecursively(comp: java.awt.Component) {
                        try {
                            comp.cursor = transparentCursor
                            if (comp is java.awt.Container) {
                                comp.components?.forEach { applyCursorRecursively(it) }
                            }
                        } catch (_: Exception) {}
                    }
                    applyCursorRecursively(window)
                }
                onDispose {
                    window.cursor = originalCursor
                }
            }

            LaunchedEffect(Unit) {
                viewModel.initApp(
                    "init_init",
                    "init_selecting",
                    "initFail_configError",
                    "initFail_noServers",
                    "initFail_noInternet"
                )
                
                syncManager.startBackgroundServices {
                    val srv = state.selectedServer ?: state.bestServer
                    if (srv != null) {
                        viewModel.startTest(srv, "testFail_err", isRemote = true)
                    }
                }
            }

            LaunchedEffect(state.autoStartOnBoot) {
                withContext(Dispatchers.IO) {
                    try {
                        val os = System.getProperty("os.name")?.lowercase() ?: ""
                        if (os.contains("win")) {
                            if (state.autoStartOnBoot) {
                                val execTarget = DesktopShortcutManager.getAppExecutionTarget()
                                // Only register Windows autostart for compiled standalone .exe instances
                                if (!execTarget.targetPath.endsWith("javaw.exe", ignoreCase = true) && !execTarget.targetPath.endsWith("java.exe", ignoreCase = true)) {
                                    val runCmd = DesktopShortcutManager.getExecutableStartCommand()
                                    val checkCmd = arrayOf("powershell.exe", "-Command", "Set-ItemProperty -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run' -Name 'MrSpeedPRO' -Value '$runCmd'")
                                    val p = Runtime.getRuntime().exec(checkCmd)
                                    p.inputStream.close(); p.errorStream.close(); p.waitFor(1, TimeUnit.SECONDS)
                                }
                            } else {
                                val removeCmd = arrayOf("powershell.exe", "-Command", "Remove-ItemProperty -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run' -Name 'MrSpeedPRO' -ErrorAction SilentlyContinue")
                                val p = Runtime.getRuntime().exec(removeCmd)
                                p.inputStream.close(); p.errorStream.close(); p.waitFor(1, TimeUnit.SECONDS)
                            }
                        }
                    } catch (e: Exception) {
                        println("[DEBUG] Autostart Registry Update Failed: ${e.message}")
                    }
                }
            }

            LaunchedEffect(state.dlSpeed, state.ulSpeed, state.ping, state.trayIconMode) {
                DesktopTrayManager.updateMetric(state.dlSpeed, state.ulSpeed, state.ping, state.trayIconMode)
            }

            WindowDraggableArea {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        CustomOpalCursorOverlay(isLightMode = isLightMode) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    // 1. App UI fills remaining height
                                    SpeedtestApp(
                                        state = state,
                                        suiteState = suiteState,
                                        historyResults = historyResults,
                                        serviceStatuses = serviceStatuses,
                                        gamingScores = gamingScores,
                                        ispLeaderboard = ispLeaderboard,
                                        onStartTest = { viewModel.startTest(it, "testFail_err") },
                                        onGoHome = { viewModel.onGoHome() },
                                        onOpenSettings = { viewModel.navigateTo(Screen.Settings) },
                                        onCloseSettings = { viewModel.goBack() },
                                        onCheckForUpdates = { platformService.showToast("Update Checker", "Checking for updates (v1.2.3 Build 123)...", isLightMode) },
                                        onFetchCaptchaToken = { withContext(Dispatchers.IO) { syncManager.fetchCaptchaToken() } },
                                        onUpdateSettings = { dl, ul, pc, useM, theme, last, lang, autoN, netM, daily, dailyT, wifi, haptics, neonP, neonS, suiteA, suiteE, trayMode, autoStart ->
                                            viewModel.updateSettings(dl, ul, pc, useM, theme, last, lang, autoN, netM, daily, dailyT, wifi, haptics, neonP, neonS, suiteA, suiteE, trayMode, autoStart)
                                        },
                                        onDismissConnectivityWarning = { viewModel.dismissConnectivityWarning() },
                                        onCompleteSetup = {
                                            viewModel.completeSetup(
                                                "init_init",
                                                "init_selecting",
                                                "initFail_configError",
                                                "initFail_noServers",
                                                "initFail_noInternet"
                                            )
                                        },
                                        onRetry = {
                                            viewModel.initApp(
                                                "init_init",
                                                "init_selecting",
                                                "initFail_configError",
                                                "initFail_noServers",
                                                "initFail_noInternet"
                                            )
                                        },
                                        onOpenHistory = { viewModel.navigateTo(Screen.History) },
                                        onCloseHistory = { viewModel.goBack() },
                                        onNavigateToProfile = { viewModel.navigateTo(Screen.Profile) },
                                        onOpenMrDetector = { viewModel.navigateTo(Screen.MrDetector) },
                                        onCloseMrDetector = { viewModel.goBack() },
                                        onSelectServer = { viewModel.onSelectServer(it) },
                                        onOpenServerSelect = { viewModel.navigateTo(Screen.ServerSelect) },
                                        onNavigateToSignUp = { viewModel.navigateTo(Screen.SignUp) },
                                        onNavigateToLogin = { viewModel.navigateTo(Screen.Login) },
                                        onNavigateToForgot = { viewModel.navigateTo(Screen.ForgotPassword) },
                                        onOpenAuth = { viewModel.navigateTo(Screen.Login) },
                                        onStartLoginQR = { viewModel.startLoginQRSession() },
                                        onStopLoginQR = { viewModel.stopLoginQRSession() },
                                        onQRCodeScanned = { viewModel.handleQRCodeScanned(it) },
                                        onDismissQRScanner = { viewModel.dismissQRScanner() },
                                        onOpenHeatmap = { viewModel.navigateTo(Screen.Heatmap) },
                                        onOpenDuel = { viewModel.navigateTo(Screen.Duel) },
                                        onOpenISPLeaderboard = { viewModel.navigateTo(Screen.ISPLeaderboard) },
                                        onOpenPrivacyDashboard = { viewModel.navigateTo(Screen.PrivacyDashboard) },
                                        onOpenGaming = { viewModel.navigateTo(Screen.GamingDashboard) },

                                        onOpenStreamingDashboard = { viewModel.navigateTo(Screen.StreamingDashboard) },
                                        onOpenSignalFinder = { viewModel.navigateTo(Screen.SignalFinder) },
                                        onOpenARPainter = { viewModel.navigateTo(Screen.ARSignalPainter) },
                                        onOpenCellTower = { viewModel.navigateTo(Screen.CellTowerMapper) },
                                        onOpenAmbientMonitor = { viewModel.navigateTo(Screen.AmbientMonitor) },
                                        onUpdateFontScale = { viewModel.updateAccessibilityFontScale(it) },
                                        onUpdateHighContrast = { viewModel.updateHighContrastMode(it) },
                                        onUpdateSimplifiedUi = { viewModel.updateSimplifiedUiMode(it) },
                                        onRunLeakTest = { viewModel.runVpnLeakTest() },
                                        onOpenPrivacy = { viewModel.navigateTo(Screen.Privacy) },
                                        onClosePrivacy = { viewModel.goBack() },
                                        onSignIn = { email, pass, token -> viewModel.signIn(email, pass, token) },
                                        onSignUp = { email, pass, name, phone, token -> viewModel.signUp(email, pass, name, phone, token) },
                                        onUpdatePhone = { phone -> viewModel.updatePhone(phone) },
                                        onTest2FaSms = { viewModel.test2FaSms() },
                                        onRespond2Fa = { reqId, allow -> viewModel.respond2FaRequest(reqId, allow) },
                                        onDismiss2Fa = { viewModel.dismiss2FaDialog() },
                                        onSelect2FaTab = { viewModel.select2FaTab(it) },
                                        onUpdate2FaCode = { viewModel.update2FaInputCode(it) },
                                        onSend2FaSms = { viewModel.send2FaSmsCode() },
                                        onSend2FaWhatsApp = { viewModel.send2FaWhatsApp() },
                                        onSend2FaTelegram = { viewModel.send2FaTelegram() },
                                        onSend2FaEmailOtp = { viewModel.send2FaEmailOtp() },
                                        onVerify2FaCode = { viewModel.submit2FaVerificationCode() },
                                        onSignOut = { viewModel.signOut() },
                                        onUpdateEmail = { email, pass -> viewModel.updateEmail(email, pass) },
                                        onUpdatePassword = { old, new -> viewModel.updatePassword(old, new) },
                                        onDeleteAccount = { viewModel.deleteAccount() },
                                        onDeleteResult = { viewModel.deleteResult(it) },
                                        onClearHistory = { viewModel.clearHistory() },
                                        onManualSync = { viewModel.performManualSync() },
                                        onResetPassword = { viewModel.resetPassword(it) },
                                        onReportIssue = { viewModel.reportServiceIssue(it) },
                                        onVerifyService = { viewModel.verifyServiceStatus(it) },
                                        onSelectService = { viewModel.selectService(it) },
                                        onSearchServices = { viewModel.updateServiceSearch(it) },
                                        onAddUnlistedService = { name, url -> viewModel.addUnlistedService(name, url) },
                                        onToggleDownFirst = { viewModel.toggleDownFirst() },
                                        onRestart = { viewModel.onRestart() },
                                        onAddDomain = { suiteViewModel.addDomain(it) },
                                        onRemoveDomain = { suiteViewModel.removeDomain(it) },
                                        onRefreshDomains = { suiteViewModel.refreshAll() },
                                        onRunDNSCheck = { d, t -> suiteViewModel.runDNSCheck(d, t) },
                                        onRunWhois = { q -> suiteViewModel.runWhois(q) },
                                        onRunPortScan = { t, p -> suiteViewModel.runPortScan(t, p) },
                                        onRunTraceroute = { t -> suiteViewModel.runTraceroute(t) },
                                        onRunAudit = { d -> suiteViewModel.runWebVitalsAudit(d) },
                                        onFetchDevices = { suiteViewModel.fetchLinkedDevices(syncManager) },
                                        onTriggerDevice = { devId -> suiteViewModel.triggerRemoteDeviceTest(syncManager, devId) },
                                        onOpenTool = { tool ->
                                            when(tool) {
                                                "dns" -> viewModel.navigateTo(Screen.DNSChecker)
                                                "whois" -> viewModel.navigateTo(Screen.WhoisLookup)
                                                "portscan" -> viewModel.navigateTo(Screen.PortScanner)
                                                "trace" -> viewModel.navigateTo(Screen.Traceroute)
                                                "heatmap" -> viewModel.navigateTo(Screen.AnomalyHeatmap)
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
                                        onOpenSuite = { viewModel.navigateTo(Screen.Suite) }
                                    )

                                    val defaultPrimary = if (isLightMode) Color(0xFFFF9F0A) else Color(0xFF00E5FF)
                                    val pColor = if ((state.neonPrimaryColor == "#00E5FF" || state.neonPrimaryColor.isEmpty()) && isLightMode) {
                                        Color(0xFFFF9F0A)
                                    } else {
                                        state.neonPrimaryColor.toColor().takeOrElse { defaultPrimary }
                                    }

                                    val btnBg = pColor.copy(alpha = 0.12f)
                                    val btnFg = pColor
                                    val closeBg = Color.Red.copy(alpha = 0.15f)
                                    val closeFg = Color.Red

                                    val centeredTextStyle = TextStyle(
                                        textAlign = TextAlign.Center
                                    )

                                    // 2. Custom Title Bar overlays transparently on top at Y = 0!
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().height(48.dp).align(Alignment.TopCenter),
                                        color = Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Spacer(Modifier.weight(1f))

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Minimize Button
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(btnBg)
                                                        .clickable { windowState.isMinimized = true }
                                                        .cursorHoverable(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "—",
                                                        color = btnFg,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        style = centeredTextStyle,
                                                        modifier = Modifier.offset(y = (-1).dp)
                                                    )
                                                }

                                                Spacer(Modifier.width(10.dp))

                                                // Maximize / Restore Button
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(btnBg)
                                                        .clickable {
                                                            windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                                                WindowPlacement.Floating
                                                            } else {
                                                                WindowPlacement.Maximized
                                                            }
                                                        }
                                                        .cursorHoverable(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (windowState.placement == WindowPlacement.Maximized) "❐" else "□",
                                                        color = btnFg,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        style = centeredTextStyle,
                                                        modifier = Modifier.offset(y = (-1).dp)
                                                    )
                                                }

                                                Spacer(Modifier.width(10.dp))

                                                // Close Button
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(closeBg)
                                                        .clickable { isWindowVisible = false }
                                                        .cursorHoverable(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "✕",
                                                        color = closeFg,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Black,
                                                        style = centeredTextStyle,
                                                        modifier = Modifier.offset(y = (-1).dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. ABSOLUTE BOTTOM DUAL 300x50 AD BAR (NO OVERLAP WITH APP CONTENT)
                                DesktopDual300x50AdRow(
                                    state = state,
                                    isLightMode = isLightMode
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activeDesktopToast by DesktopToastManager.activeToast.collectAsState()

    // Auto-clear active toast after 4.5 seconds (unless persistent)
    LaunchedEffect(activeDesktopToast) {
        if (activeDesktopToast != null && activeDesktopToast?.persistent != true) {
            delay(4500)
            DesktopToastManager.clearToast()
        }
    }

    val currentToastData = activeDesktopToast
    val showToastWindow = currentToastData != null || state.testRunning || (state.testFinished && state.isRemoteTest)

    // --- SLEEK COMPOSE TOAST OVERLAY (ALL APP TOASTS, REMOTE / SPEED TESTS) ---
    if (showToastWindow) {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val toastWidth = 390.dp
        val toastHeight = if (currentToastData?.actionLabel != null) 165.dp else 105.dp
        
        val x = screenSize.width - 420
        val y = screenSize.height - (if (currentToastData?.actionLabel != null) 225 else 165)

        Window(
            onCloseRequest = {},
            state = rememberWindowState(
                position = WindowPosition(x.dp, y.dp),
                width = toastWidth,
                height = toastHeight
            ),
            title = "Mr Speed Toast Overlay",
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            focusable = currentToastData?.persistent == true || currentToastData?.actionLabel != null
        ) {
            val toastLight = currentToastData?.isLightMode ?: isLightMode
            val pColor = if (toastLight) Color(0xFFFF9F0A) else Color(0xFF00E5FF)

            val toastBg = if (toastLight) Color(0xFFF4F4F8).copy(alpha = 0.98f) else Color(0xFF0A0A0F).copy(alpha = 0.98f)
            val textFg = if (toastLight) Color(0xFF111115) else Color.White
            val dismissBg = if (toastLight) Color(0xFFE2E2E8) else Color.DarkGray
            val dismissFg = if (toastLight) Color(0xFF111115) else Color.White

            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, pColor, RoundedCornerShape(16.dp)),
                    color = toastBg
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentToastData != null) {
                            // GENERAL APPLICATION / VPN / 2FA TOAST
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF00FF64), CircleShape)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = currentToastData.title.uppercase(),
                                    color = textFg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = currentToastData.message,
                                color = pColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!currentToastData.actionLabel.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(pColor)
                                            .clickable {
                                                isWindowVisible = true
                                                currentToastData.onAction?.invoke()
                                                DesktopToastManager.clearToast()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentToastData.actionLabel,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            style = TextStyle(textAlign = TextAlign.Center),
                                            modifier = Modifier.offset(y = 1.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(dismissBg)
                                            .clickable {
                                                currentToastData.onDismiss?.invoke()
                                                DesktopToastManager.clearToast()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "DISMISS",
                                            color = dismissFg,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            style = TextStyle(textAlign = TextAlign.Center),
                                            modifier = Modifier.offset(y = 1.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // SPEED TEST / REMOTE TEST TOAST
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (state.testRunning) pColor else Color(0xFF00FF64), CircleShape)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (state.testRunning) {
                                        if (state.isRemoteTest) "REMOTE TEST IN PROGRESS" else "SPEED TEST IN PROGRESS"
                                    } else {
                                        if (state.isRemoteTest) "REMOTE TEST COMPLETE" else "SPEED TEST COMPLETE"
                                    },
                                    color = textFg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            Spacer(Modifier.height(10.dp))
                            
                            if (state.testRunning) {
                                val progress = if (state.dlProgress < 1.0) state.dlProgress * 0.5f else 0.5f + (state.ulProgress * 0.5f)
                                val label = if (state.dlProgress < 1.0) "Downloading..." else "Uploading..."
                                
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(label, color = if (toastLight) Color.DarkGray else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                    Text("${(progress * 100).toInt()}%", color = pColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = pColor,
                                    trackColor = pColor.copy(alpha = 0.15f)
                                )
                            } else {
                                Text(
                                    text = "Result: ${state.dlSpeed.toInt()} Mbps Down / ${state.ulSpeed.toInt()} Mbps Up",
                                    color = pColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                
                                LaunchedEffect(Unit) {
                                    delay(5000)
                                    viewModel.updateState { it.copy(isRemoteTest = false) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
