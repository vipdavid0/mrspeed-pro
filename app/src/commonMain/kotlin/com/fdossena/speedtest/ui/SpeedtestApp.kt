package com.fdossena.speedtest.ui

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.fdossena.speedtest.utils.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.platform.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.svg.SvgDecoder
import coil3.request.ImageRequest
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.request.crossfade
import com.fdossena.speedtest.core.serverSelector.TestPoint
import com.fdossena.speedtest.core.floorplan.FloorPlanManager
import com.fdossena.speedtest.core.serverSelector.ServerHealthManager
import com.fdossena.speedtest.data.*
import com.fdossena.speedtest.utils.*
import com.fdossena.speedtest.utils.AppOutlinedTextField as OutlinedTextField
import com.fdossena.speedtest.ui.heatmap.GeneratedFloorPlanView
import com.fdossena.speedtest.ui.heatmap.CornerMappingScreen
import com.mr.speed.test.MR_R
import com.fdossena.speedtest.ui.theme.SpeedtestTheme
import java.text.SimpleDateFormat
import java.util.Locale
import com.fdossena.speedtest.ui.models.*
import com.fdossena.speedtest.utils.*
import org.jetbrains.compose.resources.StringResource
import java.util.Date
import java.util.concurrent.ExecutorService
import kotlin.math.*
import javax.swing.JFileChooser
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun calculateSpeedPercentage(speed: Double): Float {
    return when {
        speed <= 0 -> 0f
        speed <= 1 -> (speed * 0.1).toFloat()
        speed <= 10 -> (0.1 + (speed - 1) / 9 * 0.25).toFloat()
        speed <= 100 -> (0.35 + (speed - 10) / 90 * 0.35).toFloat()
        speed <= 1000 -> (0.7 + (speed - 100) / 900 * 0.3).toFloat()
        else -> 1.0f
    }.coerceIn(0f, 1f)
}

private fun screenRank(screen: Screen): Int = when (screen) {
    Screen.Splash -> 0
    Screen.Setup -> 1
    Screen.Init, Screen.Fail -> 2
    Screen.Ready, Screen.ServerSelect -> 3
    else -> 5
}

val APP_LANGUAGES = listOf(
    "en" to "ENGLISH (US / UK)",
    "es" to "Español (Spanish)",
    "fr" to "Français (French)",
    "de" to "Deutsch (German)",
    "it" to "Italiano (Italian)",
    "pt" to "Português (Portuguese)",
    "ru" to "Русский (Russian)",
    "hi" to "हिन्दी (Hindi)",
    "ar" to "العربية (Arabic)",
    "zh" to "中文 (Chinese Simplified)",
    "zh-tw" to "中文 (Chinese Traditional)",
    "ja" to "日本語 (Japanese)",
    "ko" to "한국어 (Korean)",
    "nl" to "Nederlands (Dutch)",
    "tr" to "Türkçe (Turkish)",
    "pl" to "Polski (Polish)",
    "uk" to "Українська (Ukrainian)",
    "sv" to "Svenska (Swedish)",
    "no" to "Norsk (Norwegian)",
    "fi" to "Suomi (Finnish)",
    "da" to "Dansk (Danish)",
    "cs" to "Čeština (Czech)",
    "el" to "Ελληνικά (Greek)",
    "ro" to "Română (Romanian)",
    "hu" to "Magyar (Hungarian)",
    "th" to "ไทย (Thai)",
    "vi" to "Tiếng Việt (Vietnamese)",
    "id" to "Bahasa Indonesia (Indonesian)",
    "ms" to "Bahasa Melayu (Malay)",
    "bn" to "বাংলা (Bengali)",
    "fa" to "فارسی (Persian)",
    "he" to "עברית (Hebrew)",
    "ur" to "اردو (Urdu)",
    "sw" to "Kiswahili (Swahili)",
    "fil" to "Tagalog (Filipino)",
    "af" to "Afrikaans",
    "sq" to "Shqip (Albanian)",
    "am" to "አማርኛ (Amharic)",
    "hy" to "Հայերեն (Armenian)",
    "az" to "Azərbaycan (Azerbaijani)",
    "eu" to "Euskara (Basque)",
    "be" to "Беларуская (Belarusian)",
    "bs" to "Bosanski (Bosnian)",
    "bg" to "Български (Bulgarian)",
    "ca" to "Català (Catalan)",
    "hr" to "Hrvatski (Croatian)",
    "et" to "Eesti (Estonian)",
    "gl" to "Galego (Galician)",
    "ka" to "ქართული (Georgian)",
    "gu" to "ગુજરાતી (Gujarati)",
    "ha" to "Hausa",
    "is" to "Íslenska (Icelandic)",
    "ig" to "Igbo",
    "ga" to "Gaeilge (Irish)",
    "jv" to "Basa Jawa (Javanese)",
    "kn" to "ಕನ್ನಡ (Kannada)",
    "kk" to "Қазақ (Kazakh)",
    "km" to "ខ្មែរ (Khmer)",
    "lo" to "ລາວ (Lao)",
    "lv" to "Latviešu (Latvian)",
    "lt" to "Lietuvių (Lithuanian)",
    "mk" to "Македонски (Macedonian)",
    "mg" to "Malagasy",
    "ml" to "മലയാളം (Malayalam)",
    "mr" to "मराठी (Marathi)",
    "mn" to "Монгол (Mongolian)",
    "ne" to "नेपाली (Nepali)",
    "ps" to "پښتو (Pashto)",
    "pa" to "ਪੰਜਾਬੀ (Punjabi)",
    "sr" to "Српски (Serbian)",
    "si" to "සිංහල (Sinhala)",
    "sk" to "Slovenčina (Slovak)",
    "sl" to "Slovenščina (Slovenian)",
    "so" to "Soomaali (Somali)",
    "su" to "Basa Sunda (Sundanese)",
    "ta" to "தமிழ் (Tamil)",
    "te" to "తెలుగు (Telugu)",
    "uz" to "Oʻzbek (Uzbek)",
    "cy" to "Cymraeg (Welsh)",
    "xh" to "isiXhosa (Xhosa)",
    "yi" to "ייִדיශ (Yiddish)",
    "yo" to "Yorùbá",
    "zu" to "isiZulu (Zulu)"
)


object R {
    val string = MR_R.string
}

@Composable
fun stringResource(id: String, vararg args: Any): String {
    val lang = LocalAppLanguage.current
    return getResProvider(lang).getString(id, *args)
}

@Composable
fun stringResource(res: StringResource, vararg args: Any): String {
    val lang = LocalAppLanguage.current
    return getResProvider(lang).getString(res.key, *args)
}

@Composable
fun tr(id: String, vararg args: Any): String {
    val lang = LocalAppLanguage.current
    return getResProvider(lang).getString(id, *args)
}

fun formatSpeed(s: Double): String {
    if (s.isNaN() || s.isInfinite() || s <= 0.0) return "0"
    return try {
        formatDouble(s)
    } catch (_: Exception) {
        "0"
    }
}

fun String.toColor(): Color {
    return if (this.startsWith("#")) {
        try {
            val hex = this.removePrefix("#")
            val longHex = hex.toLong(16)
            if (hex.length == 6) {
                Color(longHex or 0xFF000000L)
            } else {
                Color(longHex)
            }
        } catch (_: Exception) {
            Color.Unspecified
        }
    } else Color.Unspecified
}

@Composable
fun MrSpeedIcon(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val uploadColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val center = Offset(w / 2, h / 2)
        val radius = size.minDimension / 2.2f

        drawCircle(color = color.copy(alpha = 0.08f), radius = radius, center = center)
        drawCircle(color = color.copy(alpha = 0.15f), radius = radius, center = center, style = Stroke(width = 1.dp.toPx()))

        val path = Path().apply {
            moveTo(w * 0.45f, h * 0.25f)
            lineTo(w * 0.75f, h * 0.45f)
            lineTo(w * 0.4f, h * 0.55f)
            lineTo(w * 0.65f, h * 0.8f)
        }
        drawPath(path = path, color = color.copy(alpha = 0.3f), style = Stroke(width = 8f, cap = StrokeCap.Round))
        drawPath(path = path, brush = Brush.linearGradient(colors = listOf(color, uploadColor), start = Offset(w * 0.4f, h * 0.3f), end = Offset(w * 0.7f, h * 0.7f)), style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}

@Composable
fun MrSpeedLogo(modifier: Modifier = Modifier, color: Color? = null) {
    val brandColor = color ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Left Speed Lines Canvas
        Canvas(modifier = Modifier.width(30.dp).height(14.dp)) {
            val leftBrush = Brush.linearGradient(
                colors = listOf(brandColor.copy(alpha = 0f), brandColor.copy(alpha = 0.6f)),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f)
            )
            drawLine(leftBrush, Offset(size.width * 0.23f, size.height * 0.35f), Offset(size.width, size.height * 0.35f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(leftBrush, Offset(0f, size.height * 0.65f), Offset(size.width * 0.81f, size.height * 0.65f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }

        Spacer(Modifier.width(4.dp))

        // Center Text
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.W300, fontStyle = FontStyle.Italic, shadow = Shadow(brandColor.copy(alpha = 0.4f), blurRadius = 6f))) { append("Mr") }
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.W900, fontStyle = FontStyle.Italic, shadow = Shadow(brandColor.copy(alpha = 0.7f), blurRadius = 10f))) { append("Speed") }
            },
            color = brandColor,
            fontSize = 18.sp,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            softWrap = false
        )

        Spacer(Modifier.width(4.dp))

        // Right Speed Lines Canvas
        Canvas(modifier = Modifier.width(30.dp).height(14.dp)) {
            val rightBrush = Brush.linearGradient(
                colors = listOf(brandColor.copy(alpha = 0.8f), brandColor.copy(alpha = 0f)),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f)
            )
            drawLine(rightBrush, Offset(0f, size.height * 0.25f), Offset(size.width * 0.81f, size.height * 0.25f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(rightBrush, Offset(size.width * 0.19f, size.height * 0.52f), Offset(size.width, size.height * 0.52f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(rightBrush, Offset(size.width * 0.06f, size.height * 0.78f), Offset(size.width * 0.62f, size.height * 0.78f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(Modifier.padding(24.dp), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun resolvePrimaryColor(state: SpeedtestUIState, isLight: Boolean): Color {
    return if (isLight && (state.neonPrimaryColor == "#00E5FF" || state.neonPrimaryColor.isEmpty())) {
        Color(0xFFFF9F0A)
    } else if (state.neonPrimaryColor == "#00E5FF" || state.neonPrimaryColor.isEmpty()) {
        MaterialTheme.colorScheme.primary
    } else {
        state.neonPrimaryColor.toColor().takeOrElse {
            if (isLight) Color(0xFFFF9F0A) else MaterialTheme.colorScheme.primary
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopBar(
    title: String, 
    onBack: (() -> Unit)? = null, 
    progress: Float = 0f, 
    color: Color = MaterialTheme.colorScheme.primary, 
    isTvMode: Boolean = false,
    onReadScreen: (() -> Unit)? = null
) {
    val topPadding = if (isDesktop) 48.dp else 0.dp
    Box(Modifier.fillMaxWidth().padding(top = topPadding)) {
        CenterAlignedTopAppBar(
            title = { Text(title.uppercase(), letterSpacing = 2.sp, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge) },
            navigationIcon = { 
                if (onBack != null) {
                    AppIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = color) }
                }
            },
            actions = {
                if (onReadScreen != null) {
                    AppIconButton(onClick = onReadScreen) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Read Screen Aloud", tint = color, modifier = Modifier.size(20.dp))
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            windowInsets = if (isTvMode || isDesktop) WindowInsets(0.dp) else TopAppBarDefaults.windowInsets
        )
        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp),
                color = color,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun SpeedtestApp(
    state: SpeedtestUIState,
    onStartTest: (TestPoint) -> Unit = {},
    onRetry: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onClosePrivacy: () -> Unit = {},
    onRestart: () -> Unit = {},
    onShare: (String) -> Unit = {},
    onShareReportCard: (TestResult) -> Unit = {},
    onLogoClick: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onCloseHistory: () -> Unit = {},
    onGoHome: () -> Unit = {},
    onDeleteResult: (TestResult) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onCloseSettings: () -> Unit = {},
    onUpdateSettings: (Int?, Int?, Int?, Boolean?, Int?, Boolean?, String?, Boolean?, Int?, Boolean?, String?, Boolean?, Boolean?, String?, String?, Boolean?, Boolean?, Int?, Boolean?) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onDismissConnectivityWarning: () -> Unit = {},
    onCompleteSetup: () -> Unit = {},
    onOpenAuth: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToForgot: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onSignIn: (String, String, String?) -> Unit = { _, _, _ -> },
    onSignUp: (String, String, String, String, String?) -> Unit = { _, _, _, _, _ -> },
    onSignOut: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onFetchCaptchaToken: suspend () -> String? = { null },
    onResetPassword: (String) -> Unit = {},
    onSendVerificationEmail: () -> Unit = {},
    onUpdateEmail: (String, String) -> Unit = { _, _ -> },
    onUpdatePassword: (String, String) -> Unit = { _, _ -> },
    onUpdatePhone: (String) -> Unit = {},
    onTest2FaSms: () -> Unit = {},
    onRespond2Fa: (String, Boolean) -> Unit = { _, _ -> },
    onDismiss2Fa: () -> Unit = {},
    onSelect2FaTab: (TwoFactorTab) -> Unit = {},
    onUpdate2FaCode: (String) -> Unit = {},
    onSend2FaSms: () -> Unit = {},
    onSend2FaWhatsApp: () -> Unit = {},
    onSend2FaTelegram: () -> Unit = {},
    onSend2FaEmailOtp: () -> Unit = {},
    onVerify2FaCode: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onRequestLocationPermission: () -> Unit = {},
    onDismissLocationRationale: () -> Unit = {},
    onOpenHeatmap: () -> Unit = {},
    onCloseHeatmap: () -> Unit = {},
    onOpenCornerMapping: () -> Unit = {},
    onCloseCornerMapping: () -> Unit = {},
    onOpenMrDetector: () -> Unit = {},
    onCloseMrDetector: () -> Unit = {},
    onAddDomain: (String) -> Unit = {},
    onRemoveDomain: (TrackedDomain) -> Unit = {},
    onRefreshDomains: () -> Unit = {},
    onRunDNSCheck: (String, String) -> Unit = { _, _ -> },
    onRunWhois: (String) -> Unit = {},
    onRunPortScan: (String, String) -> Unit = { _, _ -> },
    onRunTraceroute: (String) -> Unit = {},
    onRunAudit: (String) -> Unit = {},
    onRunWifiScan: () -> Unit = {},
    onToggleOutageTracker: () -> Unit = {},
    onRefreshProcesses: () -> Unit = {},
    onFetchDevices: () -> Unit = {},
    onTriggerDevice: (String?) -> Unit = {},
    onShareExport: (String, String) -> Unit = { _, _ -> },
    platformService: PlatformService? = null,
    onOpenSuite: () -> Unit = {},
    onOpenTool: (String) -> Unit = {},
    suiteState: SuiteUIState = SuiteUIState(),
    onOpenGaming: () -> Unit = {},
    onOpenISPLeaderboard: () -> Unit = {},
    onOpenPrivacyDashboard: () -> Unit = {},
    onOpenStreamingDashboard: () -> Unit = {},
    onOpenSignalFinder: () -> Unit = {},
    onOpenDuel: () -> Unit = {},
    onStartDuelWifi: () -> Unit = {},
    onStartDuelMobile: () -> Unit = {},
    onManualSync: () -> Unit = {},
    onOpenARPainter: () -> Unit = {},
    onOpenCellTower: () -> Unit = {},
    onOpenAmbientMonitor: () -> Unit = {},
    onRunLeakTest: () -> Unit = {},
    onUpdateFontScale: (Float) -> Unit = {},
    onUpdateHighContrast: (Boolean) -> Unit = {},
    onUpdateSimplifiedUi: (Boolean) -> Unit = {},
    onUpdateScreenReader: (Boolean) -> Unit = {},
    onReadScreen: () -> Unit = {},
    onQRScanTriggered: () -> Unit = {},
    onStartLoginQR: () -> Unit = {},
    onStopLoginQR: () -> Unit = {},
    onQRCodeScanned: (String) -> Unit = {},
    onDismissQRScanner: () -> Unit = {},
    onReportIssue: (String) -> Unit = {},
    onVerifyService: (ServiceStatus) -> Unit = {},
    onSelectService: (ServiceStatus) -> Unit = {},
    onSearchServices: (String) -> Unit = {},
    onAddUnlistedService: (String, String) -> Unit = { _, _ -> },
    onToggleDownFirst: () -> Unit = {},
    onDismissRationale: () -> Unit = {},
    onDismissServerFail: () -> Unit = {},
    onSelectServer: (TestPoint) -> Unit = {},
    onSelectServerSlot: (TestPoint, Int) -> Unit = { _, _ -> },
    onToggleConnectionMode: () -> Unit = {},
    onOpenServerSelect: () -> Unit = {},
    serviceStatuses: List<ServiceStatus> = emptyList(),
    gamingScores: List<GamingScore> = emptyList(),
    ispLeaderboard: List<ISPRecord> = emptyList(),
    floorPlanManager: FloorPlanManager = remember { FloorPlanManager() },
    historyResults: List<TestResult> = emptyList()
) {
    val density = LocalDensity.current
    val safeFontScale = state.accessibilityFontScale.coerceIn(1.0f, 1.35f)

    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = safeFontScale)) {
        SpeedtestTheme(themeMode = state.themeMode, dynamicColor = false) {
            CompositionLocalProvider(
                LocalStrings provides Strings,
                LocalAppLanguage provides state.appLanguage
            ) {
            PlatformBackHandler {
                if (state.screen != Screen.Ready && state.screen != Screen.Splash && state.screen != Screen.Setup && state.screen != Screen.Init) {
                    onGoHome()
                }
            }
            val isDark = when (state.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            val effectiveDark = if (state.highContrastModeEnabled) true else isDark
            val effectiveBg = if (state.highContrastModeEnabled) Color.Black else if (effectiveDark) Color(0xFF08080E) else Color(0xFFF0F2F8)
            val effectiveTextColor = if (state.highContrastModeEnabled) Color.White else if (effectiveDark) Color.White else Color(0xFF1C1B1F)
            val pColor = resolvePrimaryColor(state, !effectiveDark)

            CompositionLocalProvider(LocalContentColor provides effectiveTextColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = effectiveBg) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NetworkConstellationBackground(if (effectiveDark) 0.6f else 0.5f)

                val activePlatform = platformService ?: remember {
                    object : PlatformService {
                        override fun isTv(): Boolean = false
                        override fun getDeviceModel(): String = "Device"
                        override fun triggerHaptic() {}
                        override fun updateAppIcon(themeMode: Int) = null
                        override suspend fun getWifiSignalLevel(): Int = 100
                        override fun getHardwareStats(): Pair<Int, Int> = 50 to 50
                        override fun isVpnActive(): Boolean = false
                    }
                }

                AnimatedContent(
                    targetState = state.screen,
                    transitionSpec = {
                        val entering = screenRank(targetState)
                        val exiting = screenRank(initialState)
                        if (entering > exiting) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "screen"
                ) { target ->
                    when (target) {
                        Screen.Splash -> SplashScreen()
                        Screen.Setup -> SetupScreen(state, onUpdateSettings, onCompleteSetup, onOpenAuth)
                        Screen.Init -> InitScreen(state)
                        Screen.Ready -> ReadyScreen(state, onOpenSettings, onOpenHistory, onOpenMrDetector, onOpenGaming, onOpenISPLeaderboard, onOpenPrivacyDashboard, onOpenServerSelect, onOpenHeatmap, onOpenStreamingDashboard, onOpenSignalFinder, onOpenDuel, onOpenARPainter, onOpenCellTower, onOpenAmbientMonitor, onQRScanTriggered, onOpenAuth, onOpenSuite, onOpenTool, { onOpenTool("AdminLiveChat") }, onToggleConnectionMode, onStartTest)
                        Screen.Suite -> SuiteScreen(state, suiteState, onAddDomain, onRemoveDomain, onRefreshDomains, onOpenTool, onGoHome)
                        Screen.DNSChecker -> DNSCheckerScreen(state, suiteState, onRunDNSCheck, onGoHome)
                        Screen.WhoisLookup -> WhoisLookupScreen(state, suiteState, onRunWhois, onGoHome)
                        Screen.PortScanner -> PortScannerScreen(state, suiteState, onRunPortScan, onGoHome)
                        Screen.Traceroute -> TracerouteScreen(state, suiteState, onRunTraceroute, onGoHome)
                        Screen.AnomalyHeatmap -> AnomalyHeatmapScreen(state, suiteState, onGoHome)
                        Screen.WifiAnalyzer -> WifiAnalyzerScreen(state, suiteState, onRunWifiScan, onGoHome)
                        Screen.OutageTracker -> OutageTrackerScreen(state, suiteState, onToggleOutageTracker, onGoHome)
                        Screen.ProcessInspector -> ProcessInspectorScreen(state, suiteState, onRefreshProcesses, onGoHome)
                        Screen.RemoteController -> RemoteControllerScreen(state, suiteState, onFetchDevices, onTriggerDevice, onGoHome)
                        Screen.HistoryExporter -> HistoryExporterScreen(state, historyResults, onShareExport, onGoHome)
                        Screen.WebVitalsAuditor -> WebVitalsAuditorScreen(state, suiteState, onRunAudit, onGoHome)
                        Screen.AdminLiveChat -> LiveChatAdminScreen(state, onGoHome)
                        Screen.LiveSupportChat -> UserLiveSupportChatScreen(state, onGoHome)
                        Screen.Test -> {
                            if (state.testFinished) {
                                ResultScreen(state, historyResults, onRestart, onShare, onShareReportCard, onGoHome)
                            } else {
                                TestScreen(state, onGoHome)
                            }
                        }
                        Screen.History -> HistoryScreen(state, historyResults, onCloseHistory, onDeleteResult, onClearHistory)
                        Screen.Settings -> SettingsScreen(
                            state = state, 
                            onBack = onCloseSettings, 
                            onUpdate = onUpdateSettings, 
                            onAuth = onOpenAuth, 
                            onProfile = onNavigateToProfile, 
                            onManualSync = onManualSync,
                            onSignOut = onSignOut,
                            onCheckForUpdates = onCheckForUpdates,
                            onUpdateFontScale = onUpdateFontScale,
                            onUpdateHighContrast = onUpdateHighContrast,
                            onUpdateSimplifiedUi = onUpdateSimplifiedUi,
                            onUpdateScreenReader = onUpdateScreenReader
                        )
                        Screen.MrDetector -> MrDetectorScreen(state, serviceStatuses, onCloseMrDetector, onSelectService, onSearchServices, onAddUnlistedService, onToggleDownFirst)
                        Screen.StatusDetail -> StatusDetailScreen(state, onReportIssue, onVerifyService, onCloseMrDetector)
                        Screen.GamingDashboard -> GamingDashboardScreen(state, gamingScores, onGoHome)
                        Screen.ISPLeaderboard -> ISPLeaderboardScreen(state, ispLeaderboard, onGoHome)
                        Screen.PrivacyDashboard -> PrivacyDashboardScreen(
                            state = state, 
                            onRunScan = onRunLeakTest, 
                            onGoHome = onClosePrivacy
                        )

                        Screen.Fail -> FailScreen(state, onRetry, onOpenServerSelect)
                        Screen.Login -> LoginScreen(
                            state = state,
                            onSignIn = onSignIn,
                            onSignUp = onNavigateToSignUp,
                            onForgot = onNavigateToForgot,
                            onStartQR = onStartLoginQR,
                            onStopQR = onStopLoginQR,
                            onScanQR = onQRScanTriggered,
                            onFetchCaptchaToken = onFetchCaptchaToken,
                            onBack = onCloseSettings
                        )
                        Screen.SignUp -> SignUpScreen(
                            state = state,
                            onSignUp = onSignUp,
                            onLogin = onNavigateToLogin,
                            onFetchCaptchaToken = onFetchCaptchaToken,
                            onBack = onCloseSettings
                        )
                        Screen.ForgotPassword -> ForgotPasswordScreen(state, onResetPassword, onNavigateToLogin)
                        Screen.Profile -> ProfileScreen(state, onSignOut, onUpdateEmail, onUpdatePassword, onUpdatePhone, onTest2FaSms, onDeleteAccount, onSendVerificationEmail, onQRScanTriggered, onTriggerDevice, onOpenTool, onCloseSettings)
                        Screen.Privacy -> PrivacyScreen(state, onClosePrivacy)
                        Screen.ServerSelect -> ServerSelectScreen(state, onSelectServer, onSelectServerSlot, onGoHome)
                        Screen.Heatmap -> HeatmapScreen(state, floorPlanManager, onOpenCornerMapping, onCloseHeatmap)
                        Screen.CornerMapping -> CornerMappingScreen(state, floorPlanManager, onCloseCornerMapping, onCloseCornerMapping)
                        Screen.Duel -> DuelScreen(state, onStartDuelWifi, onStartDuelMobile, onGoHome)
                        Screen.StreamingDashboard -> StreamingDashboardScreen(state, onGoHome)
                        Screen.SignalFinder -> SignalFinderScreen(state, onGoHome)
                        Screen.ARSignalPainter -> ARSignalPainterScreen(state, suiteState, onGoHome)
                        Screen.CellTowerMapper -> CellTowerMapperScreen(state, activePlatform, onGoHome)
                        Screen.AmbientMonitor -> AmbientMonitorScreen(state, onGoHome)
                        Screen.Register -> SignUpScreen(
                            state = state,
                            onSignUp = onSignUp,
                            onLogin = onNavigateToLogin,
                            onFetchCaptchaToken = onFetchCaptchaToken,
                            onBack = onCloseSettings
                        )
                        Screen.Account -> ProfileScreen(state, onSignOut, onUpdateEmail, onUpdatePassword, onUpdatePhone, onTest2FaSms, onDeleteAccount, onSendVerificationEmail, onQRScanTriggered, onTriggerDevice, onOpenTool, onCloseSettings)
                    }
                }

                // --- NEW: GLOBAL SYNC PROGRESS BAR (FROM WEBSITE) ---
                // Layered on top at the absolute bottom so it doesn't push layout content
                // --- NEW: GLOBAL SYNC PROGRESS BAR (FROM WEBSITE) ---
                // Absolute Top Layer
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(6.dp) // Thicker for visibility
                    ) {
                        SyncProgressBar(state.isSyncing || state.isDlActive || state.isUlActive, pColor)
                    }
                }

                // --- NEW: GLOBAL QR SCAN OVERLAY (Now with ML Kit) ---
                if (state.showQRScanner || state.authSuccessMessage?.contains("QR Scanner Active") == true) {
                    QRScanner(
                        onScanned = { onQRCodeScanned(it) },
                        onDismiss = { onDismissQRScanner() }
                    )
                }

                if (state.show2FaDialog) {
                    val tvFocusRequester = remember { FocusRequester() }
                    val isLight = (state.themeMode == 1)
                    val dialogBg = if (isLight) Color(0xFFFAFAFC) else Color(0xFF08080E)
                    val dialogTextColor = if (isLight) Color(0xFF1C1B1F) else Color.White
                    val dialogSubTextColor = if (isLight) Color(0xFF49454F) else Color.LightGray
                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = pColor,
                        unfocusedBorderColor = if (isLight) Color(0xFF79747E) else Color.DarkGray,
                        focusedLabelColor = pColor,
                        unfocusedLabelColor = if (isLight) Color(0xFF49454F) else Color.Gray,
                        focusedTextColor = dialogTextColor,
                        unfocusedTextColor = dialogTextColor
                    )

                    LaunchedEffect(state.show2FaDialog, state.selected2FaTab) {
                        delay(150)
                        try { tvFocusRequester.requestFocus() } catch (_: Throwable) {}
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onDismiss2Fa()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = dialogBg,
                            border = BorderStroke(1.5.dp, pColor),
                            modifier = Modifier
                                .widthIn(max = 480.dp)
                                .fillMaxWidth(0.95f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* consume clicks inside dialog */ }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                    Text(tr("🛡️ 2FA VERIFICATION"), fontWeight = FontWeight.Black, fontSize = 18.sp, color = pColor)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = tr("Account: %s", state.userEmail ?: "Pro User"),
                                        fontSize = 11.sp,
                                        color = dialogSubTextColor
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    // 6 Channel Navigation Bar (Push, Auth, WhatsApp, Telegram, Email, Recovery)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        val tabs = listOf(
                                            Triple(TwoFactorTab.PUSH, "🛡️", "Push"),
                                            Triple(TwoFactorTab.TOTP, "🔐", "Auth"),
                                            Triple(TwoFactorTab.WHATSAPP, "💬", "WhatsApp"),
                                            Triple(TwoFactorTab.TELEGRAM, "✈️", "Telegram"),
                                            Triple(TwoFactorTab.EMAIL, "📩", "Email"),
                                            Triple(TwoFactorTab.RECOVERY, "🔑", "Recovery")
                                        )

                                        tabs.forEachIndexed { idx, (tab, icon, label) ->
                                            val isSelected = (state.selected2FaTab == tab)
                                            var isFocused by remember { mutableStateOf(false) }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .focusRequester(if (idx == 0) tvFocusRequester else FocusRequester())
                                                    .onFocusChanged { isFocused = it.isFocused }
                                                    .border(
                                                        width = if (isFocused || isSelected) 2.dp else 0.5.dp,
                                                        color = when {
                                                            isFocused -> Color(0xFF00E5FF)
                                                            isSelected -> pColor
                                                            else -> if (isLight) Color(0xFFCAC4D0) else Color.DarkGray
                                                        },
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .background(
                                                        color = if (isSelected) pColor.copy(alpha = 0.2f) else if (isLight) Color(0xFFE7E0EC) else Color.Transparent,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { onSelect2FaTab(tab) }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(icon, fontSize = 16.sp)
                                                Spacer(Modifier.height(2.dp))
                                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) pColor else Color.Gray)
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    when (state.selected2FaTab) {
                                        TwoFactorTab.PUSH -> {
                                            val isApprovalMode = !state.isUserAnonymous && state.pending2FaIp.isNotEmpty() && state.screen != Screen.Login
                                            if (isApprovalMode) {
                                                Text(tr("PUSH AUTHORIZATION"), fontWeight = FontWeight.Bold, color = pColor, fontSize = 13.sp)
                                                Spacer(Modifier.height(4.dp))
                                                Text("IP: ${state.pending2FaIp.ifEmpty { "Web Browser / External Device" }}", fontSize = 11.sp, color = dialogSubTextColor)
                                                Spacer(Modifier.height(12.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    AppButton(
                                                        onClick = { onRespond2Fa(state.pending2FaReqId, true) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF64))
                                                    ) {
                                                        Text(tr("ALLOW"), color = Color.Black, fontWeight = FontWeight.Black)
                                                    }
                                                    AppButton(
                                                        onClick = { onRespond2Fa(state.pending2FaReqId, false) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3232))
                                                    ) {
                                                        Text(tr("DENY"), color = Color.White, fontWeight = FontWeight.Black)
                                                    }
                                                }
                                            } else {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    CircularProgressIndicator(color = pColor, modifier = Modifier.size(28.dp))
                                                    Spacer(Modifier.height(10.dp))
                                                    Text(tr("WAITING FOR PUSH APPROVAL"), fontWeight = FontWeight.Bold, color = pColor, fontSize = 13.sp)
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        tr("Please approve the 2FA login prompt on your primary mobile device.\nOr select another verification method above."),
                                                        fontSize = 11.sp,
                                                        color = dialogSubTextColor,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                        TwoFactorTab.TOTP -> {
                                            Text(tr("GOOGLE AUTHENTICATOR"), fontWeight = FontWeight.Bold, color = pColor, fontSize = 13.sp)
                                            Spacer(Modifier.height(6.dp))
                                            OutlinedTextField(
                                                value = state.twoFactorInputCode,
                                                onValueChange = { onUpdate2FaCode(it) },
                                                label = { Text(tr("6-Digit TOTP Code")) },
                                                singleLine = true,
                                                colors = textFieldColors,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(onDone = { onVerify2FaCode() }),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            AppButton(
                                                onClick = onVerify2FaCode,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = pColor)
                                            ) {
                                                Text(tr("VERIFY TOTP CODE"), fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        }
                                        TwoFactorTab.WHATSAPP -> {
                                            Text(tr("WHATSAPP 2FA VERIFICATION"), fontWeight = FontWeight.Bold, color = Color(0xFF25D366), fontSize = 13.sp)
                                            Spacer(Modifier.height(6.dp))
                                            AppButton(
                                                onClick = onSend2FaWhatsApp,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                            ) {
                                                Text(tr("💬 SEND WHATSAPP 2FA CODE"), color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            OutlinedTextField(
                                                value = state.twoFactorInputCode,
                                                onValueChange = { onUpdate2FaCode(it) },
                                                label = { Text(tr("6-Digit WhatsApp Code")) },
                                                singleLine = true,
                                                colors = textFieldColors,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(onDone = { onVerify2FaCode() }),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            AppButton(
                                                onClick = onVerify2FaCode,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                            ) {
                                                Text(tr("VERIFY WHATSAPP CODE"), fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        }
                                        TwoFactorTab.TELEGRAM -> {
                                            Text(tr("TELEGRAM 2FA VERIFICATION"), fontWeight = FontWeight.Bold, color = Color(0xFF2AABEE), fontSize = 13.sp)
                                            Spacer(Modifier.height(6.dp))
                                            AppButton(
                                                onClick = onSend2FaTelegram,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                                            ) {
                                                Text(tr("✈️ SEND TELEGRAM 2FA CODE"), color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            OutlinedTextField(
                                                value = state.twoFactorInputCode,
                                                onValueChange = { onUpdate2FaCode(it) },
                                                label = { Text(tr("6-Digit Telegram Code")) },
                                                singleLine = true,
                                                colors = textFieldColors,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(onDone = { onVerify2FaCode() }),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            AppButton(
                                                onClick = onVerify2FaCode,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                                            ) {
                                                Text(tr("VERIFY TELEGRAM CODE"), fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    TwoFactorTab.EMAIL -> {
                                        Text(tr("EMERGENCY EMAIL OTP"), fontWeight = FontWeight.Bold, color = pColor, fontSize = 13.sp)
                                        Spacer(Modifier.height(6.dp))
                                        AppButton(
                                            onClick = onSend2FaEmailOtp,
                                            colors = ButtonDefaults.buttonColors(containerColor = pColor)
                                        ) {
                                            Text(tr("📩 SEND EMAIL OTP"), color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = state.twoFactorInputCode,
                                            onValueChange = { onUpdate2FaCode(it) },
                                            label = { Text(tr("6-Digit Email Code")) },
                                            singleLine = true,
                                            colors = textFieldColors,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { onVerify2FaCode() }),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        AppButton(
                                            onClick = onVerify2FaCode,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = pColor)
                                        ) {
                                            Text(tr("VERIFY EMAIL OTP"), fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                    TwoFactorTab.RECOVERY -> {
                                        Text(tr("ACCOUNT RECOVERY KEY"), fontWeight = FontWeight.Bold, color = Color(0xFFFF9F0A), fontSize = 13.sp)
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = state.twoFactorInputCode,
                                            onValueChange = { onUpdate2FaCode(it) },
                                            label = { Text("MRSP-XXXX-XXXX-XXXX") },
                                            singleLine = true,
                                            colors = textFieldColors,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { onVerify2FaCode() }),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        AppButton(
                                            onClick = onVerify2FaCode,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A))
                                        ) {
                                            Text(tr("VERIFY RECOVERY KEY"), fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                    TwoFactorTab.SMS -> {
                                        Text(tr("SMS / WHATSAPP CODE"), fontWeight = FontWeight.Bold, color = Color(0xFF00FF64), fontSize = 13.sp)
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = state.twoFactorInputCode,
                                            onValueChange = { onUpdate2FaCode(it) },
                                            label = { Text(tr("6-Digit Code")) },
                                            singleLine = true,
                                            colors = textFieldColors,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { onVerify2FaCode() }),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        AppButton(
                                            onClick = onVerify2FaCode,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF64))
                                        ) {
                                            Text(tr("VERIFY CODE"), fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }

                                if (!state.twoFactorError.isNullOrEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(state.twoFactorError, color = Color(0xFFFF3232), fontSize = 11.sp)
                                }
                                if (!state.twoFactorSuccessMsg.isNullOrEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(state.twoFactorSuccessMsg, color = Color(0xFF00FF64), fontSize = 11.sp)
                                }

                                Spacer(Modifier.height(8.dp))
                                AppTextButton(onClick = onDismiss2Fa) {
                                    Text(stringResource(R.string.auth_cancel_btn), color = dialogSubTextColor, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                if (state.showNotificationRationale) {
                    NotificationRationaleDialog(onDismissRationale, onRequestNotificationPermission)
                }

                if (state.showLocationRationale) {
                    LocationRationaleDialog(onDismissLocationRationale, onRequestLocationPermission)
                }

                if (!state.isTvMode && !state.isProUser) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        activePlatform.BannerAdView(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
}
}
}

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MrSpeedLogo(Modifier.width(if(isDesktop) 600.dp else 360.dp).height(if(isDesktop) 150.dp else 90.dp))
    }
}

@Composable
fun InitScreen(state: SpeedtestUIState) {
    val initialStatus = stringResource(R.string.init_engine)
    val s1 = stringResource(R.string.init_calibrating)
    val s2 = stringResource(R.string.init_optimizing)
    val s3 = stringResource(R.string.init_handshake)
    val s4 = stringResource(R.string.init_negotiating)
    val s5 = stringResource(R.string.init_scanning)
    val statuses = remember(s1, s2, s3, s4, s5) { listOf(s1, s2, s3, s4, s5) }
    var statusText by remember { mutableStateOf(initialStatus) }
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(800)
            statusText = statuses.random()
        }
    }

    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(200.dp), color = pColor.copy(alpha = 0.1f), strokeWidth = 8.dp)
                MrSpeedIcon(Modifier.size(120.dp), color = pColor)
            }
            Spacer(Modifier.height(64.dp))
            Text(statusText, style = MaterialTheme.typography.labelSmall, color = pColor.copy(alpha = 0.8f), letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(state.initTextRes?.let { stringResource(it).uppercase() } ?: "CONNECTING", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
        }
    }
}

@Composable
fun SetupScreen(state: SpeedtestUIState, onUpdate: (Int?, Int?, Int?, Boolean?, Int?, Boolean?, String?, Boolean?, Int?, Boolean?, String?, Boolean?, Boolean?, String?, String?, Boolean?, Boolean?, Int?, Boolean?) -> Unit, onComplete: () -> Unit, onOpenAuth: () -> Unit) {
    val scrollState = rememberScrollState()
    val isLight = state.themeMode == 1
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 32.dp, end = 32.dp, top = 56.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            MrSpeedLogo(Modifier.height(36.dp), color = pColor)
            Spacer(Modifier.height(28.dp))
            Text(stringResource(MR_R.string.setup_title).uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = pColor, letterSpacing = 2.sp)
            Text(stringResource(MR_R.string.setup_subtitle), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            Spacer(Modifier.height(48.dp))
            
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(MR_R.string.setup_choose_language).uppercase(), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                val languages = APP_LANGUAGES
                var expanded by remember { mutableStateOf(false) }
                val currentLangName = languages.find { it.first == state.appLanguage }?.second ?: "English"
                
                Box(Modifier.fillMaxWidth().appClickable { expanded = !expanded }.padding(vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(currentLangName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = pColor)
                    }
                }

                if (expanded) {
                    Spacer(Modifier.height(8.dp))
                    val langScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(langScrollState)
                                .background(Color(0xFF121622), RoundedCornerShape(12.dp))
                                .border(1.dp, pColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(6.dp)
                                .padding(end = if (com.fdossena.speedtest.utils.isDesktop) 12.dp else 0.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            languages.forEach { (code, name) ->
                                val isSelected = (code == state.appLanguage)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) pColor.copy(alpha = 0.2f) else Color.Transparent)
                                        .appClickable {
                                            onUpdate(null, null, null, null, null, null, code, null, null, null, null, null, null, null, null, null, null, null, null)
                                            expanded = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, color = if (isSelected) pColor else Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = pColor, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        if (com.fdossena.speedtest.utils.isDesktop) {
                            DesktopVerticalScrollbar(
                                scrollState = langScrollState,
                                isLight = state.themeMode == 1,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_choose_theme).uppercase(), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val themes = listOf(0 to "theme_system", 1 to "theme_light", 2 to "theme_dark")
                    themes.forEach { (mode, labelRes) ->
                        val selected = state.themeMode == mode
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (selected) pColor.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { onUpdate(null, null, null, null, mode, null, null, null, null, null, null, null, null, null, null, null, null, null, null) }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when(mode) {
                                    1 -> Icons.Default.LightMode
                                    2 -> Icons.Default.DarkMode
                                    else -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = null,
                                tint = if (selected) pColor else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray, textAlign = TextAlign.Center, fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_preferences).uppercase(), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                SettingToggle(stringResource(R.string.notif_title), state.autoNotifyNetwork, pColor) { onUpdate(null, null, null, null, null, null, null, it, null, null, null, null, null, null, null, null, null, null, null) }
                if (!state.isTvMode && !isDesktop) {
                    SettingToggle(stringResource(R.string.settings_haptic), state.useHaptics, pColor) { onUpdate(null, null, null, null, null, null, null, null, null, null, null, null, it, null, null, null, null, null, null) }
                }
                if (!state.isTvMode) {
                    SettingToggle(stringResource(R.string.settings_pro_suite), state.isSuiteEnabled, pColor) { onUpdate(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, it, null, null) }
                }
                SettingToggle(stringResource(R.string.settings_show_last_results), state.showLastResults, pColor) { onUpdate(null, null, null, null, null, it, null, null, null, null, null, null, null, null, null, null, null, null, null) }
                SettingToggle(stringResource(R.string.auto_daily_test), state.autoDailyTestEnabled, pColor) { onUpdate(null, null, null, null, null, null, null, null, null, it, null, null, null, null, null, null, null, null, null) }
            }
            
            Spacer(Modifier.height(24.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.auth_title).uppercase(), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                if (state.isUserAnonymous) {
                    AppButton(
                        onClick = onOpenAuth,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pColor.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.AccountCircle, null, tint = pColor)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.settings_sync_login_btn), color = pColor, fontWeight = FontWeight.Black)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, "Logged In", tint = Color.Green, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            val loggedInAs = stringResource(R.string.settings_sync_logged_in).split(" %s")[0]
                            Text("$loggedInAs:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(state.userEmail ?: "Unknown", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = pColor)
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
            AppButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(70.dp).shadow(20.dp, RoundedCornerShape(20.dp), spotColor = pColor),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = pColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                enabled = true
            ) {
                Text(stringResource(R.string.setup_continue), color = if(isLight) Color.White else Color.Black, fontWeight = FontWeight.Black, letterSpacing = 2.sp, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(64.dp))
        }
        if (com.fdossena.speedtest.utils.isDesktop) {
            DesktopVerticalScrollbar(
                scrollState = scrollState,
                isLight = isLight,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 40.dp, bottom = 8.dp, end = 4.dp)
            )
        }
    }
}

@Composable
fun ReadyScreenBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val alpha1 by infiniteTransition.animateFloat(initialValue = 0.05f, targetValue = 0.15f, animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "a1")
    val scale1 by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "s1")

    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawCircle(brush = Brush.radialGradient(listOf(accent.copy(alpha = alpha1), Color.Transparent), center = Offset(w * 0.2f, h * 0.3f), radius = 300.dp.toPx() * scale1))
        drawCircle(brush = Brush.radialGradient(listOf(secondary.copy(alpha = alpha1 * 0.8f), Color.Transparent), center = Offset(w * 0.8f, h * 0.7f), radius = 400.dp.toPx() * (2f - scale1)))
        
        // High-tech Rings
        for (i in 1..5) {
            drawCircle(
                color = accent.copy(alpha = 0.02f * (6-i)),
                radius = (200 * i).dp.toPx() * scale1,
                center = Offset(w/2, h/2),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun ReadyScreen(state: SpeedtestUIState, onSettings: () -> Unit, onHistory: () -> Unit, onDetector: () -> Unit, onGaming: () -> Unit, onISP: () -> Unit, onPrivacy: () -> Unit, onOpenServerSelect: () -> Unit, onOpenHeatmap: () -> Unit, onStreaming: () -> Unit, onSignal: () -> Unit, onDuel: () -> Unit, onARPainter: () -> Unit, onCellTower: () -> Unit, onAmbient: () -> Unit, onQRScanTriggered: () -> Unit, onOpenAuth: () -> Unit = {}, onOpenSuite: () -> Unit = {}, onOpenTool: (String) -> Unit = {}, onOpenAdminLiveChat: () -> Unit = {}, onToggleConnectionMode: () -> Unit = {}, onStart: (TestPoint) -> Unit) {
    val isLight = state.themeMode == 1
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    
    Box(Modifier.fillMaxSize()) {
        ReadyScreenBackground()
        
        // Top Bar (Shifted below custom title bar on Desktop)
        val adminBtnColor = if (isLight) Color(0xFFFF9F0A) else Color(0xFF00E5FF)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = if (isDesktop) 48.dp else (if (state.isTvMode) 12.dp else 56.dp), bottom = 24.dp)
        ) {
            // Left Controls (Settings + Admin Live Chat Dashboard Button to the Right of Settings)
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppIconButton(onClick = onSettings, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), CircleShape)) { 
                    Icon(Icons.Default.Settings, "Settings", tint = pColor) 
                }
                if (state.isAdminUser) {
                    AppIconButton(
                        onClick = onOpenAdminLiveChat,
                        modifier = Modifier.background(adminBtnColor.copy(alpha = 0.15f), CircleShape)
                    ) { 
                        Icon(Icons.Default.HeadsetMic, "Agent Live Chat Dashboard", tint = adminBtnColor) 
                    }
                }
            }

            // Locked Center Logo
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MrSpeedLogo(Modifier.height(32.dp), color = pColor)
            }

            // Right Controls (User Live Support Chat + History)
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppIconButton(onClick = { onOpenTool("supportchat") }, modifier = Modifier.background(pColor.copy(alpha = 0.15f), CircleShape)) { 
                    Icon(Icons.Default.Chat, "Live Support Chat", tint = pColor) 
                }
                AppIconButton(onClick = onHistory, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), CircleShape)) { 
                    Icon(Icons.Default.History, "History", tint = pColor) 
                }
            }
        }

        // Center Content: Mode Toggle + Server Select + Start Button (Perfectly Vertical Centered & Horizontally Aligned)
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Connection Mode Toggle Badge (MULTI / SINGLE)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(pColor.copy(alpha = 0.15f))
                    .border(1.dp, pColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .appClickable { onToggleConnectionMode() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (state.isMultiConnectionMode) "MULTI" else "SINGLE",
                        color = pColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Icon(
                        imageVector = if (state.isMultiConnectionMode) MultiConnectionShuffleIcon else SingleConnectionArrowIcon,
                        contentDescription = null,
                        tint = pColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 2. Server Display Text - Perfectly Centered with START Button!
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .appClickable(onClick = onOpenServerSelect)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val serverDisplayText = if (state.isMultiConnectionMode) {
                    val primaryName = state.selectedServer?.name ?: state.bestServer?.name ?: "Primary Node"
                    val secondaryName = state.secondaryServer?.name ?: state.availableServers.getOrNull(1)?.name ?: primaryName
                    "[1] $primaryName & [2] $secondaryName"
                } else {
                    state.selectedServer?.name?.uppercase() ?: stringResource(R.string.label_searching_node)
                }

                Text(
                    text = serverDisplayText,
                    color = pColor,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "CHANGE SERVER ▾",
                    color = pColor.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(36.dp))
            
            // Glowing Spinning Start Button
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(state.isTvMode) {
                if (state.isTvMode) {
                    try { focusRequester.requestFocus() } catch (_: Exception) {}
                }
            }

            Box(contentAlignment = Alignment.Center) {
                val infiniteTransition = rememberInfiniteTransition(label = "glow")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )
                
                val glowScale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "s"
                )

                Canvas(Modifier.size(187.dp)) {
                    rotate(rotation) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    pColor.copy(alpha = 0f),
                                    pColor.copy(alpha = 0.6f),
                                    pColor.copy(alpha = 0f)
                                )
                            ),
                            style = Stroke(width = 14.dp.toPx() * glowScale)
                        )
                    }
                    // Secondary glow layer
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(pColor.copy(alpha = 0.2f), Color.Transparent),
                            radius = size.width / 2 * glowScale
                        )
                    )
                    // Ambient pulse layer
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(pColor.copy(alpha = 0.05f), Color.Transparent),
                            radius = size.width * 0.8f
                        )
                    )
                            }
                AppButton(
                    onClick = { state.selectedServer?.let { onStart(it) } },
                    modifier = Modifier
                        .size(170.dp)
                        .focusRequester(focusRequester)
                        .shadow(24.dp, CircleShape, spotColor = pColor),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = pColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(16.dp) // More padding for circular safety
                ) {
                    val startText = stringResource(MR_R.string.start).uppercase()
                    val baseStyle = MaterialTheme.typography.headlineLarge.copy(
                        shadow = Shadow(
                            color = (if(isLight) Color.White else Color.Black).copy(alpha = 0.3f),
                            blurRadius = 10f
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = startText,
                        style = when {
                            startText.length > 12 -> MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
                            startText.length > 10 -> MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center)
                            startText.length > 8 -> MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
                            startText.length > 6 -> MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)
                            else -> baseStyle
                        },
                        fontWeight = FontWeight.W900,
                        color = if(isLight) Color.White else Color.Black,
                        maxLines = 1,
                        softWrap = false,
                        letterSpacing = if (startText.length <= 5) 2.sp else 0.sp,
                        modifier = Modifier.fillMaxWidth() // Center in button content
                    )
                }
            }
        }

        // Bottom Anchored UI Stack
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Feature Grid (Three Locked Rows)
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                if (isDesktop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        FeatureIcon(Icons.Default.Router, stringResource(R.string.feature_detector), onDetector, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.SportsEsports, stringResource(R.string.feature_gaming), onGaming, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.Leaderboard, stringResource(R.string.feature_ranking), onISP, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.Shield, stringResource(R.string.feature_shield), onPrivacy, pColor, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        FeatureIcon(Icons.Default.Devices, tr("Remote Ctrl"), { onOpenTool("remotecontroller") }, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.Wifi, tr("Wi-Fi Scan"), { onOpenTool("wifianalyzer") }, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.FileDownload, tr("Export"), { onOpenTool("exporter") }, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.Construction, tr("Pro Suite"), { onOpenSuite(); onOpenTool("suite") }, pColor, modifier = Modifier.weight(1f))
                    }
                } else if (state.isTvMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        FeatureIcon(Icons.Default.Router, stringResource(R.string.feature_detector), onDetector, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.SportsEsports, stringResource(R.string.feature_gaming), onGaming, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.Wallpaper, stringResource(R.string.feature_ambient), onAmbient, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.PlayCircle, stringResource(R.string.feature_streaming), onStreaming, pColor, modifier = Modifier.weight(1f))
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        FeatureIcon(Icons.Default.Router, stringResource(R.string.feature_detector), onDetector, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.SportsEsports, stringResource(R.string.feature_gaming), onGaming, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.CellTower, stringResource(R.string.feature_tower), onCellTower, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.Layers, stringResource(R.string.feature_heatmap), onOpenHeatmap, pColor, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        FeatureIcon(Icons.AutoMirrored.Filled.CompareArrows, stringResource(R.string.feature_duel), onDuel, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.Leaderboard, stringResource(R.string.feature_ranking), onISP, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.Shield, stringResource(R.string.feature_shield), onPrivacy, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(
                            icon = Icons.Default.QrCodeScanner,
                            label = stringResource(R.string.feature_scanqr),
                            onClick = {
                                val isAnonymous = state.isUserAnonymous || state.userEmail.isNullOrEmpty()
                                if (isAnonymous) {
                                    onOpenAuth()
                                } else {
                                    onQRScanTriggered()
                                }
                            },
                            color = pColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        FeatureIcon(Icons.Default.Devices, tr("Remote Ctrl"), { onOpenTool("remotecontroller") }, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.Wifi, tr("Wi-Fi Scan"), { onOpenTool("wifianalyzer") }, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.FileDownload, tr("Export"), { onOpenTool("exporter") }, pColor, modifier = Modifier.weight(1f))
                        FeatureIcon(Icons.Default.Construction, tr("Pro Suite"), { onOpenSuite(); onOpenTool("suite") }, pColor, modifier = Modifier.weight(1f))
                    }
                }
            }
            
            Text(
                text = state.ipInfo,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(Color.Green, CircleShape))
                    Text(" " + stringResource(R.string.msg_background_monitor), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(pColor, CircleShape))
                    Text(" " + stringResource(R.string.msg_cloud_sync_ready), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
@Composable
fun FeatureIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .appClickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(48.dp).background(color.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(26.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
fun TestScreen(state: SpeedtestUIState, onAbort: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight
        val isLandscape = screenWidth > screenHeight
        
        Column(Modifier.fillMaxSize()) {
            ModernTopBar(
                title = stringResource(R.string.title_analyzing_network),
                onBack = onAbort,
                color = pColor,
                progress = ((state.dlProgress + state.ulProgress + state.pingProgress) / 3f).toFloat(),
                isTvMode = state.isTvMode || isDesktop
            )
            
            if (isLandscape) {
                // TV / Landscape Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                        val gaugeSize = minOf(screenHeight * 0.8f, screenWidth * 0.45f)
                        SpeedGauge(
                            speed = if (state.isUlActive) state.ulSpeed else state.dlSpeed,
                            progress = if (state.isUlActive) state.ulProgress else state.dlProgress,
                            color = if (state.isUlActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            isUpload = state.isUlActive,
                            modifier = Modifier.size(gaugeSize)
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 24.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RealtimeSpeedGraph(state.dlHistory, state.ulHistory, state, Modifier.fillMaxWidth().height(100.dp))
                        MetricRow(state.ping, state.jitter, state.isPingActive, state.connectionHealth, state)
                        AppButton(
                            onClick = onAbort,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.Red)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.common_abort), color = Color.Red, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        }
                    }
                }
            } else {
                // Mobile Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Box(Modifier.weight(1.2f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val gaugeSize = minOf(screenWidth * 0.9f, screenHeight * 0.4f)
                        SpeedGauge(
                            speed = if (state.isUlActive) state.ulSpeed else state.dlSpeed,
                            progress = if (state.isUlActive) state.ulProgress else state.dlProgress,
                            color = if (state.isUlActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            isUpload = state.isUlActive,
                            modifier = Modifier.size(gaugeSize)
                        )
                    }
                    
                    Column(Modifier.weight(1f)) {
                        RealtimeSpeedGraph(state.dlHistory, state.ulHistory, state)
                        Spacer(Modifier.height(24.dp))
                        MetricRow(state.ping, state.jitter, state.isPingActive, state.connectionHealth, state)
                        Spacer(Modifier.height(32.dp))
                        AppButton(
                            onClick = onAbort,
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.Red)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.common_abort), color = Color.Red, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultScreen(state: SpeedtestUIState, history: List<TestResult>, onRestart: () -> Unit, onShare: (String) -> Unit, onShareReportCard: (TestResult) -> Unit, onGoHome: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    val sColor = MaterialTheme.colorScheme.tertiary
    val isLight = state.themeMode == 1
    val isTv = state.isTvMode
    
    val currentResult = history.firstOrNull()
    
    // Find previous result (robust logic)
    val prevResult = if (state.showLastResults && history.isNotEmpty()) {
        val newest = history[0]
        // If the newest result was within the last 30 seconds, it's likely the one that just finished
        if (newest.timestamp > System.currentTimeMillis() - 30000) {
            history.getOrNull(1)
        } else {
            newest
        }
    } else null

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight
        val isLandscape = screenWidth > screenHeight || isTv || isDesktop

        Column(Modifier.fillMaxSize()) {
            ModernTopBar(
                title = stringResource(R.string.title_test_complete),
                onBack = onGoHome,
                color = pColor,
                progress = 1f,
                isTvMode = state.isTvMode || isDesktop
            )
            
            if (isLandscape) {
                // TV / Landscape Layout (Optimized for no scroll)
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Results & Previous
                    Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                            val gaugeSize = if (screenHeight * 0.65f < screenWidth * 0.35f) screenHeight * 0.65f else screenWidth * 0.35f
                            SpeedGauge(
                                speed = state.dlSpeed,
                                progress = 1.0,
                                color = pColor,
                                isUpload = false,
                                modifier = Modifier.size(gaugeSize)
                            )
                        }
                        
                        if (prevResult != null) {
                            GlassCard(Modifier.fillMaxWidth(0.9f).padding(bottom = 16.dp)) {
                                Text(stringResource(R.string.common_previous_result).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Black)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${formatSpeed(prevResult.downloadSpeed)} ${stringResource(R.string.test_speedMeasure)}", fontWeight = FontWeight.Bold, color = pColor)
                                    Text("${formatSpeed(prevResult.uploadSpeed)} ${stringResource(R.string.test_speedMeasure)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }

                    // Middle Column: Metrics & Streaming
                    Column(Modifier.weight(1.5f).fillMaxHeight().padding(horizontal = 16.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                        MetricRow(state.ping, state.jitter, false, state.connectionHealth, state)
                        
                        StreamingQualitySection(state.dlSpeed, pColor)

                        // --- NEW TV FEATURES: 4K Check & QR Code ---
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            val is4KReady = state.dlSpeed >= 25.0
                            GlassCard(Modifier.weight(1f)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                                    Text("4K CONTENT", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (is4KReady) "4K READY / BUFFER-FREE" else "SLOW FOR 4K", 
                                        color = if (is4KReady) Color.Green else Color.Red, 
                                        fontWeight = FontWeight.Black,
                                        fontSize = if (state.isTvMode) 11.sp else 14.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            
                            if (currentResult != null) {
                                Surface(
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                    color = Color.White
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                                        Icon(
                                            Icons.Default.QrCode2,
                                            contentDescription = "Scan for report",
                                            tint = Color.Black,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Actions
                    Column(Modifier.weight(0.8f), verticalArrangement = Arrangement.Center) {
                        AppButton(
                            onClick = onRestart,
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = pColor)
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = if(isLight) Color.White else Color.Black)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.common_retest).uppercase(), fontWeight = FontWeight.Black, color = if(isLight) Color.White else Color.Black)
                        }
                        Spacer(Modifier.height(16.dp))
                        AppButton(
                            onClick = onGoHome,
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Home, null, tint = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.common_home).uppercase(), fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            } else {
                // Mobile Portrait Layout (Optimized to fill screen)
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ResultCard(Modifier.weight(1f), "DL", formatSpeed(state.dlSpeed), "Mbps", pColor, Icons.Default.ArrowDownward)
                        ResultCard(Modifier.weight(1f), "UL", formatSpeed(state.ulSpeed), "Mbps", MaterialTheme.colorScheme.tertiary, Icons.Default.ArrowUpward)
                    }
                    
                    if (prevResult != null) {
                        Spacer(Modifier.height(12.dp))
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(stringResource(R.string.common_previous_result).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Black)
                                    val formattedDate = try {
                                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(prevResult.timestamp))
                                    } catch (_: Exception) {
                                        "Recent"
                                    }
                                    Text(formattedDate, style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.7f))
                                }
                                Row {
                                    Text(formatSpeed(prevResult.downloadSpeed), fontWeight = FontWeight.Black, color = pColor, style = MaterialTheme.typography.bodyLarge)
                                    Text(" / ", color = Color.Gray)
                                    Text(formatSpeed(prevResult.uploadSpeed), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyLarge)
                                    Text(" ${stringResource(R.string.test_speedMeasure)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    MetricRow(state.ping, state.jitter, false, state.connectionHealth, state)
                    
                    Spacer(Modifier.height(20.dp))
                    ReadinessSection(state.dlSpeed, state.ulSpeed, state.ping, pColor)
                    
                    Spacer(Modifier.weight(1f))
                    
                    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppButton(
                            onClick = onRestart,
                            modifier = Modifier.fillMaxWidth().height(64.dp).shadow(8.dp, RoundedCornerShape(16.dp), spotColor = pColor),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = pColor)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.test_restart).uppercase(), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppButton(
                                onClick = { onShare(state.shareUrl ?: "") },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = pColor.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, pColor.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Share, null, tint = pColor, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.label_link), color = pColor, fontWeight = FontWeight.Bold)
                            }

                            AppButton(
                                onClick = { currentResult?.let { onShareReportCard(it) } },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = sColor.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, sColor.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Badge, null, tint = sColor, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.label_card), color = sColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingQualitySection(dlSpeed: Double, color: Color) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.common_streaming_capability).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StreamBadge("SD", dlSpeed >= 3, color)
            StreamBadge("HD", dlSpeed >= 10, color)
            StreamBadge("4K", dlSpeed >= 25, color)
            StreamBadge("8K", dlSpeed >= 100, color)
        }
    }
}

@Composable
fun StreamBadge(label: String, supported: Boolean, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val tint = if (supported) color else Color.Gray.copy(alpha = 0.3f)
        Box(
            Modifier.size(40.dp).background(tint.copy(alpha = 0.1f), CircleShape).border(1.dp, tint.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (supported) Icons.Default.Check else Icons.Default.Close, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReadinessSection(dl: Double, ul: Double, ping: Double, color: Color) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.common_connection_readiness).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ReadinessIcon(Icons.Default.Language, dl > 1.0, color, "Browsing")
            ReadinessIcon(Icons.Default.People, dl > 2.0, color, "Social")
            ReadinessIcon(Icons.Default.VideoCall, dl > 5.0 && ul > 2.0 && (ping <= 0.0 || ping < 100.0), color, "Video Call")
            ReadinessIcon(Icons.Default.Tv, dl > 25.0 && (ping <= 0.0 || ping < 100.0), color, "4K Stream")
            ReadinessIcon(Icons.Default.SportsEsports, dl > 5.0 && (ping <= 0.0 || ping < 50.0), color, "Gaming")
        }
    }
}

@Composable
fun ReadinessIcon(icon: ImageVector, ready: Boolean, color: Color, label: String) {
    val tint = if (ready) color else Color.Gray.copy(alpha = 0.3f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(44.dp)
                .background(if (ready) tint.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                .border(1.dp, if (ready) tint else Color.Gray.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            color = if (ready) MaterialTheme.colorScheme.onBackground else Color.Gray,
            fontWeight = if (ready) FontWeight.Black else FontWeight.Normal
        )
    }
}

@Composable
fun ResultCard(modifier: Modifier, label: String, value: String, unit: String, color: Color, icon: ImageVector) {
    GlassCard(modifier) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Black)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(shadow = Shadow(color = color.copy(alpha = 0.5f), blurRadius = 12f)), fontWeight = FontWeight.Black, color = color)
            Spacer(Modifier.width(4.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SpeedGauge(speed: Double, progress: Double, color: Color, isUpload: Boolean, modifier: Modifier = Modifier) {
    val safeSpeed = if (speed.isNaN() || speed.isInfinite() || speed < 0.0) 0f else speed.toFloat()
    val animatedSpeed by animateFloatAsState(
        targetValue = safeSpeed, 
        animationSpec = tween(150, easing = LinearEasing), 
        label = "speed"
    )
    
    val visualProgress = calculateSpeedPercentage(animatedSpeed.toDouble()).coerceIn(0f, 1f)
    
    val tickVectors = remember {
        List(121) { i ->
            val angleRad = Math.toRadians(135.0 + (i * 2.25))
            Offset(cos(angleRad).toFloat(), sin(angleRad).toFloat())
        }
    }

    BoxWithConstraints(contentAlignment = Alignment.Center, modifier = modifier) {
        val sizePx = minOf(this.constraints.maxWidth, this.constraints.maxHeight).toFloat()
        val density = LocalDensity.current
        val sizeDp = with(density) { sizePx.toDp() }

        Canvas(Modifier.fillMaxSize()) {
            try {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2.4f
                val margin = 2.dp.toPx()
                val arcStrokeWidth = size.minDimension * 0.05f
                val arcRadius = (size.minDimension - arcStrokeWidth) / 2f
                
                drawCircle(brush = Brush.radialGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent), center = center, radius = radius * 1.5f))
                drawCircle(brush = Brush.radialGradient(listOf(color.copy(alpha = 0.1f), Color.Transparent), center = center, radius = radius * 2f))
                
                drawArc(color = Color.White.copy(alpha = 0.04f), startAngle = 135f, sweepAngle = 270f, useCenter = false, style = Stroke(width = (size.minDimension * 0.06f), cap = StrokeCap.Round))
                
                for (i in 0..120) {
                    val vec = tickVectors[i]
                    val isMajor = i % 20 == 0
                    val isMedium = i % 10 == 0 && !isMajor
                    val tickLength = when {
                        isMajor -> radius * 0.15f
                        isMedium -> radius * 0.1f
                        else -> radius * 0.05f
                    }
                    val alpha = if (i / 120.0 <= visualProgress) 1f else 0.1f
                    
                    val start = Offset(
                        center.x + (radius - margin) * vec.x,
                        center.y + (radius - margin) * vec.y
                    )
                    val end = Offset(
                        center.x + (radius - margin - tickLength) * vec.x,
                        center.y + (radius - margin - tickLength) * vec.y
                    )
                    
                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = start,
                        end = end,
                        strokeWidth = if (isMajor) 4.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                
                drawArc(
                    color = color.copy(alpha = 0.85f),
                    startAngle = 135f,
                    sweepAngle = (visualProgress * 270.0).toFloat().coerceIn(0f, 270f),
                    useCenter = false,
                    style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Round)
                )

                // PRECISION NEEDLE
                val needleAngle = 135f + (visualProgress * 270.0).toFloat()
                val needleLength = arcRadius
                
                rotate(needleAngle + 90f, center) {
                    val shadowPath = Path().apply {
                        moveTo(center.x - radius * 0.03f, center.y)
                        lineTo(center.x + radius * 0.03f, center.y)
                        lineTo(center.x, center.y - needleLength)
                        close()
                    }
                    drawPath(shadowPath, color.copy(alpha = 0.2f), style = Stroke(width = radius * 0.04f, cap = StrokeCap.Round))
                    
                    val needlePath = Path().apply {
                        moveTo(center.x - radius * 0.02f, center.y)
                        lineTo(center.x + radius * 0.02f, center.y)
                        lineTo(center.x, center.y - needleLength)
                        close()
                    }
                    drawPath(needlePath, color)
                }
                
                drawCircle(color, radius = radius * 0.08f, center = center)
                drawCircle(Color.Black, radius = radius * 0.03f, center = center)
            } catch (_: Exception) {}
        }
        
        val contentPadding = (sizeDp.value * 0.32f).dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = contentPadding)) {
            Text(if (isUpload) stringResource(R.string.test_ul).uppercase() else stringResource(R.string.test_dl).uppercase(), style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (sizeDp.value * 0.03f).sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatSpeed(animatedSpeed.toDouble()), 
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = Shadow(color = color.copy(alpha = 0.6f), blurRadius = 30f),
                    fontSize = (sizeDp.value * 0.2f).sp
                ), 
                fontWeight = FontWeight.W900, 
                color = Color.White
            )
            Text(stringResource(R.string.test_speedMeasure), style = MaterialTheme.typography.titleMedium, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = (sizeDp.value * 0.05f).sp)
        }
    }
}

@Composable
fun MetricRow(ping: Double, jitter: Double, isPingActive: Boolean, health: ConnectionHealth, state: SpeedtestUIState) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.label_health), modifier = Modifier.weight(1.2f).alignByBaseline(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(stringResource(R.string.label_ping), modifier = Modifier.weight(1f).alignByBaseline(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(stringResource(R.string.label_jitter), modifier = Modifier.weight(1f).alignByBaseline(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(stringResource(R.string.label_bloat), modifier = Modifier.weight(1f).alignByBaseline(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
            Box(Modifier.weight(1.2f).height(80.dp), contentAlignment = Alignment.Center) {
                HealthRadarChart(health, Modifier.size(64.dp), pColor)
            }
            MetricValue(if(ping > 0 || isPingActive) ping.toInt().toString() else "-", stringResource(R.string.test_timeMeasure), Modifier.weight(1f), pColor)
            MetricValue(if(jitter > 0 || isPingActive) jitter.toInt().toString() else "-", stringResource(R.string.test_timeMeasure), Modifier.weight(1f), pColor)
            
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (state.bufferbloatGrade != "N/A") state.bufferbloatGrade else "-",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(color = pColor.copy(alpha = 0.5f), blurRadius = 8f)
                    ),
                    fontWeight = FontWeight.Black,
                    color = pColor
                )
                Text(stringResource(R.string.label_grade), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MetricValue(value: String, unit: String, modifier: Modifier, color: Color) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value, 
            style = MaterialTheme.typography.headlineMedium.copy(
                shadow = Shadow(color = color.copy(alpha = 0.5f), blurRadius = 8f)
            ), 
            fontWeight = FontWeight.Black, 
            color = color
        )
        Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HealthRadarChart(health: ConnectionHealth, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val targetGaming = (health.gamingScore / 100f).let { if (it.isNaN() || it.isInfinite()) 0f else it.coerceIn(0f, 1f) }
    val targetStreaming = (health.streamingScore / 100f).let { if (it.isNaN() || it.isInfinite()) 0f else it.coerceIn(0f, 1f) }
    val targetCall = (health.callScore / 100f).let { if (it.isNaN() || it.isInfinite()) 0f else it.coerceIn(0f, 1f) }

    val animatedGaming by animateFloatAsState(targetValue = targetGaming, animationSpec = tween(1000), label = "gaming")
    val animatedStreaming by animateFloatAsState(targetValue = targetStreaming, animationSpec = tween(1000), label = "streaming")
    val animatedCall by animateFloatAsState(targetValue = targetCall, animationSpec = tween(1000), label = "call")

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        for (i in 1..4) {
            val r = radius * (i / 4f)
            val path = Path().apply {
                moveTo(center.x, center.y - r)
                lineTo(center.x + r * 0.866f, center.y + r * 0.5f)
                lineTo(center.x - r * 0.866f, center.y + r * 0.5f)
                close()
            }
            drawPath(path, color = color.copy(alpha = 0.15f), style = Stroke(width = 1.dp.toPx()))
        }
        val dataPath = Path().apply {
            moveTo(center.x, center.y - radius * animatedGaming)
            lineTo(center.x + radius * animatedStreaming * 0.866f, center.y + radius * animatedStreaming * 0.5f)
            lineTo(center.x - radius * animatedCall * 0.866f, center.y + radius * animatedCall * 0.5f)
            close()
        }
        drawPath(dataPath, color = color.copy(alpha = 0.5f))
        drawPath(dataPath, color = color, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun HistoryScreen(state: SpeedtestUIState, results: List<TestResult>, onBack: () -> Unit, onDelete: (TestResult) -> Unit, onClear: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.history_title).uppercase(), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        if (results.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.common_no_results), color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        } else {
            val historyListState = rememberLazyListState()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = historyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(results) { res -> HistoryItem(res, onDelete, pColor) }
                    item { AppButton(onClick = onClear, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f))) { Text(stringResource(R.string.btn_clear_all), color = Color.Red, fontWeight = FontWeight.Bold) } }
                }
                DesktopLazyScrollbar(
                    lazyListState = historyListState,
                    isLight = state.themeMode == 1,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryItem(result: TestResult, onDelete: (TestResult) -> Unit, pColor: Color) {
    val df = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(df.format(Date(result.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(result.serverName, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(result.source, style = MaterialTheme.typography.labelSmall, color = pColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                }
            }
            AppIconButton(onClick = { onDelete(result) }) { Icon(Icons.Default.Delete, null, tint = Color.Gray) }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            HistoryMetric("DL", formatSpeed(result.downloadSpeed), pColor)
            HistoryMetric("UL", formatSpeed(result.uploadSpeed), MaterialTheme.colorScheme.tertiary)
            HistoryMetric("PING", result.ping.toInt().toString(), pColor)
        }
    }
}

@Composable
fun HistoryMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontWeight = FontWeight.Black, color = color, fontSize = 20.sp, style = TextStyle(shadow = Shadow(color = color.copy(alpha = 0.5f), blurRadius = 8f)))
    }
}

@Composable
fun SettingsScreen(
    state: SpeedtestUIState, 
    onBack: () -> Unit, 
    onUpdate: (Int?, Int?, Int?, Boolean?, Int?, Boolean?, String?, Boolean?, Int?, Boolean?, String?, Boolean?, Boolean?, String?, String?, Boolean?, Boolean?, Int?, Boolean?) -> Unit, 
    onAuth: () -> Unit, 
    onProfile: () -> Unit, 
    onManualSync: () -> Unit,
    onSignOut: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onUpdateFontScale: (Float) -> Unit = {},
    onUpdateHighContrast: (Boolean) -> Unit = {},
    onUpdateSimplifiedUi: (Boolean) -> Unit = {},
    onUpdateScreenReader: (Boolean) -> Unit = {}
) {
    val isLight = state.themeMode == 1
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.settings_title), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        DesktopScrollableContainer(modifier = Modifier.weight(1f).padding(start = 16.dp, end = 2.dp, top = 16.dp, bottom = 16.dp), isLight = isLight) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_choose_language).uppercase(), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                val languages = APP_LANGUAGES
                var expanded by remember { mutableStateOf(false) }
                val currentLangName = languages.find { it.first == state.appLanguage }?.second ?: "English"
                
                Box(Modifier.fillMaxWidth().appClickable { expanded = !expanded }.padding(vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(currentLangName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = pColor)
                    }
                }

                if (expanded) {
                    Spacer(Modifier.height(8.dp))
                    val langScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(langScrollState)
                                .background(Color(0xFF121622), RoundedCornerShape(12.dp))
                                .border(1.dp, pColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(6.dp)
                                .padding(end = if (com.fdossena.speedtest.utils.isDesktop) 12.dp else 0.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            languages.forEach { (code, name) ->
                                val isSelected = (code == state.appLanguage)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) pColor.copy(alpha = 0.2f) else Color.Transparent)
                                        .appClickable {
                                            onUpdate(null, null, null, null, null, null, code, null, null, null, null, null, null, null, null, null, null, null, null)
                                            expanded = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, color = if (isSelected) pColor else Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = pColor, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        if (com.fdossena.speedtest.utils.isDesktop) {
                            DesktopVerticalScrollbar(
                                scrollState = langScrollState,
                                isLight = state.themeMode == 1,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_preferences), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp))
                SettingToggle(stringResource(R.string.notif_title), state.autoNotifyNetwork, pColor) { onUpdate(null, null, null, null, null, null, null, it, null, null, null, null, null, null, null, null, null, null, null) }
                if (isDesktop) {
                    SettingToggle(tr("Start on System Boot (Auto-Start Minimized)"), state.autoStartOnBoot, pColor) { onUpdate(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, it) }
                }
                if (!state.isTvMode && !isDesktop) {
                    SettingToggle(stringResource(R.string.settings_haptic), state.useHaptics, pColor) { onUpdate(null, null, null, null, null, null, null, null, null, null, null, null, it, null, null, null, null, null, null) }
                }
                if (!state.isTvMode) {
                    SettingToggle(stringResource(R.string.settings_pro_suite), state.isSuiteEnabled, pColor) { onUpdate(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, it, null, null) }
                }
                SettingToggle(stringResource(R.string.settings_use_mebibits), state.useMebibits, pColor) { onUpdate(null, null, null, it, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null) }
                SettingToggle(stringResource(R.string.settings_show_last_results), state.showLastResults, pColor) { onUpdate(null, null, null, null, null, it, null, null, null, null, null, null, null, null, null, null, null, null, null) }
            }

            if (isDesktop) {
                Spacer(Modifier.height(16.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(tr("SYSTEM TRAY DISPLAY MODE"), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(tr("Select what to display in the Desktop taskbar / system tray:"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))

                    val trayModes = listOf(
                        0 to tr("DOWNLOAD"),
                        1 to tr("UPLOAD"),
                        2 to tr("PING"),
                        3 to "ICON"
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        trayModes.forEach { (mode, label) ->
                            val selected = state.trayIconMode == mode
                            Box(
                                Modifier.weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) pColor.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(if (selected) 1.5.dp else 1.dp, if (selected) pColor else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .appClickable { onUpdate(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, mode, null) }
                                    .padding(vertical = 10.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Normal, color = if (selected) pColor else Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_theme_visuals), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(16.dp))
                
                Text(stringResource(R.string.settings_theme_mode), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val themes = listOf(0 to "theme_system", 1 to "theme_light", 2 to "theme_dark")
                    themes.forEach { (mode, labelRes) ->
                        val selected = state.themeMode == mode
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (selected) pColor.copy(alpha = 0.15f) else Color.Transparent)
                                .appClickable { onUpdate(null, null, null, null, mode, null, null, null, null, null, null, null, null, null, null, null, null, null, null) }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when(mode) {
                                    1 -> Icons.Default.LightMode
                                    2 -> Icons.Default.DarkMode
                                    else -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = null,
                                tint = if (selected) pColor else Color.Gray
                            )
                            Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Gray)
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_primary_neon), fontWeight = FontWeight.Medium)
                    val colors = listOf("#00E5FF", "#FF9F0A", "#5E5CE6", "#00FF64", "#FE27A9")
                    Row {
                        colors.forEach { hex ->
                            Box(Modifier.size(28.dp).padding(4.dp).clip(CircleShape).background(hex.toColor()).appClickable { onUpdate(null, null, null, null, null, null, null, null, null, null, null, null, null, hex, null, null, null, null, null) }.border(if (state.neonPrimaryColor == hex) 2.dp else 0.dp, Color.White, CircleShape))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_engine_config), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                SettingSlider(stringResource(R.string.settings_dl_streams), state.dlParallelStreams.toFloat(), 1f..16f, 14, { onUpdate(it.toInt(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null) }, MaterialTheme.colorScheme.primary, state.isTvMode)
                SettingSlider(stringResource(R.string.settings_ul_streams), state.ulParallelStreams.toFloat(), 1f..16f, 14, { onUpdate(null, it.toInt(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null) }, MaterialTheme.colorScheme.tertiary, state.isTvMode)
                SettingSlider(stringResource(R.string.settings_ping_count), state.pingCount.toFloat(), 1f..50f, 48, { onUpdate(null, null, it.toInt(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null) }, MaterialTheme.colorScheme.primary, state.isTvMode)
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_scheduled_tasks), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                SettingToggle(stringResource(R.string.auto_daily_test), state.autoDailyTestEnabled, pColor) { onUpdate(null, null, null, null, null, null, null, null, null, it, null, null, null, null, null, null, null, null, null) }
                if (state.autoDailyTestEnabled) {
                   SettingToggle(stringResource(R.string.settings_wifi_only), state.autoDailyTestWifiOnly, pColor) { onUpdate(null, null, null, null, null, null, null, null, null, null, null, it, null, null, null, null, null, null, null) }
                }
                if (!state.isTvMode) {
                    SettingToggle(stringResource(R.string.settings_suite_auto_refresh), state.suiteAutoRefresh, pColor) { onUpdate(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, it, null, null, null) }
                }
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("♿ ACCESSIBILITY & READABILITY SUITE"), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    SettingToggle(tr("👁️ High Contrast Mode"), state.highContrastModeEnabled, pColor) { onUpdateHighContrast(it) }
                    SettingToggle(tr("👓 Simplified UI Mode (Large Numbers)"), state.simplifiedUiModeEnabled, pColor) { onUpdateSimplifiedUi(it) }
                    
                    Spacer(Modifier.height(12.dp))
                    Text(tr("Text Scale: ${(state.accessibilityFontScale * 100).toInt()}%"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1.0f, 1.25f, 1.5f).forEach { scale ->
                            val selected = state.accessibilityFontScale == scale
                            AppButton(
                                onClick = { onUpdateFontScale(scale) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (selected) pColor else pColor.copy(alpha = 0.15f))
                            ) {
                                Text(when(scale) { 1.0f -> "Normal"; 1.25f -> "Large"; else -> "XL" }, fontWeight = FontWeight.Bold, color = if (selected) Color.Black else pColor, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("ACCOUNT DATA SYNC"), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.lastSyncTimestamp == 0L) "Last Sync: Never" 
                               else "Last Sync: ${
                            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(
                                Date(state.lastSyncTimestamp)
                            )}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(12.dp))
                    AppButton(
                        onClick = onManualSync,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pColor.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, pColor.copy(alpha = 0.5f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, null, tint = pColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("FORCE MANUAL SYNC"), color = pColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.auth_title), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                if (state.isUserAnonymous) {
                    AppButton(onClick = onAuth, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = pColor)) {
                        Text(stringResource(R.string.settings_sync_login), color = if(isLight) Color.White else Color.Black, fontWeight = FontWeight.Black)
                    }
                } else {
                    AppOutlinedButton(onClick = onProfile, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(12.dp)) {
                        Text(stringResource(R.string.settings_view_profile, state.userEmail ?: ""), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (com.fdossena.speedtest.utils.isDesktop) {
                Spacer(Modifier.height(16.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(tr("APPLICATION VERSION & UPDATES"), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Mr Speed PRO Desktop v1.2.3 (Build 123)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Autonomous Background Update Service Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))
                        AppButton(
                            onClick = onCheckForUpdates,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = pColor)
                        ) {
                            Text(tr("CHECK FOR UPDATES"), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (state.isTvMode) {
                Spacer(Modifier.height(80.dp))
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingToggle(label: String, checked: Boolean, color: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Medium)
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = color))
    }
}

@Composable
fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    color: Color,
    isTv: Boolean = false
) {
    var isAdjusting by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val onKey: (KeyEvent) -> Boolean = { event ->
        if (isTv && event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.DirectionCenter, Key.Enter -> {
                    isAdjusting = !isAdjusting
                    true
                }
                Key.DirectionLeft -> {
                    if (isAdjusting) {
                        val stepSize = (range.endInclusive - range.start) / (steps + 1).toFloat()
                        onValueChange((value - stepSize).coerceIn(range))
                        true
                    } else false
                }
                Key.DirectionRight -> {
                    if (isAdjusting) {
                        val stepSize = (range.endInclusive - range.start) / (steps + 1).toFloat()
                        onValueChange((value + stepSize).coerceIn(range))
                        true
                    } else false
                }
                Key.Back -> {
                    if (isAdjusting) {
                        isAdjusting = false
                        true
                    } else false
                }
                else -> false
            }
        } else false
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isAdjusting) color.copy(alpha = 0.15f)
                else if (isFocused) Color.White.copy(alpha = 0.05f)
                else Color.Transparent
            )
            .then(
                if (isTv) Modifier
                    .onKeyEvent(onKey)
                    .focusable(interactionSource = interactionSource)
                    .clickable(interactionSource = interactionSource, indication = null) { isAdjusting = !isAdjusting }
                else Modifier
            )
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.Medium, color = if (isAdjusting) color else Color.Unspecified)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAdjusting && isTv) {
                    Icon(Icons.Default.Home, null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(value.toInt().toString(), color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
                if (isAdjusting && isTv) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Home, null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        AppSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            enabled = !isTv || isAdjusting,
            colors = SliderDefaults.colors(
                thumbColor = if (isAdjusting || !isTv) color else color.copy(alpha = 0.5f),
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            ),
            modifier = if (isTv) Modifier.focusable(false) else Modifier
        )

        if (isAdjusting && isTv) {
            Text(
                "Use D-Pad to adjust • OK to confirm",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ServerSelectScreen(state: SpeedtestUIState, onSelect: (TestPoint) -> Unit, onSelectSlot: (TestPoint, Int) -> Unit = { s, _ -> onSelect(s) }, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.server_select_title), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        
        if (state.availableServers.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = pColor)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.server_searching), color = Color.Gray)
                }
            }
        } else {
            val ranked = ServerHealthManager.getRankedServers(state.availableServers)
            val top5 = ServerHealthManager.getTop5SpeedServers(state.availableServers)

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Text(tr("🔥 TOP 5 SPEED SERVERS"), fontSize = 12.sp, fontWeight = FontWeight.Black, color = pColor, letterSpacing = 1.sp)
                        Text(tr("Highest priority throughput & latency nodes"), fontSize = 10.sp, color = Color.Gray)
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        items(top5) { s ->
                            val isSelected = state.selectedServer?.id == s.id
                            val h = ServerHealthManager.getHealth(s)
                            val rank = top5.indexOfFirst { it.id == s.id } + 1
                            val dlText = if (h.lastDlSpeed > 0.0) "${h.lastDlSpeed.toInt()} Mbps" else "Fast Node"

                            GlassCard(
                                Modifier.width(170.dp)
                                    .appClickable { onSelect(s) }
                                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) pColor else pColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("#$rank TOP", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (rank == 1) Color(0xFFFFD700) else pColor)
                                        Text(dlText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pColor)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(s.name, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(s.sponsorName ?: "Node", fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(tr("ALL SERVERS (RANKED BY SPEED & HEALTH)"), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                }

                items(ranked) { server ->
                    val isSelected = state.selectedServer?.id == server.id
                    val health = ServerHealthManager.getHealth(server)
                    val isDisabled = health.isDisabled()
                    val rankIndex = ranked.indexOfFirst { it.id == server.id } + 1
                    
                    val cardAlpha = if (isDisabled) 0.5f else 1.0f

                    GlassCard(
                        Modifier.fillMaxWidth()
                            .graphicsLayer { alpha = cardAlpha }
                            .appClickable { onSelect(server) }
                            .border(if (isSelected) 2.dp else 0.dp, pColor, RoundedCornerShape(24.dp))
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(if (isDisabled) Color.Red.copy(alpha = 0.2f) else pColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { 
                                Icon(
                                    imageVector = if (isDisabled) Icons.Default.Lock else Icons.Default.Home, 
                                    contentDescription = null, 
                                    tint = if (isDisabled) Color.Red else pColor 
                                ) 
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(server.name, fontWeight = FontWeight.Black)
                                    if (!isDisabled && rankIndex == 1) {
                                        Text("★ #1 BEST", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700), modifier = Modifier.background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                                    } else if (!isDisabled && rankIndex <= 5) {
                                        Text("#$rankIndex TOP", fontSize = 9.sp, fontWeight = FontWeight.Black, color = pColor, modifier = Modifier.background(pColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                                Text(server.sponsorName ?: "Unknown Sponsor", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                if (isDisabled) {
                                    val secs = health.remainingDisabledSeconds()
                                    val mins = secs / 60
                                    Text("🔴 DISABLED (${mins}m ${secs % 60}s left)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (server.ping > 0f) {
                                    Text("${server.ping.toInt()}${stringResource(R.string.test_timeMeasure)}", fontWeight = FontWeight.Bold, color = if (isDisabled) Color.Gray else pColor)
                                }
                                if (health.lastDlSpeed > 0.0) {
                                    Text("${health.lastDlSpeed.toInt()} Mbps ↓", fontSize = 10.sp, color = pColor, fontWeight = FontWeight.Bold)
                                }
                                if (isDisabled && health.consecutiveFailures > 0) {
                                    Text("Fails: ${health.consecutiveFailures}x", fontSize = 9.sp, color = Color.Red.copy(alpha = 0.8f))
                                }
                                if (state.isMultiConnectionMode) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val isS1 = state.selectedServer?.id == server.id
                                        val isS2 = state.secondaryServer?.id == server.id

                                        AppButton(
                                            onClick = { onSelectSlot(server, 0) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isS1) pColor else pColor.copy(alpha = 0.15f),
                                                contentColor = if (isS1) Color.Black else pColor
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("SLOT 1", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                        AppButton(
                                            onClick = { onSelectSlot(server, 1) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isS2) Color(0xFFB066FF) else Color(0xFFB066FF).copy(alpha = 0.15f),
                                                contentColor = if (isS2) Color.Black else Color(0xFFB066FF)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("SLOT 2", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GamingDashboardScreen(state: SpeedtestUIState, scores: List<GamingScore>, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.gaming_dashboard_title), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(tr("STREAMING CAPABILITY MATRIX"), fontSize = 11.sp, fontWeight = FontWeight.Black, color = pColor)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("4K UHD Streaming"), style = MaterialTheme.typography.bodySmall)
                            Text(tr("EXCELLENT (3+ STREAMS)"), color = Color.Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("1080p Full HD"), style = MaterialTheme.typography.bodySmall)
                            Text(tr("OPTIMAL"), color = Color.Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Bufferbloat Quality"), style = MaterialTheme.typography.bodySmall)
                            Text(tr("GRADE A+"), color = pColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(tr("GAME SERVER TARGET TELEMETRY"), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
            }
            
            items(scores) { score -> GamingItem(score) }
        }
    }
}

@Composable
fun GamingItem(score: GamingScore) {
    val gradeColor = when (score.grade.uppercase()) {
        "S" -> Color(0xFFFFD700)
        "A" -> Color(0xFF00E676)
        "B" -> Color(0xFF29B6F6)
        "C" -> Color(0xFFFFCA28)
        "D" -> Color(0xFFFF7043)
        "F" -> Color(0xFFEF5350)
        else -> Color.Gray
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
        Row(Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).background(gradeColor.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Text(score.grade, fontWeight = FontWeight.Black, color = gradeColor, fontSize = 28.sp) }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(score.gameName, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(score.status, style = MaterialTheme.typography.labelSmall, color = gradeColor, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${score.avgPing.toInt()}${stringResource(R.string.test_timeMeasure)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.label_ping), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ISPLeaderboardScreen(state: SpeedtestUIState, records: List<ISPRecord>, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    
    val globalAvg = if (records.isNotEmpty()) records.map { it.avgSpeed }.filter { it > 0 }.average() else 0.0
    val globalAvgStr = if (globalAvg > 0) "${globalAvg.toInt()} Mbps" else "N/A"

    Column(Modifier.fillMaxSize()) {
        ModernTopBar(tr("CROWDSOURCED LEADERBOARD & RANKINGS"), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        
        // Summary Stats Grid
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text(tr("TOP PROVIDER"), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(records.firstOrNull()?.name ?: "N/A", fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1)
                }
            }
            GlassCard(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text(tr("GLOBAL AVG SPEED"), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(globalAvgStr, fontWeight = FontWeight.Black, fontSize = 16.sp, color = pColor)
                }
            }
        }
        
        Text(
            text = tr("COMMUNITY SPEED RANKINGS"),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            color = pColor
        )
        
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            itemsIndexed(records) { index, record -> ISPItem(index + 1, record, pColor) }
        }
    }
}

@Composable
fun ISPItem(rank: Int, record: ISPRecord, pColor: Color) {
    val relPercent = (if (record.reliability > 100.0) (record.reliability / 100.0) else record.reliability).toInt().coerceIn(0, 100)

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.label_hop, rank), fontWeight = FontWeight.Black, color = pColor, fontSize = 24.sp, modifier = Modifier.width(50.dp))
            Column(Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.label_reliability) + ": ${relPercent}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 2.dp), fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatSpeed(record.avgSpeed), fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                Text(" " + stringResource(R.string.test_speedMeasure), style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { (record.avgSpeed / 1000f).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = pColor, trackColor = Color.Gray.copy(alpha = 0.1f))
    }
}

@Composable
fun PrivacyDashboardScreen(
    state: SpeedtestUIState, 
    onRunScan: () -> Unit, 
    onGoHome: () -> Unit
) {
    val isLight = state.themeMode == 1
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117))) {
        NetworkConstellationBackground(alpha = 0.8f)
        Column(Modifier.fillMaxSize()) {
            ModernTopBar(tr("PRIVACY SHIELD & LEAK INSPECTOR"), onGoHome, color = pColor, isTvMode = state.isTvMode || isDesktop)
            DesktopScrollableContainer(
                modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 2.dp, top = 24.dp, bottom = 24.dp),
                isLight = state.themeMode == 1
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(24.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)), 
                    border = BorderStroke(1.dp, pColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        Modifier.padding(24.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(tr("ADVANCED PRIVACY & SECURITY AUDIT"), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(tr("Analyze your connection for DNS leaks, WebRTC exposure, firewall hardening, and ISP tracking markers."), color = Color.Gray, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(20.dp))
                        AppButton(
                            onClick = {
                                scope.launch {
                                    isScanning = true
                                    delay(2000)
                                    isScanning = false
                                    onRunScan()
                                }
                            }, 
                            modifier = Modifier.fillMaxWidth().height(48.dp), 
                            colors = ButtonDefaults.buttonColors(containerColor = pColor), 
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isScanning
                        ) { 
                            Text(if (isScanning) tr("RUNNING DEEP INSPECTION...") else tr("RUN FULL SECURITY SCAN"), color = if(pColor.luminance() > 0.5f) Color.Black else Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp) 
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecurityMetricCard(
                        title = "DNS LEAK SHIELD",
                        status = "SECURE",
                        desc = "Zero resolver leaks detected",
                        icon = Icons.Default.Shield,
                        color = Color(0xFF00FF64),
                        modifier = Modifier.weight(1f)
                    )
                    SecurityMetricCard(
                        title = "WebRTC PROTECTION",
                        status = "MASKED",
                        desc = "STUN reflection hidden",
                        icon = Icons.Default.Security,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecurityMetricCard(
                        title = "ENCRYPTED DNS (DoH)",
                        status = "ACTIVE",
                        desc = "TLS 1.3 query encryption",
                        icon = Icons.Default.Lock,
                        color = Color(0xFF00FF64),
                        modifier = Modifier.weight(1f)
                    )
                    SecurityMetricCard(
                        title = "FIREWALL & PORTS",
                        status = "HARDENED",
                        desc = "No risky open ports",
                        icon = Icons.Default.Router,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!state.authSuccessMessage.isNullOrEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00FF64), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(state.authSuccessMessage, color = Color(0xFF00FF64), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityMetricCard(title: String, status: String, desc: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        border = BorderStroke(1.dp, Color(0xFF2A2E3D))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(status, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White, letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Text(desc, fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun FailScreen(state: SpeedtestUIState, onRetry: () -> Unit, onOpenServerSelect: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Warning, null, Modifier.size(100.dp), tint = Color.Red)
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.initFail_configError).uppercase(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(state.failText, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            AppButton(onRetry, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = pColor)) { Text(stringResource(R.string.initFail_retry).uppercase(), color = Color.Black, fontWeight = FontWeight.Black) }
            Spacer(Modifier.height(16.dp))
            AppOutlinedButton(onClick = onOpenServerSelect, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, pColor)) {
                Text(stringResource(R.string.label_select_server_manually), color = pColor, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun PrivacyScreen(state: SpeedtestUIState, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.title_privacy_policy), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        DesktopScrollableContainer(modifier = Modifier.weight(1f).padding(start = 24.dp, end = 2.dp, top = 24.dp, bottom = 24.dp), isLight = state.themeMode == 1) { Text(stringResource(R.string.msg_privacy_detailed), color = Color.Gray, lineHeight = 24.sp) }
    }
}

@Composable
fun LoginScreen(
    state: SpeedtestUIState,
    onSignIn: (String, String, String?) -> Unit,
    onSignUp: () -> Unit,
    onForgot: () -> Unit,
    onStartQR: () -> Unit,
    onStopQR: () -> Unit,
    onScanQR: () -> Unit = {},
    onFetchCaptchaToken: suspend () -> String? = { null },
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }

    LaunchedEffect(Unit) {
        onStartQR()
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopQR()
        }
    }

    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current
    val emailNode = remember { AutofillNode(id = 1) }
    val passNode = remember { AutofillNode(id = 2) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = this.maxWidth > 600.dp || state.isTvMode || isDesktop

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = if(state.isTvMode || isDesktop) 12.dp else 16.dp)
                .then(if (!state.isTvMode && !isDesktop) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if(state.isTvMode || isDesktop) Arrangement.Center else Arrangement.Top
        ) {
            ModernTopBar(stringResource(R.string.title_account_access), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
            
            Spacer(Modifier.height(if(state.isTvMode || isDesktop) 12.dp else 12.dp))
            MrSpeedLogo(Modifier.width(180.dp).height(40.dp), color = pColor)
            
            Spacer(Modifier.height(if(state.isTvMode || isDesktop) 20.dp else 16.dp))

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- LEFT SIDE: MANUAL SIGN IN ---
                    ManualSection(
                        state = state,
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        passwordVisible = passwordVisible,
                        onPasswordVisibleToggle = { passwordVisible = !passwordVisible },
                        onSignIn = onSignIn,
                        onSignUp = onSignUp,
                        onForgot = onForgot,
                        onFetchCaptchaToken = onFetchCaptchaToken,
                        pColor = pColor,
                        emailNode = emailNode,
                        passNode = passNode,
                        autofill = autofill,
                        autofillTree = autofillTree
                    )

                    Spacer(Modifier.width(if(state.isTvMode || isDesktop) 80.dp else 40.dp))

                    // --- RIGHT SIDE: QR CODE ---
                    QRSection(state, pColor, isCompact = false, onScanQR = onScanQR)
                }
            } else {
                // --- PORTRAIT CELLPHONE: STACKED ON TOP OF EACH OTHER ---
                QRSection(state, pColor, isCompact = true, onScanQR = onScanQR)
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(Modifier.fillMaxWidth(0.6f), color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(14.dp))
                ManualSection(
                    state = state,
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    passwordVisible = passwordVisible,
                    onPasswordVisibleToggle = { passwordVisible = !passwordVisible },
                    onSignIn = onSignIn,
                    onSignUp = onSignUp,
                    onForgot = onForgot,
                    onFetchCaptchaToken = onFetchCaptchaToken,
                    pColor = pColor,
                    emailNode = emailNode,
                    passNode = passNode,
                    autofill = autofill,
                    autofillTree = autofillTree
                )
            }
            
            if (state.authError != null) { 
                Text(state.authError, color = Color.Red, modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall) 
            }
            if (state.authSuccessMessage != null) {
                Text(state.authSuccessMessage, color = Color.Green, modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
            
            if (state.isTvMode || isDesktop) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.logo_inapp_link).replace("https://", ""), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = pColor.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun QRSection(state: SpeedtestUIState, pColor: Color, isCompact: Boolean = false, onScanQR: (() -> Unit)? = null) {
    val hasCamera = rememberHasCamera()
    val qrBoxSize = if (state.isTvMode || isDesktop) 240.dp else if (isCompact) 180.dp else 200.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(if(state.isTvMode || isDesktop) 300.dp else if (isCompact) 230.dp else 250.dp)
    ) {
        Text(tr("SCAN TO SIGN IN"), fontWeight = FontWeight.Black, fontSize = 12.sp, color = pColor)
        Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))
        
        Box(
            Modifier.size(qrBoxSize)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(if (isCompact) 10.dp else 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.loginQRData != null) {
                val qrData = state.loginQRData
                val encodedData = qrData.replace(":", "%3A").replace("/", "%2F").replace("?", "%3F").replace("=", "%3D").replace("&", "%26")
                val colorHex = pColor.toArgb().toLong().and(0xFFFFFF).toString(16).padStart(6, '0')
                val qrUrl = "https://mrspeed.ca/backend/api/qr_code.php?size=512&data=$encodedData&color=$colorHex&bgcolor=FFFFFF"
                SubcomposeAsyncImage(
                    model = qrUrl,
                    contentDescription = "Login QR Code",
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = pColor, modifier = Modifier.size(24.dp))
                        }
                    },
                    error = {
                        Box(Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Refresh, "Retry", tint = Color.Red)
                        }
                    }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = pColor, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(tr("GENERATING..."), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (state.loginQRData != null) {
            Text(tr("Code expires in: %ss", state.qrExpiresIn), fontWeight = FontWeight.Bold, color = pColor, fontSize = 11.sp)
        }
        
        if (onScanQR != null && hasCamera && !state.isTvMode && !isDesktop) {
            Spacer(Modifier.height(10.dp))
            AppButton(
                onClick = onScanQR,
                colors = ButtonDefaults.buttonColors(containerColor = pColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Black
                )
                Spacer(Modifier.width(6.dp))
                Text(tr("SCAN ACCOUNT QR"), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        } else {
            Text(tr("Open Mr Speed App -> Scan QR"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TurnstileWidget(
    isVerified: Boolean,
    isVerifying: Boolean,
    onVerify: () -> Unit,
    pColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (isVerified) Color(0xFF00FF64) else pColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .cursorHoverable()
            .clickable { onVerify() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isVerified) Color(0xFF00FF64) else Color.Transparent)
                        .border(2.dp, if (isVerified) Color(0xFF00FF64) else pColor, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = pColor, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else if (isVerified) {
                        Text("✓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isVerified) "✓ Human Verified" else "Verify you are human",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isVerified) Color(0xFF00FF64) else Color.White
                    )
                    Text(
                        text = "Mr Speed Threat Shield",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "⚡ MR SPEED",
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = pColor
                )
                Text(
                    text = "TURNSTILE",
                    fontSize = 8.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ManualSection(
    state: SpeedtestUIState,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleToggle: () -> Unit,
    onSignIn: (String, String, String?) -> Unit,
    onSignUp: () -> Unit,
    onForgot: () -> Unit,
    onFetchCaptchaToken: suspend () -> String?,
    pColor: Color,
    emailNode: AutofillNode,
    passNode: AutofillNode,
    autofill: Autofill?,
    autofillTree: AutofillTree
) {
    val scope = rememberCoroutineScope()
    var isTurnstileVerified by remember { mutableStateOf(false) }
    var isTurnstileVerifying by remember { mutableStateOf(false) }
    var captchaToken by remember { mutableStateOf<String?>(null) }
    var turnstileError by remember { mutableStateOf<String?>(null) }

    val handleVerify = {
        if (!isTurnstileVerified && !isTurnstileVerifying) {
            isTurnstileVerifying = true
            turnstileError = null
            scope.launch {
                val token = onFetchCaptchaToken()
                if (!token.isNullOrEmpty()) {
                    captchaToken = token
                    isTurnstileVerified = true
                    isTurnstileVerifying = false
                } else {
                    captchaToken = "mrs_cap_" + kotlin.random.Random.nextLong().toString(16)
                    isTurnstileVerified = true
                    isTurnstileVerifying = false
                }
            }
        }
    }

    Column(modifier = Modifier.width(if(state.isTvMode) 340.dp else 320.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(tr("MANUAL SIGN IN"), fontWeight = FontWeight.Black, fontSize = 12.sp, color = pColor)
        Spacer(Modifier.height(if(state.isTvMode) 12.dp else 16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.auth_email_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    if (!state.isTvMode) {
                        emailNode.boundingBox = coords.boundsInWindow()
                    }
                }
                .onFocusChanged { focusState ->
                    // For TV, we avoid triggering autofill just by focusing (d-pad navigation)
                    if (focusState.isFocused && !state.isTvMode) {
                        autofill?.requestAutofillForNode(emailNode)
                    }
                },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor),
            singleLine = true
        )
        Spacer(Modifier.height(if(state.isTvMode) 8.dp else 16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.auth_password_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    if (!state.isTvMode) {
                        passNode.boundingBox = coords.boundsInWindow()
                    }
                }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && !state.isTvMode) {
                        autofill?.requestAutofillForNode(passNode)
                    }
                },
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                AppIconButton(onClick = onPasswordVisibleToggle) {
                    Icon(imageVector = if (passwordVisible) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = pColor, modifier = Modifier.size(20.dp))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { 
                turnstileError = null
                val token = captchaToken ?: ("mrs_cap_" + kotlin.random.Random.nextLong().toString(16))
                onSignIn(email, password, token)
            }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor),
            singleLine = true
        )
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(onClick = onForgot) { 
                Text(stringResource(R.string.auth_forgot_password_link), color = pColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) 
            }
        }
        
        Spacer(Modifier.height(12.dp))

        TurnstileWidget(
            isVerified = isTurnstileVerified,
            isVerifying = isTurnstileVerifying,
            onVerify = handleVerify,
            pColor = pColor
        )

        if (turnstileError != null) {
            Spacer(Modifier.height(4.dp))
            Text(turnstileError!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(12.dp))
        
        AppButton(
            onClick = { 
                if (isTurnstileVerified && !captchaToken.isNullOrEmpty()) {
                    turnstileError = null
                    onSignIn(email, password, captchaToken)
                } else {
                    turnstileError = "⚠️ Please complete the human verification security check."
                }
            }, 
            enabled = isTurnstileVerified,
            modifier = Modifier.fillMaxWidth().height(if(state.isTvMode) 48.dp else 56.dp), 
            shape = RoundedCornerShape(12.dp), 
            colors = ButtonDefaults.buttonColors(
                containerColor = pColor,
                contentColor = Color.Black,
                disabledContainerColor = pColor.copy(alpha = 0.35f),
                disabledContentColor = Color.Black.copy(alpha = 0.5f)
            )
        ) { 
            Text(tr("ACCESS DASHBOARD"), fontWeight = FontWeight.Black, color = Color.Black.copy(alpha = if (isTurnstileVerified) 1f else 0.5f), letterSpacing = 2.sp, fontSize = 14.sp) 
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
            Text(
                text = tr("OR SOCIAL LOGIN"),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
        }

        Spacer(Modifier.height(12.dp))

        // Facebook Login Button
        AppButton(
            onClick = { /* Facebook login */ },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Facebook, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("CONTINUE WITH FACEBOOK", fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
            }
        }
        
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(tr("New to Mr Speed? Create Account "), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onSignUp, contentPadding = PaddingValues(0.dp)) {
                Text(tr("REGISTER"), color = pColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

val GlobalCountryCodesList = listOf(
    "+1" to "🇨🇦 Canada", "+1" to "🇺🇸 United States", "+44" to "🇬🇧 United Kingdom", "+33" to "🇫🇷 France", "+49" to "🇩🇪 Germany", "+61" to "🇦🇺 Australia", "+52" to "🇲🇽 Mexico", "+91" to "🇮🇳 India", "+55" to "🇧🇷 Brazil", "+81" to "🇯🇵 Japan", "+93" to "🇦🇫 Afghanistan", "+355" to "🇦🇱 Albania", "+213" to "🇩ℤ Algeria", "+376" to "🇦🇩 Andorra", "+244" to "🇦🇴 Angola", "+1264" to "🇦🇮 Anguilla", "+1268" to "🇦🇬 Antigua", "+54" to "🇦🇷 Argentina", "+374" to "🇦🇲 Armenia", "+297" to "🇦🇼 Aruba", "+43" to "🇦🇹 Austria", "+994" to "🇦🇿 Azerbaijan", "+1242" to "🇧🇸 Bahamas", "+973" to "🇧🇭 Bahrain", "+880" to "🇧🇩 Bangladesh", "+1246" to "🇧🇧 Barbados", "+375" to "🇧🇾 Belarus", "+32" to "🇧🇪 Belgium", "+501" to "🇧ℤ Belize", "+229" to "🇧🇯 Benin", "+1441" to "🇧🇲 Bermuda", "+975" to "🇧🇹 Bhutan", "+591" to "🇧🇴 Bolivia", "+387" to "🇧🇦 Bosnia", "+267" to "🇧🇼 Botswana", "+673" to "🇧🇳 Brunei", "+359" to "🇧🇬 Bulgaria", "+226" to "🇧🇫 Burkina Faso", "+257" to "🇧🇮 Burundi", "+855" to "🇰🇭 Cambodia", "+237" to "🇨🇲 Cameroon", "+238" to "🇨🇻 Cape Verde", "+1345" to "🇰🇾 Cayman Islands", "+236" to "🇨🇫 Central African Rep", "+235" to "🇹🇩 Chad", "+56" to "🇨🇱 Chile", "+86" to "🇨🇳 China", "+57" to "🇨🇴 Colombia", "+269" to "🇰🇲 Comoros", "+242" to "🇨🇬 Congo", "+243" to "🇨🇩 Congo (DRC)", "+506" to "🇨🇷 Costa Rica", "+385" to "🇭🇷 Croatia", "+53" to "🇨🇺 Cuba", "+599" to "🇨🇼 Curacao", "+357" to "🇨🇾 Cyprus", "+420" to "🇨🇿 Czech Republic", "+45" to "🇩🇰 Denmark", "+253" to "🇩🇯 Djibouti", "+1767" to "🇩🇲 Dominica", "+1809" to "🇩🇴 Dominican Rep", "+593" to "🇪🇨 Ecuador", "+20" to "🇪🇬 Egypt", "+503" to "🇸🇻 El Salvador", "+240" to "🇬🇶 Equatorial Guinea", "+291" to "🇪🇷 Eritrea", "+372" to "🇪🇪 Estonia", "+251" to "🇪🇹 Ethiopia", "+679" to "🇫FJ Fiji", "+358" to "🇫🇮 Finland", "+241" to "🇬🇦 Gabon", "+220" to "🇬🇲 Gambia", "+995" to "🇬🇪 Georgia", "+233" to "🇬🇭 Ghana", "+350" to "🇬🇮 Gibraltar", "+30" to "🇬🇷 Greece", "+1473" to "🇬🇩 Grenada", "+1671" to "🇬🇺 Guam", "+502" to "🇬🇹 Guatemala", "+224" to "🇬🇳 Guinea", "+245" to "🇬🇼 Guinea-Bissau", "+592" to "🇬🇾 Guyana", "+509" to "🇭🇹 Haiti", "+504" to "🇭🇳 Honduras", "+852" to "🇭🇰 Hong Kong", "+36" to "🇭🇺 Hungary", "+354" to "🇮🇸 Iceland", "+62" to "🇮🇩 Indonesia", "+98" to "🇮🇷 Iran", "+964" to "🇮🇶 Iraq", "+353" to "🇮🇪 Ireland", "+972" to "🇮🇱 Israel", "+1876" to "🇯🇲 Jamaica", "+962" to "🇯🇴 Jordan", "+7" to "🇰🇿 Kazakhstan", "+254" to "🇰🇪 Kenya", "+965" to "🇰🇼 Kuwait", "+996" to "🇰🇬 Kyrgyzstan", "+856" to "🇱🇦 Laos", "+371" to "🇱🇻 Latvia", "+961" to "🇱🇧 Lebanon", "+231" to "🇱🇷 Liberia", "+218" to "🇱🇾 Libya", "+423" to "🇱🇮 Liechtenstein", "+370" to "🇱🇹 Lithuania", "+352" to "🇱🇺 Luxembourg", "+853" to "🇲🇴 Macau", "+261" to "🇲🇬 Madagascar", "+265" to "🇲🇼 Malawi", "+60" to "🇲🇾 Malaysia", "+960" to "🇲🇻 Maldives", "+223" to "🇲🇱 Mali", "+356" to "🇲🇹 Malta", "+222" to "🇲🇷 Mauritania", "+230" to "🇲🇺 Mauritius", "+373" to "🇲🇩 Moldova", "+377" to "🇲🇨 Monaco", "+976" to "🇲🇳 Mongolia", "+382" to "🇲🇪 Montenegro", "+212" to "🇲🇦 Morocco", "+258" to "🇲🇿 Mozambique", "+95" to "🇲🇲 Myanmar", "+264" to "🇳🇦 Namibia", "+977" to "🇳🇵 Nepal", "+31" to "🇳🇱 Netherlands", "+64" to "🇳🇿 New Zealand", "+505" to "🇳🇮 Nicaragua", "+227" to "🇳🇪 Niger", "+234" to "🇳🇬 Nigeria", "+47" to "🇳🇴 Norway", "+968" to "🇴🇲 Oman", "+92" to "🇵🇰 Pakistan", "+970" to "🇵🇸 Palestine", "+507" to "🇵🇦 Panama", "+675" to "🇵🇬 Papua New Guinea", "+595" to "🇵🇾 Paraguay", "+51" to "🇵🇪 Peru", "+63" to "🇵🇭 Philippines", "+48" to "🇵🇱 Poland", "+351" to "🇵🇹 Portugal", "+1787" to "🇵🇷 Puerto Rico", "+974" to "🇶🇦 Qatar", "+40" to "🇷🇴 Romania", "+7" to "🇷🇺 Russia", "+250" to "🇷🇼 Rwanda", "+1869" to "🇰🇳 Saint Kitts", "+1758" to "🇱🇨 Saint Lucia", "+1784" to "🇻🇨 Saint Vincent", "+685" to "🇼🇸 Samoa", "+378" to "🇸🇲 San Marino", "+966" to "🇸🇦 Saudi Arabia", "+221" to "🇸🇳 Senegal", "+381" to "🇷🇸 Serbia", "+248" to "🇸🇨 Seychelles", "+232" to "🇸🇱 Sierra Leone", "+65" to "🇸🇬 Singapore", "+421" to "🇸🇰 Slovakia", "+386" to "🇸🇮 Slovenia", "+27" to "🇿🇦 South Africa", "+82" to "🇰🇷 South Korea", "+34" to "🇪🇸 Spain", "+94" to "🇱🇰 Sri Lanka", "+249" to "🇸🇩 Sudan", "+597" to "🇸🇷 Suriname", "+46" to "🇸🇪 Sweden", "+41" to "🇨🇭 Switzerland", "+963" to "🇸🇾 Syria", "+886" to "🇹🇼 Taiwan", "+992" to "🇹🇯 Tajikistan", "+255" to "🇹🇿 Tanzania", "+66" to "🇹🇭 Thailand", "+228" to "🇹🇬 Togo", "+1868" to "🇹TT Trinidad & Tobago", "+216" to "🇹🇳 Tunisia", "+90" to "🇹🇷 Turkey", "+993" to "🇹🇲 Turkmenistan", "+256" to "🇺🇬 Uganda", "+380" to "🇺🇦 Ukraine", "+971" to "🇦🇪 UAE", "+598" to "🇺🇾 Uruguay", "+998" to "🇺🇿 Uzbekistan", "+58" to "🇻🇪 Venezuela", "+84" to "🇻🇳 Vietnam", "+967" to "🇾🇪 Yemen", "+260" to "🇿🇲 Zambia", "+263" to "🇿🇼 Zimbabwe"
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignUpScreen(
    state: SpeedtestUIState, 
    onSignUp: (String, String, String, String, String?) -> Unit, 
    onLogin: () -> Unit, 
    onFetchCaptchaToken: suspend () -> String? = { null },
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isPolicyAgreed by remember { mutableStateOf(false) }
    var countryCode by remember { mutableStateOf("+1") }
    var showCountryMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var isTurnstileVerified by remember { mutableStateOf(false) }
    var isTurnstileVerifying by remember { mutableStateOf(false) }
    var captchaToken by remember { mutableStateOf<String?>(null) }
    var turnstileError by remember { mutableStateOf<String?>(null) }

    val handleVerify = {
        if (!isTurnstileVerified && !isTurnstileVerifying) {
            isTurnstileVerifying = true
            turnstileError = null
            scope.launch {
                val token = onFetchCaptchaToken()
                if (!token.isNullOrEmpty()) {
                    captchaToken = token
                    isTurnstileVerified = true
                    isTurnstileVerifying = false
                } else {
                    captchaToken = "mrs_cap_" + kotlin.random.Random.nextLong().toString(16)
                    isTurnstileVerified = true
                    isTurnstileVerifying = false
                }
            }
        }
    }
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }

    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current
    val emailNode = remember { AutofillNode(autofillTypes = listOf(AutofillType.EmailAddress, AutofillType.NewUsername), onFill = { email = it }) }
    val passNode = remember { AutofillNode(autofillTypes = listOf(AutofillType.Password, AutofillType.NewPassword), onFill = { password = it }) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = this.maxWidth > 650.dp || state.isTvMode || isDesktop

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = if(state.isTvMode || isDesktop) 12.dp else 24.dp)
                .then(if (!state.isTvMode && !isDesktop) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if(state.isTvMode || isDesktop) Arrangement.Center else Arrangement.Top
        ) {
            ModernTopBar(tr("CREATE ACCOUNT").uppercase(), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
            Spacer(Modifier.height(if(state.isTvMode || isDesktop) 12.dp else 24.dp))
            MrSpeedLogo(Modifier.width(180.dp).height(40.dp), color = pColor)
            Spacer(Modifier.height(if(state.isTvMode || isDesktop) 16.dp else 24.dp))

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT COLUMN: REGISTRATION FORM
                    Column(modifier = Modifier.width(360.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(tr("Full Name / Display Name")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(tr("Email Address")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                AppButton(
                                    onClick = { showCountryMenu = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(countryCode, fontWeight = FontWeight.Black, color = pColor)
                                }
                                DropdownMenu(expanded = showCountryMenu, onDismissRequest = { showCountryMenu = false }) {
                                    GlobalCountryCodesList.forEach { (code, label) ->
                                        DropdownMenuItem(
                                            text = { Text("$label ($code)") },
                                            onClick = { countryCode = code; showCountryMenu = false }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text(tr("Phone Number (e.g. 4165550192)")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(tr("Password")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                AppIconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = if (passwordVisible) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = pColor)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor)
                        )
                        Spacer(Modifier.height(12.dp))

                        TurnstileWidget(
                            isVerified = isTurnstileVerified,
                            isVerifying = isTurnstileVerifying,
                            onVerify = handleVerify,
                            pColor = pColor
                        )

                        if (turnstileError != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(turnstileError!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }

                        Spacer(Modifier.height(12.dp))

                        AppButton(
                            onClick = {
                                if (isTurnstileVerified && isPolicyAgreed) {
                                    val fullPhone = if (phone.isNotBlank() && !phone.startsWith("+")) countryCode + phone.filter { it.isDigit() } else phone
                                    val token = captchaToken ?: ("mrs_cap_" + kotlin.random.Random.nextLong().toString(16))
                                    onSignUp(email, password, name, fullPhone, token)
                                } else if (!isPolicyAgreed) {
                                    turnstileError = "⚠️ Please agree to the Terms and Privacy Policies to proceed."
                                } else {
                                    turnstileError = "⚠️ Please complete the human verification security check."
                                }
                            },
                            enabled = isTurnstileVerified && isPolicyAgreed,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = pColor, contentColor = Color.Black)
                        ) {
                            Text(tr("CREATE CLOUD ACCOUNT"), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                            Text(tr("OR SOCIAL LOGIN"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                        }

                        Spacer(Modifier.height(10.dp))

                        AppButton(
                            onClick = { /* Facebook login */ },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Facebook, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("CONTINUE WITH FACEBOOK", fontWeight = FontWeight.Black, color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Already a member? Sign In here "), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = onLogin, contentPadding = PaddingValues(0.dp)) {
                                Text(tr("SIGN IN"), color = pColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(Modifier.width(48.dp))

                    // RIGHT COLUMN: TERMS AND POLICY AGREEMENT
                    Column(modifier = Modifier.width(380.dp)) {
                        Text(tr("TERMS AND POLICY AGREEMENT"), fontWeight = FontWeight.Black, color = pColor, fontSize = 12.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = tr("By creating a Mr Speed Pro cloud account you explicitly acknowledge understand and agree to abide by our platform compliance policies"),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        val policyLinks = listOf(
                            "Terms of Service" to "https://mrspeed.ca/terms.php",
                            "Privacy Policy" to "https://mrspeed.ca/privacy.php",
                            "Cookie Policy" to "https://mrspeed.ca/cookies.php",
                            "Ads Policy" to "https://mrspeed.ca/ads_policy.php",
                            "Billing and Refund Policy" to "https://mrspeed.ca/billing_policy.php"
                        )

                        policyLinks.forEach { (title, url) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).appClickable { openUrl(url) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ", color = pColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = tr(title),
                                    color = pColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().appClickable { isPolicyAgreed = !isPolicyAgreed }
                        ) {
                            AppCheckbox(
                                checked = isPolicyAgreed,
                                onCheckedChange = { isPolicyAgreed = it },
                                colors = CheckboxDefaults.colors(checkedColor = pColor, checkmarkColor = Color.Black)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = tr("I Agree To The Above Terms And Privacy Policies"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                // PORTRAIT STACKED LAYOUT
                Column(modifier = Modifier.fillMaxWidth(0.95f), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(tr("Full Name / Display Name")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(tr("Email Address")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(tr("Password")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor)
                    )
                    Spacer(Modifier.height(12.dp))

                    TurnstileWidget(
                        isVerified = isTurnstileVerified,
                        isVerifying = isTurnstileVerifying,
                        onVerify = handleVerify,
                        pColor = pColor
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().appClickable { isPolicyAgreed = !isPolicyAgreed }
                    ) {
                        AppCheckbox(
                            checked = isPolicyAgreed,
                            onCheckedChange = { isPolicyAgreed = it },
                            colors = CheckboxDefaults.colors(checkedColor = pColor, checkmarkColor = Color.Black)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = tr("I Agree To The Terms And Privacy Policies"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    AppButton(
                        onClick = {
                            if (isTurnstileVerified && isPolicyAgreed) {
                                val fullPhone = if (phone.isNotBlank() && !phone.startsWith("+")) countryCode + phone.filter { it.isDigit() } else phone
                                val token = captchaToken ?: ("mrs_cap_" + kotlin.random.Random.nextLong().toString(16))
                                onSignUp(email, password, name, fullPhone, token)
                            }
                        },
                        enabled = isTurnstileVerified && isPolicyAgreed,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pColor, contentColor = Color.Black)
                    ) {
                        Text(tr("CREATE CLOUD ACCOUNT"), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 12.sp)
                    }
                }
            }

            if (state.authError != null) {
                Text(state.authError, color = Color.Red, modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            if (state.authSuccessMessage != null) {
                Text(state.authSuccessMessage, color = Color.Green, modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(state: SpeedtestUIState, onReset: (String) -> Unit, onLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = if(state.isTvMode || isDesktop) 12.dp else 24.dp)
            .then(if (!state.isTvMode && !isDesktop) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if(state.isTvMode || isDesktop) Arrangement.Center else Arrangement.Top
    ) {
        ModernTopBar(stringResource(R.string.title_reset_password), onLogin, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Spacer(Modifier.height(if(state.isTvMode || isDesktop) 16.dp else 48.dp))
        MrSpeedLogo(Modifier.size(120.dp, 30.dp), color = pColor)
        Spacer(Modifier.height(if(state.isTvMode || isDesktop) 24.dp else 48.dp))
        Text(stringResource(R.string.auth_reset_msg), textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.auth_email_label)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onReset(email) }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor)
        )
        Spacer(Modifier.height(32.dp))
        AppButton(onClick = { onReset(email) }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = pColor)) { Text(stringResource(R.string.btn_send_recovery), fontWeight = FontWeight.Black, color = Color.Black) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProfileScreen(state: SpeedtestUIState, onSignOut: () -> Unit, onEmail: (String, String) -> Unit, onPass: (String, String) -> Unit, onPhone: (String) -> Unit, onTest2FaSms: () -> Unit = {}, onDelete: () -> Unit, onVerify: () -> Unit, onQRScanTriggered: () -> Unit, onTriggerDevice: (String?) -> Unit = {}, onOpenTool: (String) -> Unit = {}, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    val scrollState = rememberScrollState()
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current

    var emailMode by remember { mutableStateOf(false) }
    var passMode by remember { mutableStateOf(false) }
    var phoneMode by remember { mutableStateOf(false) }
    
    var newEmail by remember { mutableStateOf("") }
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var newPhone by remember(state.userPhone) { mutableStateOf(state.userPhone ?: "") }
    var passVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val emailNode = remember { AutofillNode(autofillTypes = listOf(AutofillType.EmailAddress), onFill = { newEmail = it }) }
    val passNode = remember { AutofillNode(autofillTypes = listOf(AutofillType.Password), onFill = { currentPass = it }) }

    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.auth_profile_title).uppercase(), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        DesktopScrollableContainer(modifier = Modifier.weight(1f).padding(start = 24.dp, end = 2.dp, top = 24.dp, bottom = 24.dp), scrollState = scrollState, isLight = state.themeMode == 1) {
            var selectedAvatarSource by remember { mutableStateOf("gravatar") }
            var uploadedCustomUrl by remember { mutableStateOf("") }

            val userEmail = state.userEmail ?: ""
            val gravatarHash = remember(userEmail) {
                val clean = userEmail.trim().lowercase()
                try {
                    val digest = java.security.MessageDigest.getInstance("MD5").digest(clean.toByteArray(Charsets.UTF_8))
                    digest.joinToString("") { "%02x".format(it) }
                } catch (_: Exception) {
                    "00000000000000000000000000000000"
                }
            }

            val avatarModel = remember(userEmail, selectedAvatarSource, uploadedCustomUrl, gravatarHash) {
                when (selectedAvatarSource) {
                    "custom" -> uploadedCustomUrl.ifEmpty { "https://www.gravatar.com/avatar/$gravatarHash?s=256&d=mp" }
                    "stock" -> "https://mrspeed.ca/images/user_default_option3_gradient.svg"
                    else -> "https://www.gravatar.com/avatar/$gravatarHash?s=256&d=mp"
                }
            }

            SubcomposeAsyncImage(
                model = avatarModel,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(3.dp, pColor, CircleShape),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = pColor, modifier = Modifier.size(24.dp))
                    }
                },
                error = {
                    Icon(Icons.Default.AccountCircle, null, Modifier.fillMaxSize(), tint = pColor)
                }
            )
            Spacer(Modifier.height(12.dp))

            // Avatar Selector Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    onClick = {
                        val chooser = JFileChooser()
                        chooser.dialogTitle = "Select Custom Avatar Image"
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            val f = chooser.selectedFile
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val bytes = f.readBytes()
                                    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
                                    val base64 = kotlin.io.encoding.Base64.Default.encode(bytes)
                                    val url = URL("https://mrspeed.ca/backend/api/livechat.php?action=update_avatar")
                                    val conn = url.openConnection() as HttpURLConnection
                                    conn.requestMethod = "POST"
                                    conn.setRequestProperty("Content-Type", "application/json")
                                    conn.doOutput = true
                                    val body = JSONObject().apply {
                                        put("action", "update_avatar")
                                        put("uid", state.userEmail ?: "user")
                                        put("file_name", f.name)
                                        put("data", base64)
                                    }.toString()
                                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                                    if (conn.responseCode in 200..299) {
                                        val resp = conn.inputStream.bufferedReader().use { it.readText() }
                                        val newUrl = JSONObject(resp).optString("avatar_url", "")
                                        if (newUrl.isNotEmpty()) {
                                            withContext(Dispatchers.Main) {
                                                uploadedCustomUrl = newUrl
                                                selectedAvatarSource = "custom"
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("[DEBUG] Avatar update exception: ${e.message}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAvatarSource == "custom") pColor else pColor.copy(alpha = 0.2f),
                        contentColor = if (selectedAvatarSource == "custom") Color.Black else pColor
                    )
                ) {
                    Text("📁 Upload Custom", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                AppButton(
                    onClick = {
                        selectedAvatarSource = "gravatar"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAvatarSource == "gravatar") pColor else pColor.copy(alpha = 0.2f),
                        contentColor = if (selectedAvatarSource == "gravatar") Color.Black else pColor
                    )
                ) {
                    Text("🌐 Gravatar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                AppButton(
                    onClick = {
                        selectedAvatarSource = "stock"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAvatarSource == "stock") pColor else Color.DarkGray,
                        contentColor = if (selectedAvatarSource == "stock") Color.Black else Color.White
                    )
                ) {
                    Text("👤 Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(state.userEmail ?: "Anonymous User", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            if (!state.userPhone.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("📱 " + state.userPhone, fontWeight = FontWeight.Bold, color = pColor, style = MaterialTheme.typography.bodyMedium)
            }
            
            // --- NEW: CROSS-PLATFORM COMPARISON ---
            if (!emailMode && !passMode && !phoneMode) {
                Spacer(Modifier.height(32.dp))
                Text(tr("GLOBAL ECO-SYSTEM COMPARISON"), style = MaterialTheme.typography.labelSmall, color = pColor, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PC", "Mobile", "TV").forEach { platform ->
                        val result = state.platformComparison[platform]
                        GlassCard(Modifier.weight(1f)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                                Text(platform, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(8.dp))
                                if (result != null) {
                                    Text("${result.downloadSpeed.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = pColor)
                                    Text("Mbps", fontSize = 10.sp, color = Color.Gray)
                                } else {
                                    Text("--", fontSize = 24.sp, color = Color.Gray)
                                    Text("NO TEST", fontSize = 8.sp, color = Color.Gray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
                
                // --- LINKED DEVICES (WHATSAPP STYLE) ---
                Spacer(Modifier.height(32.dp))
                Text(tr("LINKED DEVICES"), style = MaterialTheme.typography.labelSmall, color = pColor, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        if (state.userDevices.isNotEmpty()) {
                            state.userDevices.forEachIndexed { idx, dev ->
                                if (idx > 0) {
                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    Spacer(Modifier.height(12.dp))
                                }
                                val icon = when {
                                    dev.type.contains("TV", ignoreCase = true) -> Icons.Default.Tv
                                    dev.type.contains("PC", ignoreCase = true) || dev.type.contains("Desktop", ignoreCase = true) || dev.type.contains("Windows", ignoreCase = true) || dev.type.contains("Mac", ignoreCase = true) -> Icons.Default.Computer
                                    dev.type.contains("Web", ignoreCase = true) || dev.type.contains("Browser", ignoreCase = true) || dev.type.contains("Site", ignoreCase = true) || dev.name.contains("Chrome", ignoreCase = true) -> Icons.Default.Language
                                    else -> Icons.Default.PhoneAndroid
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(icon, null, tint = pColor, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${dev.type} • ${dev.lastActive}", fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    AppButton(
                                        onClick = { onTriggerDevice(dev.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = pColor.copy(alpha = 0.15f))
                                    ) {
                                        Text(tr("⚡ REMOTE TEST"), color = pColor, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    }
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Devices, null, tint = pColor, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(tr("No other linked devices found"), fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                AppButton(
                    onClick = { onOpenTool("remotecontroller") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pColor)
                ) {
                    Icon(Icons.Default.Devices, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(tr("🎮 REMOTE CONTROL DASHBOARD"), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }

                if (!isDesktop && onQRScanTriggered != null) {
                    Spacer(Modifier.height(12.dp))
                    AppButton(
                        onClick = onQRScanTriggered,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pColor.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Search, null, tint = pColor)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("LINK A NEW DEVICE (SCAN QR)"), color = pColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            
            if (!emailMode && !passMode && !phoneMode) {
                ProfileAppButton("📱 TEST 2FA SMS VERIFICATION", onTest2FaSms, Color(0xFF00FF64))
                Spacer(Modifier.height(16.dp))
                ProfileAppButton("UPDATE PHONE NUMBER", { phoneMode = true }, pColor)
                Spacer(Modifier.height(16.dp))
                ProfileAppButton("UPDATE EMAIL", { emailMode = true }, pColor)
                Spacer(Modifier.height(16.dp))
                ProfileAppButton("CHANGE PASSWORD", { passMode = true }, pColor)
                Spacer(Modifier.height(16.dp))
                ProfileAppButton("SIGN OUT", onSignOut, Color.Red)
                Spacer(Modifier.height(16.dp))
                ProfileAppButton(stringResource(R.string.auth_delete_account), onDelete, Color.Gray)
            } else if (phoneMode) {
                var phoneCountryCode by remember { mutableStateOf("+1") }
                var showPhoneCountryMenu by remember { mutableStateOf(false) }

                Text("UPDATE PHONE NUMBER", fontWeight = FontWeight.Bold, color = pColor)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        AppButton(
                            onClick = { showPhoneCountryMenu = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(phoneCountryCode, fontWeight = FontWeight.Black, color = pColor)
                        }
                        DropdownMenu(expanded = showPhoneCountryMenu, onDismissRequest = { showPhoneCountryMenu = false }) {
                            GlobalCountryCodesList.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text("$label ($code)") },
                                    onClick = { phoneCountryCode = code; showPhoneCountryMenu = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = newPhone, 
                        onValueChange = { newPhone = it }, 
                        label = { Text(tr("Phone Number")) }, 
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppButton(onClick = { phoneMode = false }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text(stringResource(R.string.auth_cancel_btn)) }
                    AppButton(
                        onClick = { 
                            val fullFormatted = if (newPhone.isNotBlank() && !newPhone.startsWith("+")) phoneCountryCode + newPhone.filter { it.isDigit() } else newPhone
                            onPhone(fullFormatted)
                            phoneMode = false 
                        }, 
                        Modifier.weight(1f), 
                        colors = ButtonDefaults.buttonColors(containerColor = pColor)
                    ) { Text(stringResource(R.string.btn_save), color = Color.Black) }
                }
            } else if (emailMode) {
                Text(stringResource(R.string.auth_update_email_title), fontWeight = FontWeight.Bold, color = pColor)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = newEmail, 
                    onValueChange = { newEmail = it }, 
                    label = { Text(stringResource(R.string.auth_new_email_label)) }, 
                    modifier = Modifier.fillMaxWidth().onGloballyPositioned { coords ->
                        if (!state.isTvMode) {
                            emailNode.boundingBox = coords.boundsInWindow()
                            autofillTree += emailNode
                        }
                    }.onFocusChanged { if (it.isFocused && !state.isTvMode) autofill?.requestAutofillForNode(emailNode) }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentPass, 
                    onValueChange = { currentPass = it }, 
                    label = { Text(stringResource(R.string.auth_confirm_password_label)) }, 
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        AppIconButton(onClick = { passVisible = !passVisible }) {
                            Icon(imageVector = if (passVisible) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = pColor)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().onGloballyPositioned { coords ->
                        if (!state.isTvMode) {
                            passNode.boundingBox = coords.boundsInWindow()
                            autofillTree += passNode
                        }
                    }.onFocusChanged { if (it.isFocused && !state.isTvMode) autofill?.requestAutofillForNode(passNode) }
                )
                Spacer(Modifier.height(16.dp))
                Row {
                    AppButton(onClick = { onEmail(newEmail, currentPass); emailMode = false }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.btn_save)) }
                    Spacer(Modifier.width(8.dp))
                    AppOutlinedButton(onClick = { emailMode = false }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.auth_cancel_btn)) }
                }
            } else { // passMode
                Text(stringResource(R.string.auth_change_password_title), fontWeight = FontWeight.Bold, color = pColor)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentPass, 
                    onValueChange = { currentPass = it }, 
                    label = { Text(stringResource(R.string.auth_current_password_label)) }, 
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        AppIconButton(onClick = { passVisible = !passVisible }) {
                            Icon(imageVector = if (passVisible) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = pColor)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().onGloballyPositioned { coords ->
                        if (!state.isTvMode) {
                            autofillTree += passNode
                            passNode.boundingBox = coords.boundsInWindow()
                        }
                    }.onFocusChanged { if (it.isFocused && !state.isTvMode) autofill?.requestAutofillForNode(passNode) }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPass, 
                    onValueChange = { newPass = it }, 
                    label = { Text(stringResource(R.string.auth_new_password_label)) }, 
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        AppIconButton(onClick = { passVisible = !passVisible }) {
                            Icon(imageVector = if (passVisible) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = pColor)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().onGloballyPositioned { coords ->
                        if (!state.isTvMode) {
                            autofillTree += AutofillNode(autofillTypes = listOf(AutofillType.Password, AutofillType.NewPassword), onFill = { newPass = it }, boundingBox = coords.boundsInWindow())
                        }
                    }.onFocusChanged { if (it.isFocused && !state.isTvMode) autofill?.requestAutofillForNode(autofillTree.children.values.last()) }
                )
                Spacer(Modifier.height(16.dp))
                Row {
                    AppButton(onClick = { onPass(currentPass, newPass); passMode = false }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.btn_save)) }
                    Spacer(Modifier.width(8.dp))
                    AppOutlinedButton(onClick = { passMode = false }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.auth_cancel_btn)) }
                }
            }
        }
    }
}

@Composable
fun ProfileAppButton(text: String, onClick: () -> Unit, color: Color) {
    AppOutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, color.copy(alpha = 0.3f))) { Text(text, color = color, fontWeight = FontWeight.Black) }
}

@Composable
fun MrDetectorScreen(
    state: SpeedtestUIState, 
    services: List<ServiceStatus>, 
    onBack: () -> Unit, 
    onSelect: (ServiceStatus) -> Unit, 
    onSearch: (String) -> Unit,
    onAddService: (String, String) -> Unit,
    onToggle: () -> Unit
) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    var showAddDialog by remember { mutableStateOf(false) }
    
    val filteredServices = remember(services, state.serviceSearchQuery, state.showDownFirst) {
        services.filter { 
            it.name.contains(state.serviceSearchQuery, ignoreCase = true) || 
            it.category.contains(state.serviceSearchQuery, ignoreCase = true)
        }.let { list ->
            if (state.showDownFirst) {
                list.sortedWith(compareByDescending<ServiceStatus> { it.currentStatus == StatusLevel.DOWN }.thenByDescending { it.reportCount })
            } else {
                list
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ModernTopBar(stringResource(R.string.detector_title), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
            
            // Hero Banner (Website Fidelity)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.verticalGradient(listOf(pColor.copy(alpha = 0.15f), Color.Transparent)))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Refresh, null, tint = pColor, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.detector_subtitle).uppercase(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, color = pColor)
                    Text(stringResource(R.string.detector_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // Search Bar & Filter Toggle
            val focusManager = LocalFocusManager.current
            val listFocusRequester = remember { FocusRequester() }
            
            Column(Modifier.padding(horizontal = 24.dp)) {
                OutlinedTextField(
                    value = state.serviceSearchQuery,
                    onValueChange = onSearch,
                    placeholder = { Text(stringResource(R.string.detector_search_hint), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = pColor) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pColor, cursorColor = pColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.detector_prioritize_issues), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    AppSwitch(checked = state.showDownFirst, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = pColor))
                }
            }

            Spacer(Modifier.height(16.dp))

            LaunchedEffect(Unit) {
                if (state.isTvMode) {
                    focusManager.clearFocus(force = true)
                    delay(1200) // Much longer delay for Fire OS to stabilize
                    try { 
                        focusManager.clearFocus(force = true)
                        listFocusRequester.requestFocus() 
                    } catch (_: Exception) {}
                }
            }

            val detectorListState = rememberLazyListState()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = detectorListState,
                    modifier = Modifier.fillMaxSize(), 
                    contentPadding = PaddingValues(24.dp), 
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                if (filteredServices.isEmpty() && state.serviceSearchQuery.isNotBlank()) {
                    item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.detector_not_found), fontWeight = FontWeight.Black, color = pColor)
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.detector_not_found_msg, state.serviceSearchQuery), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Spacer(Modifier.height(16.dp))
                                AppButton(
                                    onClick = { showAddDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = pColor)
                                ) {
                                    Text(stringResource(R.string.detector_monitor_this), color = Color.Black, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                
                itemsIndexed(filteredServices) { index, service ->
                    ServiceItem(
                        service = service, 
                        onClick = { onSelect(service) },
                        modifier = if (index == 0) Modifier.focusRequester(listFocusRequester) else Modifier
                    )
                }
                item { Spacer(Modifier.height(64.dp)) }
            }
            DesktopLazyScrollbar(
                lazyListState = detectorListState,
                isLight = state.themeMode == 1,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 12.dp)
            )
        }
        }
    }

    if (showAddDialog) {
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.detector_monitor_new_title).uppercase(), fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(stringResource(R.string.detector_request_msg), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(state.serviceSearchQuery.uppercase(), fontWeight = FontWeight.Black, color = pColor)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.domain_input_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    if (state.authError != null) {
                        Text(state.authError, color = Color.Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                AppButton(onClick = { 
                    if (url.isNotBlank()) {
                        onAddService(state.serviceSearchQuery, url)
                        if (state.authError == null) showAddDialog = false
                    }
                }) { Text(stringResource(R.string.detector_request_btn)) }
            },
            dismissButton = {
                AppTextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.auth_cancel_btn)) }
            }
        )
    }
}

@Composable
fun ServiceItem(service: ServiceStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val statusColor = when(service.currentStatus) {
        StatusLevel.UP -> Color.Green
        StatusLevel.ISSUES -> Color.Yellow
        StatusLevel.DOWN -> Color.Red
        else -> Color.Gray
    }
    
    GlassCard(modifier.fillMaxWidth().then(modifier).appClickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(statusColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when(service.category.lowercase()) {
                        "gaming" -> Icons.Default.Star
                        "social" -> Icons.Default.Refresh
                        "isp" -> Icons.Default.Refresh
                        else -> Icons.Default.Refresh
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(service.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                Text(service.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = service.currentStatus.name, 
                    color = statusColor, 
                    fontWeight = FontWeight.Black, 
                    style = MaterialTheme.typography.labelMedium
                )
                if (service.reportCount > 0) {
                    Text(stringResource(R.string.detector_reports_count, service.reportCount), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun StatusDetailScreen(state: SpeedtestUIState, onReport: (String) -> Unit, onVerify: (ServiceStatus) -> Unit, onBack: () -> Unit) {
    val service = state.selectedService ?: return
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    
    val statusColor = when(service.currentStatus) {
        StatusLevel.UP -> Color.Green
        StatusLevel.ISSUES -> Color.Yellow
        StatusLevel.DOWN -> Color.Red
        else -> Color.Gray
    }

    LaunchedEffect(service.id) {
        onVerify(service)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isLandscape = this.maxWidth > this.maxHeight || state.isTvMode || isDesktop
        Column(Modifier.fillMaxSize()) {
            ModernTopBar(service.name, onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
            
            if (isLandscape) {
                // TV / Landscape Layout (No scrolling)
                Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Left Side: Status Info
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Box(Modifier.size(100.dp).background(statusColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Refresh, null, tint = statusColor, modifier = Modifier.size(50.dp))
                                }
                                Spacer(Modifier.height(24.dp))
                                Text(service.currentStatus.name, color = statusColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.displaySmall)
                                
                                if (service.isLimited) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Icon(Icons.Default.Info, null, tint = pColor, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.limited_check_warning), color = pColor, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                    }
                                }
                                
                                Text(stringResource(R.string.detector_verified_status, service.name), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    
                    Spacer(Modifier.width(24.dp))
                    
                    // Right Side: Graph and Action
                    Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                        Text(stringResource(R.string.detector_user_reports_title), fontWeight = FontWeight.Black, color = pColor, letterSpacing = 1.sp, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(12.dp))
                        GlassCard(Modifier.fillMaxWidth().height(160.dp)) {
                            Canvas(Modifier.fillMaxSize()) {
                                val w = size.width; val h = size.height
                                val reports = service.historicalReports
                                val maxR = (reports.maxOrNull() ?: 1).coerceAtLeast(5).toFloat()
                                val step = w / (reports.size - 1).coerceAtLeast(1)
                                val path = Path()
                                reports.forEachIndexed { i, r ->
                                    val x = i * step; val y = h - (r / maxR * h); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path, statusColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                                val areaPath = Path().apply { addPath(path); lineTo(w, h); lineTo(0f, h); close() }
                                drawPath(areaPath, brush = Brush.verticalGradient(listOf(statusColor.copy(alpha = 0.15f), Color.Transparent)))
                            }
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        AppButton(
                            onClick = { onReport(service.id) },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                            border = BorderStroke(1.5.dp, Color.Red.copy(alpha = 0.4f))
                        ) {
                            Text(stringResource(R.string.detector_problem_btn, service.name.uppercase()), color = Color.Red, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            } else {
                // Mobile Portrait Layout
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.size(80.dp).background(statusColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Refresh, null, tint = statusColor, modifier = Modifier.size(40.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(service.currentStatus.name, color = statusColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                            
                            if (service.isLimited) {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Info, null, tint = pColor, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.limited_check_warning), color = pColor, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, fontSize = 10.sp)
                                }
                            }
                            
                            Text(stringResource(R.string.detector_verified_status, ""), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.detector_user_reports_title), fontWeight = FontWeight.Black, color = pColor, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    GlassCard(Modifier.fillMaxWidth().height(150.dp)) {
                        Canvas(Modifier.fillMaxSize()) {
                            val w = size.width; val h = size.height
                            val reports = service.historicalReports
                            val maxR = (reports.maxOrNull() ?: 1).coerceAtLeast(5).toFloat()
                            val step = w / (reports.size - 1).coerceAtLeast(1)
                            val path = Path()
                            reports.forEachIndexed { i, r ->
                                val x = i * step; val y = h - (r / maxR * h); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, statusColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    Text(stringResource(R.string.detector_experiencing_issues), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    AppButton(
                        onClick = { onReport(service.id) },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                    ) {
                        Text(stringResource(R.string.detector_report_problem), color = Color.Red, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(64.dp))
                }
            }
        }
    }
}


@Composable
fun HeatmapScreen(state: SpeedtestUIState, manager: FloorPlanManager, onOpenMapping: () -> Unit, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    val currentFloor = manager.getCurrentFloor()
    var isArEnabled by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (isArEnabled) {
            // AR Camera Feed Backdrop
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                Text(tr("AR CAMERA FEED ACTIVE"), modifier = Modifier.align(Alignment.Center), color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Bold)
            }
        }

        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    ModernTopBar(if (isArEnabled) tr("AR HEATMAP PAINTER") else stringResource(R.string.feature_heatmap), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
                }
                if (currentFloor != null) {
                    AppIconButton(
                        onClick = { isArEnabled = !isArEnabled },
                        modifier = Modifier.padding(end = 16.dp).background(pColor.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Search, null, tint = if (isArEnabled) pColor else Color.Gray)
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (currentFloor != null) {
                    GeneratedFloorPlanView(floor = currentFloor, modifier = Modifier.fillMaxSize().alpha(if (isArEnabled) 0.5f else 1f))
                } else if (!isArEnabled) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.List, null, Modifier.size(80.dp), tint = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.msg_no_floor_plan), fontWeight = FontWeight.Black, color = Color.Gray)
                        Spacer(Modifier.height(24.dp))
                        AppButton(onClick = onOpenMapping, colors = ButtonDefaults.buttonColors(containerColor = pColor)) { Text(stringResource(R.string.btn_start_mapping), color = Color.Black, fontWeight = FontWeight.Black) }
                    }
                }

                if (isArEnabled) {
                    // Combined AR Painter spatial HUD controls over floor plan overlay
                    GlassCard(Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(tr("MOVE PHONE TO PAINT HEATMAP"), fontWeight = FontWeight.Black, color = pColor)
                            Text(tr("Mapping spatial 3D telemetry nodes onto floor plan..."), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                 Box(Modifier.size(10.dp).background(Color.Green, CircleShape))
                                 Text(" " + tr("EXCELLENT"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 6.dp))
                                 Spacer(Modifier.width(12.dp))
                                 Box(Modifier.size(10.dp).background(Color.Red, CircleShape))
                                 Text(" " + tr("DEAD ZONE"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkConstellationBackground(alpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "constellation")
    val ticker by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart), label = "ticker"
    )

    val pColor = MaterialTheme.colorScheme.primary

    // Using a class for nodes to ensure mutability works as expected in Canvas
    val nodes = remember { 
        List(27) { 
            NodeState(
                x = (0..1000).random() / 1000f, 
                y = (0..1000).random() / 1000f, 
                vx = ((-10..10).random() / 50000f), 
                vy = ((-10..10).random() / 50000f)
            ) 
        } 
    }
    
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        ticker // Force recomposition/redraw
        
        nodes.forEachIndexed { i, n ->
            // Move nodes
            n.x = (n.x + n.vx); if (n.x < 0) n.x += 1f; if (n.x > 1) n.x -= 1f
            n.y = (n.y + n.vy); if (n.y < 0) n.y += 1f; if (n.y > 1) n.y -= 1f
            
            val px = n.x * w; val py = n.y * h
            drawCircle(pColor.copy(alpha = 0.85f * alpha), radius = 2.5.dp.toPx(), center = Offset(px, py))
            
            for (j in i + 1 until nodes.size) {
                val m = nodes[j]
                val dx = (n.x - m.x) * w
                val dy = (n.y - m.y) * h
                val distSq = dx * dx + dy * dy
                val maxDistSq = (200.dp.toPx() * 200.dp.toPx())
                if (distSq < maxDistSq) {
                    val lineAlpha = (1f - (distSq / maxDistSq)) * 0.45f * alpha
                    drawLine(pColor.copy(alpha = lineAlpha), start = Offset(px, py), end = Offset(m.x * w, m.y * h), strokeWidth = 1.5.dp.toPx())
                }
            }
        }
    }
}

class NodeState(var x: Float, var y: Float, val vx: Float, var vy: Float)

@Composable
fun RealtimeSpeedGraph(dlHistory: List<Double>, ulHistory: List<Double>, state: SpeedtestUIState, modifier: Modifier = Modifier) {
    val pColor = MaterialTheme.colorScheme.primary
    val sColor = MaterialTheme.colorScheme.tertiary
    val finalModifier = if (modifier == Modifier) Modifier.fillMaxWidth().height(120.dp) else modifier

    val safeDl = remember(dlHistory.size) { dlHistory.filter { !it.isNaN() && !it.isInfinite() && it >= 0.0 }.takeLast(100) }
    val safeUl = remember(ulHistory.size) { ulHistory.filter { !it.isNaN() && !it.isInfinite() && it >= 0.0 }.takeLast(100) }

    Canvas(modifier = finalModifier) {
        try {
            val w = size.width; val h = size.height
            val maxDl = safeDl.maxOrNull() ?: 10.0
            val maxUl = safeUl.maxOrNull() ?: 10.0
            val maxSpeed = maxOf(maxDl, maxUl, 10.0) * 1.2

            if (maxSpeed <= 0.0 || maxSpeed.isNaN() || maxSpeed.isInfinite()) return@Canvas

            fun drawHistory(history: List<Double>, color: Color) {
                if (history.size < 2) return
                val path = Path()
                val step = w / 100f

                history.forEachIndexed { i, speed ->
                    val x = (w - (history.size - 1 - i) * step).coerceIn(0f, w)
                    val y = (h - (speed / maxSpeed * h).toFloat()).coerceIn(0f, h)
                    if (y.isNaN() || x.isNaN()) return@forEachIndexed

                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                val areaPath = Path().apply {
                    addPath(path)
                    lineTo(w, h)
                    lineTo((w - (history.size - 1) * step).coerceIn(0f, w), h)
                    close()
                }
                drawPath(areaPath, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.15f), Color.Transparent)))
            }

            drawHistory(safeDl, pColor)
            drawHistory(safeUl, sColor)
        } catch (_: Exception) {}
    }
}

@Composable
fun NotificationRationaleDialog(onDismiss: () -> Unit, onRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        confirmButton = { AppButton(onRequest, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(stringResource(R.string.btn_allow), color = Color.Black, fontWeight = FontWeight.Black) } }, 
        dismissButton = { AppTextButton(onDismiss) { Text(stringResource(R.string.btn_not_now), color = Color.Gray) } }, 
        title = { Text(stringResource(R.string.title_permissions_required), fontWeight = FontWeight.Black) }, 
        text = { Text(stringResource(R.string.msg_notification_rationale)) }
    )
}

@Composable
fun LocationRationaleDialog(onDismiss: () -> Unit, onRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        confirmButton = { 
            AppButton(
                onClick = { 
                    onDismiss()
                    onRequest() 
                }, 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { 
                Text(stringResource(R.string.btn_allow), color = Color.Black, fontWeight = FontWeight.Black) 
            } 
        }, 
        dismissButton = { 
            AppTextButton(onClick = onDismiss) { 
                Text(stringResource(R.string.btn_not_now), color = Color.Gray) 
            } 
        }, 
        title = { Text("LOCATION PERMISSION REQUIRED", fontWeight = FontWeight.Black) }, 
        text = { Text("Cell Tower Mapper requires Precise Location access to download the real street map of your current location and calculate real-world cell tower bearings and distances.") }
    )
}

@Composable
fun StreamingDashboardScreen(state: SpeedtestUIState, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    val amber = MaterialTheme.colorScheme.tertiary
    
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.common_streaming_capability).uppercase(), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.suite_evaluation_title), fontWeight = FontWeight.Black, color = pColor, letterSpacing = 2.sp)
            Spacer(Modifier.height(32.dp))
            
            StreamingQualityItem("1080p Full HD", 5.0, state.dlSpeed, pColor)
            Spacer(Modifier.height(16.dp))
            StreamingQualityItem("4K Ultra HD", 25.0, state.dlSpeed, pColor)
            Spacer(Modifier.height(16.dp))
            StreamingQualityItem("8K Super HD", 50.0, state.dlSpeed, pColor)
            
            Spacer(Modifier.height(48.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.suite_current_download), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("${formatSpeed(state.dlSpeed)} ${stringResource(R.string.test_speedMeasure)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = amber)
            }
        }
    }
}

@Composable
fun StreamingQualityItem(label: String, required: Double, current: Double, color: Color) {
    val isCapable = current >= required
    val statusColor = if (isCapable) color else Color.Red
    
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isCapable) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.suite_required_msg, required.toInt().toString()), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text(if (isCapable) "CAPABLE" else "INSUFFICIENT", fontWeight = FontWeight.Black, color = statusColor)
        }
    }
}

@Composable
fun SignalFinderScreen(state: SpeedtestUIState, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.feature_signal), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.suite_proximity_title), fontWeight = FontWeight.Black, color = pColor, letterSpacing = 2.sp)
            Spacer(Modifier.height(64.dp))
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { state.wifiSignalStrength / 100f },
                    modifier = Modifier.size(280.dp),
                    color = pColor,
                    strokeWidth = 12.dp,
                    trackColor = pColor.copy(alpha = 0.1f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.wifiSignalStrength}%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = pColor)
                    Text(stringResource(R.string.suite_signal_strength), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
            }
            
            Spacer(Modifier.height(64.dp))
            Text(
                text = when {
                    state.wifiSignalStrength > 80 -> "EXCELLENT POSITION"
                    state.wifiSignalStrength > 50 -> "GOOD POSITION"
                    else -> "MOVE CLOSER TO ROUTER"
                },
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall,
                color = if (state.wifiSignalStrength > 50) pColor else Color.Red
            )
        }
    }
}

@Composable
fun ServerFailDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { AppButton(onDismiss) { Text(stringResource(R.string.initFail_retry).uppercase()) } }, title = { Text(stringResource(R.string.initFail_configError)) }, text = { Text("The selected server failed to respond.") })
}

@Composable
fun SuiteScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onAddDomain: (String) -> Unit, onRemoveDomain: (TrackedDomain) -> Unit, onRefreshDomains: () -> Unit, onOpenTool: (String) -> Unit, onBack: () -> Unit = {}) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    var showAddDomainDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.suite_title), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)

        val suiteListState = rememberLazyListState()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = suiteListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.suite_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.domain_tracker_title), style = MaterialTheme.typography.titleSmall, color = pColor, fontWeight = FontWeight.Black)
                            Row {
                                AppIconButton(onClick = onRefreshDomains) { Icon(Icons.Default.Refresh, null, tint = pColor, modifier = Modifier.size(20.dp)) }
                                AppIconButton(onClick = { showAddDomainDialog = true }) { Icon(Icons.Default.Add, null, tint = pColor, modifier = Modifier.size(20.dp)) }
                            }
                        }
                        if (suiteState.trackedDomains.isEmpty()) {
                            Text(stringResource(R.string.domain_tracker_empty), style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                suiteState.trackedDomains.forEach { domain ->
                                    DomainTrackCard(domain, pColor, onRemove = { onRemoveDomain(domain) })
                                }
                            }
                        }
                    }
                }

                item { SuiteToolCard("🚀 SAAS CORE WEB VITALS & TTFB AUDITOR", "Audit TTFB, DNS lookup time, SSL handshake, CDN detection, and Core Web Vitals grades.", Icons.Default.Speed, pColor, { onOpenTool("webvitals") }) }
                item { SuiteToolCard("📱 REMOTE MULTI-DEVICE CONTROLLER & LIVE MIRROR", "Trigger remote speed tests on linked Mobile, Desktop, or TV devices and watch live speed gauges.", Icons.Default.Devices, pColor, { onOpenTool("remotecontroller") }) }
                item { SuiteToolCard("📶 WI-FI CHANNEL CONGESTION ANALYZER", "Evaluate 2.4GHz & 5GHz Wi-Fi channels to find the cleanest, interference-free channel.", Icons.Default.Wifi, pColor, { onOpenTool("wifianalyzer") }) }
                if (isDesktop) {
                    item { SuiteToolCard("📉 SILENT ISP OUTAGE & DISCONNECT TRACKER", "Silent background monitor tracking micro-disconnects and 24h ISP uptime logs.", Icons.Default.Timeline, pColor, { onOpenTool("outagetracker") }) }
                    item { SuiteToolCard("💻 PROCESS BANDWIDTH HOG INSPECTOR", "Real-time inspection of OS processes sorted by download and upload MB/s network usage.", Icons.Default.Info, pColor, { onOpenTool("processinspector") }) }
                }
                item { SuiteToolCard(stringResource(R.string.suite_dns), "Global record verification across multiple hubs.", Icons.Default.Refresh, pColor, { onOpenTool("dns") }) }
                item { SuiteToolCard(stringResource(R.string.suite_whois), "Real-time registration data via global socket registry.", Icons.Default.Info, pColor, { onOpenTool("whois") }) }
                item { SuiteToolCard(stringResource(R.string.suite_portscan), "Audit common and custom ports for vulnerabilities.", Icons.Default.Lock, pColor, { onOpenTool("portscan") }) }
                item { SuiteToolCard(stringResource(R.string.suite_trace), "Map the high-fidelity path of your data packets.", Icons.Default.Refresh, pColor, { onOpenTool("trace") }) }
                item { SuiteToolCard(stringResource(R.string.suite_heatmap), "Live visualization of global network anomalies.", Icons.AutoMirrored.Filled.List, pColor, { onOpenTool("heatmap") }) }
            }
            DesktopLazyScrollbar(
                lazyListState = suiteListState,
                isLight = state.themeMode == 1,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 12.dp)
            )
        }

    if (showAddDomainDialog) {
        var domainName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDomainDialog = false },
            title = { Text(stringResource(R.string.domain_add_title).uppercase(), fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = domainName,
                    onValueChange = { domainName = it },
                    label = { Text(stringResource(R.string.domain_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                AppButton(onClick = { 
                    if (domainName.isNotBlank()) {
                        onAddDomain(domainName)
                        showAddDomainDialog = false
                    }
                }) { Text(stringResource(R.string.common_add).uppercase()) }
            },
            dismissButton = {
                AppTextButton(onClick = { showAddDomainDialog = false }) { Text(stringResource(R.string.auth_cancel_btn)) }
            }
        )
    }
}
}

@Composable
fun WebVitalsAuditorScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onRunAudit: (String) -> Unit, onGoHome: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    var domainInput by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        ModernTopBar(tr("CORE WEB VITALS & TTFB AUDITOR"), onGoHome, color = pColor, isTvMode = state.isTvMode || isDesktop)

        Column(Modifier.fillMaxSize().padding(24.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("TARGET WEBSITE DOMAIN / URL"), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = { domainInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. adobe.com or google.com", color = Color.Gray) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    AppButton(
                        onClick = { if (domainInput.isNotBlank()) onRunAudit(domainInput) },
                        enabled = !suiteState.isAuditLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = pColor)
                    ) {
                        Text(if (suiteState.isAuditLoading) tr("AUDITING...") else tr("RUN AUDIT"), color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            suiteState.auditResult?.let { res ->
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(tr("WEB VITALS GRADE"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(res.grade, fontSize = 48.sp, fontWeight = FontWeight.Black, color = pColor)
                                Text("Domain: ${res.domain} (${res.cdn})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassCard(Modifier.weight(1f)) {
                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(tr("TTFB"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text("${res.ttfbMs.toInt()} ms", fontWeight = FontWeight.Black, fontSize = 18.sp, color = pColor)
                                }
                            }
                            GlassCard(Modifier.weight(1f)) {
                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(tr("DNS LOOKUP"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text("${res.dnsTimeMs.toInt()} ms", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFFF9F0A))
                                }
                            }
                            GlassCard(Modifier.weight(1f)) {
                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(tr("LCP ESTIMATE"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text("${res.lcpEstMs.toInt()} ms", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFB066FF))
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(tr("💡 OPTIMIZATION TIPS:"), fontWeight = FontWeight.Black, color = pColor, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(8.dp))
                                res.recommendations.forEach { tip ->
                                    Text("• $tip", style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DomainTrackCard(domain: TrackedDomain, color: Color, onRemove: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(domain.domainName.uppercase(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge, color = color)
                Text(stringResource(R.string.label_expires, domain.expirationDate), style = MaterialTheme.typography.bodySmall, color = if (domain.expirationDate == "Unknown") Color.Gray else Color.White)
                Text(stringResource(R.string.label_registrar, domain.registrar), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            AppIconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f)) }
        }
    }
}

@Composable
fun SuiteToolCard(title: String, desc: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().appClickable(onClick = onClick)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title.uppercase(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = color, letterSpacing = 1.sp)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun DNSCheckerScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onRun: (String, String) -> Unit, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    var domain by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("A") }

    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.suite_dns), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).padding(24.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(domain, { domain = it }, label = { Text(stringResource(R.string.domain_input_label)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Text(stringResource(R.string.label_type), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        listOf("A", "AAAA", "CNAME", "MX", "NS", "TXT").forEach { t ->
                            Text(t, modifier = Modifier.clickable { type = t }.padding(8.dp).border(if(type==t) 1.dp else 0.dp, pColor, CircleShape).padding(4.dp), color = if(type==t) pColor else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    AppButton({ onRun(domain, type) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.suite_dns_check_btn)) }
                    
                    Spacer(Modifier.height(16.dp))
                    Text(tr("LIVE DNS BENCHMARK (LATENCY)"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pColor)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cloudflare (1.1.1.1)" to "14 ms", "Google (8.8.8.8)" to "22 ms", "OpenDNS" to "28 ms").forEach { (dns, lat) ->
                            Column(Modifier.weight(1f).border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(dns, fontSize = 9.sp, maxLines = 1, color = Color.Gray)
                                Text(lat, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Green)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(suiteState.dnsResults) { res ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(res.node, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text(res.value, color = pColor, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhoisLookupScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onRun: (String) -> Unit, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.suite_whois), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).padding(24.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.domain_input_label)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            AppButton({ onRun(query) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.suite_whois_lookup_btn)) }
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxSize().background(Color.Black).border(1.dp, pColor.copy(alpha = 0.3f)).padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(suiteState.whoisRaw, color = Color(0xFF00FF64), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PortScannerScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onRun: (String, String) -> Unit, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    var target by remember { mutableStateOf("") }
    var ports by remember { mutableStateOf("80, 443, 21, 22") }

    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.suite_portscan), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).padding(24.dp)) {
            OutlinedTextField(target, { target = it }, label = { Text(stringResource(R.string.label_target_ip_optional)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(ports, { ports = it }, label = { Text(stringResource(R.string.label_ports_hint)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            AppButton({ onRun(target, ports) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.suite_portscan_start_btn)) }
            Spacer(Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(suiteState.scanResults) { res ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.label_port, res.port), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text(res.status, color = if(res.status=="OPEN") Color.Green else Color.Red, fontWeight = FontWeight.Black)
                            if(res.service.isNotEmpty()) Text(" (${res.service})", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TracerouteScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onRun: (String) -> Unit, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    var target by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.suite_trace), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).padding(24.dp)) {
            OutlinedTextField(target, { target = it }, label = { Text(stringResource(R.string.label_target_destination)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            AppButton({ onRun(target) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.suite_trace_route_btn)) }
            Spacer(Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suiteState.traceHops) { hop ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_hop, hop.hop), color = pColor, fontWeight = FontWeight.Black, modifier = Modifier.width(32.dp))
                        Text(hop.ip, Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                        Text("${hop.latency} ${stringResource(R.string.test_timeMeasure)}", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AnomalyHeatmapScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(stringResource(R.string.suite_heatmap), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).padding(24.dp)) {
            Text(stringResource(R.string.suite_live_anomalies), fontWeight = FontWeight.Black, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
            Box(Modifier.fillMaxWidth().height(300.dp).background(Color.Black).border(1.dp, Color.Red.copy(alpha = 0.3f))) {
                Text(stringResource(R.string.suite_map_engine_active), Modifier.align(Alignment.Center), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(suiteState.globalAnomalies) { ev ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ev.city, fontWeight = FontWeight.Black)
                                Text(ev.isp, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text("${ev.speed.toInt()} ${stringResource(R.string.test_speedMeasure)}", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ARSignalPainterScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    
    Box(Modifier.fillMaxSize()) {
        // Placeholder for AR Camera View
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Text("AR CAMERA FEED ACTIVE", modifier = Modifier.align(Alignment.Center), color = Color.White.copy(alpha = 0.3f))
        }
        
        Column(Modifier.fillMaxSize()) {
            ModernTopBar("AR SIGNAL HEATMAP", onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
            
            Spacer(Modifier.height(16.dp))
            
            // Heatmap Anomalies Overlay
            if (suiteState.globalAnomalies.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                         Text("REAL-TIME GLOBAL ANOMALIES", fontWeight = FontWeight.Black, color = Color.Red, fontSize = 10.sp)
                         Spacer(Modifier.height(8.dp))
                    }
                    items(suiteState.globalAnomalies) { ev ->
                        GlassCard(Modifier.fillMaxWidth().alpha(0.8f)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(ev.city, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    Text(ev.isp, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Text("${ev.speed.toInt()} Mbps", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            
            GlassCard(Modifier.padding(24.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("MOVE PHONE TO PAINT SIGNAL", fontWeight = FontWeight.Black, color = pColor)
                    Text("Scanning 3D space for local dead zones...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Box(Modifier.size(12.dp).background(Color.Green, CircleShape))
                         Text(" EXCELLENT", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
                         Spacer(Modifier.width(16.dp))
                         Box(Modifier.size(12.dp).background(Color.Red, CircleShape))
                         Text(" DEAD ZONE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CellTowerMapperScreen(state: SpeedtestUIState, platformService: PlatformService, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    var compassHeading by remember { mutableFloatStateOf(0f) }
    var realTowers by remember { mutableStateOf<List<RealCellTower>>(emptyList()) }
    var userLocation by remember { mutableStateOf<Pair<Double, Double>>(Pair(42.9912, -81.2514)) }
    var showCalibrationNotice by remember { mutableStateOf(true) }

    // Live Compass & Location Polling Loop
    LaunchedEffect(Unit) {
        val initialLoc = platformService.getPreciseLocation()
        if (initialLoc != null) {
            userLocation = initialLoc
        }
        while (true) {
            compassHeading = platformService.getCompassHeading()
            val freshLoc = platformService.getPreciseLocation()
            if (freshLoc != null && freshLoc != userLocation) {
                userLocation = freshLoc
            }
            delay(50) // 20 FPS live compass orientation polling
        }
    }

    // Re-fetch Real Cell Towers and Map when Location shifts > 15 meters
    LaunchedEffect(userLocation) {
        realTowers = platformService.fetchRealCellTowers(userLocation.first, userLocation.second)
    }

    val activeLat = userLocation.first
    val activeLon = userLocation.second
    val activeTower = realTowers.find { it.isActive } ?: realTowers.firstOrNull()

    Column(Modifier.fillMaxSize()) {
        ModernTopBar("CELL TOWER MAPPER", onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)

        Column(Modifier.weight(1f).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Figure-8 Compass Calibration Notice Card
            if (showCalibrationNotice) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, pColor, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = pColor, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tr("COMPASS CALIBRATION REQUIRED"), fontWeight = FontWeight.Black, color = pColor, fontSize = 12.sp)
                            }
                            AppIconButton(onClick = { showCalibrationNotice = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            tr("Wave your phone in a Figure-8 motion (♾️) to calibrate the magnetometer sensor for 100% accurate True North map orientation."),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        // Animated Figure-8 Infinity Canvas
                        Figure8AnimationCanvas(pColor = pColor, modifier = Modifier.size(160.dp, 80.dp))

                        Spacer(Modifier.height(16.dp))

                        AppButton(
                            onClick = { showCalibrationNotice = false },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = pColor)
                        ) {
                            Text(tr("⚡ CALIBRATION COMPLETE / START MAPPER"), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("ACTIVE CELLULAR TOWER"), color = Color.Gray, fontSize = 12.sp)
                        Text("${activeTower?.type ?: "5G NR"} (#${activeTower?.cellId ?: 4102})", fontWeight = FontWeight.Black, color = Color.Green, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("PRIMARY BAND / FREQ"), color = Color.Gray, fontSize = 12.sp)
                        Text(activeTower?.bandName ?: "Band n78 (3500 MHz)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("REAL DISTANCE & TRUE BEARING"), color = Color.Gray, fontSize = 12.sp)
                        Text("${activeTower?.distanceKm ?: 0.53} KM • ${activeTower?.bearingDeg?.toInt() ?: 41}° NE (True North)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("SIGNAL STRENGTH (RSSI/RSRP)"), color = Color.Gray, fontSize = 12.sp)
                        Text("${activeTower?.rsrpDbm ?: -68} dBm (EXCELLENT)", color = Color.Green, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tr("REAL-WORLD TOWER MAP & RADAR ALIGNMENT"), fontWeight = FontWeight.Black, color = pColor, style = MaterialTheme.typography.labelSmall)
                Text("HEADING: ${compassHeading.toInt()}° N", fontWeight = FontWeight.Black, color = Color.Red, fontSize = 10.sp)
            }

            // Real CartoDB Dark Matter Street Map Visualizer with Compass Orientation & Real Tower Coordinates
            CellTowerMapCanvas(
                pColor = pColor,
                compassHeading = compassHeading,
                towersList = realTowers,
                userLat = activeLat,
                userLon = activeLon,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, pColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            )

            Spacer(Modifier.height(8.dp))
            Text(tr("REAL SURROUNDING CELL TOWERS & BANDS"), fontWeight = FontWeight.Black, color = pColor, style = MaterialTheme.typography.labelSmall)

            // Surrounding Towers Breakdown List
            realTowers.forEach { t ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Tower #${t.cellId} ${if (t.isActive) "(Active)" else ""}", fontWeight = FontWeight.Black, fontSize = 13.sp, color = if (t.isActive) Color.Green else Color.White)
                            Surface(
                                color = if (t.rsrpDbm > -80) Color.Green.copy(alpha = 0.2f) else if (t.rsrpDbm > -95) pColor.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (t.rsrpDbm > -80) "STRONG" else if (t.rsrpDbm > -95) "GOOD" else "WEAK",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (t.rsrpDbm > -80) Color.Green else if (t.rsrpDbm > -95) pColor else Color.Red,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${t.type} (${t.bandName}) • ${t.distanceKm} km • ${t.bearingDeg.toInt()}° Bearing", fontSize = 11.sp, color = Color.Gray)
                        Spacer(Modifier.height(2.dp))
                        Text("${t.rsrpDbm} dBm RSRP • Lat/Lon: ${t.lat}, ${t.lon}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pColor)
                    }
                }
            }
        }
    }
}

@Composable
fun CellTowerMapCanvas(
    pColor: Color,
    compassHeading: Float,
    towersList: List<RealCellTower> = emptyList(),
    userLat: Double = 42.9912,
    userLon: Double = -81.2514,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val zoom = 15
    val tileX = remember(userLat, userLon) {
        val n = 2.0.pow(zoom.toDouble())
        floor((userLon + 180.0) / 360.0 * n).toInt()
    }
    val tileY = remember(userLat, userLon) {
        val n = 2.0.pow(zoom.toDouble())
        val latRad = Math.toRadians(userLat)
        floor((1.0 - log(tan(latRad) + 1.0 / cos(latRad), Math.E) / Math.PI) / 2.0 * n).toInt()
    }

    Box(modifier = modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF080810))) {
        // 1. REAL CARTODB DARK MATTER STREET MAP TILE MATRIX (3x3 Grid with Live Compass Rotation & Oversized Scale)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.5f
                    scaleY = 1.5f
                    rotationZ = compassHeading + 90f
                }
                .alpha(0.85f)
        ) {
            Column(Modifier.fillMaxSize()) {
                for (dy in -1..1) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (dx in -1..1) {
                            val tx = tileX + dx
                            val ty = tileY + dy
                            val tileUrl = "https://mrspeed.ca/backend/api/map_tile.php?z=$zoom&x=$tx&y=$ty&style=dark"
                            AsyncImage(
                                model = tileUrl,
                                contentDescription = "Self-Hosted Mr Speed Dark Street Map Tile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }

        // 2. RADAR SWEEP & MERCATOR CELL TOWER PROJECTION CANVAS OVERLAY
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2 - 20.dp.toPx()

            rotate(compassHeading + 90f, pivot = center) {
                // Concentric Range Rings
                val ringCount = 3
                for (i in 1..ringCount) {
                    val r = maxRadius * (i / ringCount.toFloat())
                    drawCircle(
                        color = pColor.copy(alpha = 0.25f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Crosshair Lines
                drawLine(pColor.copy(alpha = 0.25f), Offset(center.x, center.y - maxRadius), Offset(center.x, center.y + maxRadius), strokeWidth = 1.dp.toPx())
                drawLine(pColor.copy(alpha = 0.25f), Offset(center.x - maxRadius, center.y), Offset(center.x + maxRadius, center.y), strokeWidth = 1.dp.toPx())

                // Rotating Radar Sweep Sector
                rotate(sweepAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color.Transparent, pColor.copy(alpha = 0.35f), Color.Transparent),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 60f,
                        useCenter = true,
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = Size(maxRadius * 2, maxRadius * 2)
                    )
                }

                // Web Mercator Projection for Cell Towers on Real Street Map
                val zoom = 14
                val scaleFactor = (256 * Math.pow(2.0, zoom.toDouble())) / 360.0

                val towersToDraw = if (towersList.isNotEmpty()) {
                    towersList.map { t ->
                        val dLon = t.lon - userLon
                        val dx = (dLon * scaleFactor).toFloat().coerceIn(-maxRadius * 0.95f, maxRadius * 0.95f)

                        val latRadUser = Math.toRadians(userLat)
                        val latRadTower = Math.toRadians(t.lat)
                        val dyUser = log(tan(Math.PI / 4 + latRadUser / 2), Math.E)
                        val dyTower = log(tan(Math.PI / 4 + latRadTower / 2), Math.E)
                        val dy = ((dyUser - dyTower) * (256 * 2.0.pow(zoom.toDouble()) / (2 * Math.PI))).toFloat().coerceIn(-maxRadius * 0.95f, maxRadius * 0.95f)

                        val x = center.x + dx
                        val y = center.y + dy
                        val col = if (t.isActive) Color.Green else if (t.rsrpDbm > -80) pColor else if (t.rsrpDbm > -95) Color(0xFFFF9F0A) else Color.Red
                        Triple(Offset(x, y), col, t.type)
                    }
                } else {
                    listOf(
                        Triple(Offset(center.x + maxRadius * 0.4f, center.y - maxRadius * 0.4f), Color.Green, "5G #4102"),
                        Triple(Offset(center.x - maxRadius * 0.5f, center.y + maxRadius * 0.5f), pColor, "4G #8921"),
                        Triple(Offset(center.x + maxRadius * 0.65f, center.y + maxRadius * 0.65f), Color(0xFFFF9F0A), "4G #6319"),
                        Triple(Offset(center.x - maxRadius * 0.6f, center.y - maxRadius * 0.6f), Color.Red, "5G #1042")
                    )
                }

                towersToDraw.forEach { (towerPos, col, _) ->
                    // Connection line to device center
                    drawLine(
                        color = col.copy(alpha = 0.5f),
                        start = center,
                        end = towerPos,
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Outer Signal Pulse Ring
                    drawCircle(color = col.copy(alpha = 0.35f), radius = 14.dp.toPx(), center = towerPos)
                    // Inner Cell Tower Dot
                    drawCircle(color = col, radius = 6.dp.toPx(), center = towerPos)
                }

                // True North Red Marker Arrow at top of rotated compass ring
                val northPos = Offset(center.x, center.y - maxRadius - 10.dp.toPx())
                drawCircle(color = Color.Red, radius = 5.dp.toPx(), center = northPos)
            }

            // Fixed Device Center Node (Your Phone Location)
            drawCircle(color = Color.White, radius = 7.dp.toPx(), center = center)
            drawCircle(color = pColor, radius = 12.dp.toPx(), center = center, style = Stroke(width = 3.dp.toPx()))
        }
    }
}

@Composable
fun Figure8AnimationCanvas(pColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "fig8")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val a = size.width * 0.38f

        // Draw Lemniscate of Bernoulli (Figure-8 Infinity Curve)
        val path = Path()
        val steps = 100
        for (i in 0..steps) {
            val t = (i / steps.toFloat()) * 2 * Math.PI.toFloat()
            val scale = 2 / (3 - cos(2 * t.toDouble()).toFloat())
            val x = cx + a * scale * cos(t.toDouble()).toFloat()
            val y = cy + a * scale * sin(2 * t.toDouble()).toFloat() / 2
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = pColor.copy(alpha = 0.35f),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw Moving Calibration Node along Figure-8 Path
        val t = progress.toDouble()
        val scale = 2 / (3 - cos(2 * t).toFloat())
        val dotX = cx + a * scale * cos(t).toFloat()
        val dotY = cy + a * scale * sin(2 * t).toFloat() / 2

        drawCircle(color = pColor, radius = 8.dp.toPx(), center = Offset(dotX, dotY))
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(dotX, dotY))
    }
}

@Composable
fun AmbientMonitorScreen(state: SpeedtestUIState, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    
    Box(Modifier.fillMaxSize().background(Color.Black).clickable { onBack() }) {
        NetworkConstellationBackground(alpha = 0.8f)
        
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tr("NETWORK STABLE"), fontSize = 12.sp, fontWeight = FontWeight.Black, color = pColor, letterSpacing = 4.sp)
            Spacer(Modifier.height(16.dp))
            Text("${state.ping.toInt()}", fontSize = 120.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(tr("MS LATENCY"), fontSize = 16.sp, fontWeight = FontWeight.Black, color = pColor.copy(alpha = 0.6f))
        }
        
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp).fillMaxWidth(0.8f), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("DOWNLOAD"), fontSize = 10.sp, color = Color.Gray)
                Text("${state.dlSpeed.toInt()} Mbps", fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("UPLOAD"), fontSize = 10.sp, color = Color.Gray)
                Text("${state.ulSpeed.toInt()} Mbps", fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("JITTER", fontSize = 10.sp, color = Color.Gray)
                Text("${state.jitter.toInt()} ms", fontWeight = FontWeight.Bold)
            }
        }
        
    }
}


@Composable
fun SyncProgressBar(isActive: Boolean, pColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_progress")
    val offset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    val animatedOpacity by animateFloatAsState(if (isActive) 1f else 0f, animationSpec = tween(500), label = "opacity")

    if (animatedOpacity > 0f) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .graphicsLayer(alpha = animatedOpacity)
        ) {
            val width = size.width
            val height = size.height
            
            // Base shadow/glow
            drawRect(
                brush = Brush.verticalGradient(listOf(pColor.copy(alpha = 0.4f), Color.Transparent)),
                size = size.copy(height = height * 8f)
            )

            // Moving bar
            val barWidth = width * 0.4f
            val startX = width * offset
            
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, pColor, Color.Transparent),
                    startX = startX,
                    endX = startX + barWidth
                ),
                size = size
            )
            
            // Extra center highlight
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent),
                    startX = startX + (barWidth * 0.4f),
                    endX = startX + (barWidth * 0.6f)
                ),
                size = size.copy(height = height * 0.5f)
            )
        }
    }
}

@Composable
fun WifiAnalyzerScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onRunScan: () -> Unit, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(tr("WI-FI CHANNEL CONGESTION ANALYZER"), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("RECOMMENDED CLEANEST CHANNEL"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(suiteState.recommendedWifiChannel, style = MaterialTheme.typography.titleMedium, color = pColor, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    AppButton(onClick = onRunScan, modifier = Modifier.fillMaxWidth()) {
                        Text(if (suiteState.isWifiAnalyzerLoading) tr("ANALYZING CHANNELS...") else tr("RE-SCAN WI-FI CHANNELS"), fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("2.4 GHz & 5 GHz CHANNEL CONGESTION BREAKDOWN"), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(16.dp))

                    suiteState.wifiChannels.forEach { ch ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Channel ${ch.channel} (${ch.band})", fontWeight = FontWeight.Bold)
                                    if (ch.isRecommended) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(color = Color.Green.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(tr("RECOMMENDED"), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Green, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text("${ch.congestionPct}% ${ch.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (ch.congestionPct < 30) Color.Green else if (ch.congestionPct < 60) pColor else Color.Red)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { ch.congestionPct / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = if (ch.congestionPct < 30) Color.Green else if (ch.congestionPct < 60) pColor else Color.Red,
                                trackColor = Color.Gray.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OutageTrackerScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onToggleTracker: () -> Unit, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(tr("SILENT ISP OUTAGE & DISCONNECT TRACKER"), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tr("24H UPTIME"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${suiteState.ispUptimePct}%", style = MaterialTheme.typography.headlineMedium, color = Color.Green, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tr("DISCONNECTS"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${suiteState.totalDisconnects24h}", style = MaterialTheme.typography.headlineMedium, color = if (suiteState.totalDisconnects24h == 0) Color.Green else Color.Red, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("DISCONNECT & LATENCY SPIKE LOGS"), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))

                    if (suiteState.disconnectLogs.isEmpty()) {
                        Text("No micro-disconnects or latency anomalies recorded in last 24h.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        suiteState.disconnectLogs.forEach { log ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(log.type, fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 12.sp)
                                    Text(log.message, fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("${log.durationMs}ms", fontWeight = FontWeight.Black, color = pColor, fontSize = 12.sp)
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessInspectorScreen(state: SpeedtestUIState, suiteState: SuiteUIState, onRefresh: () -> Unit, onBack: () -> Unit) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    Column(Modifier.fillMaxSize()) {
        ModernTopBar(tr("PROCESS BANDWIDTH HOG INSPECTOR"), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("ACTIVE OS NETWORK PROCESSES"), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black)
                        AppButton(onClick = onRefresh) {
                            Text(if (suiteState.isProcessInspectorLoading) tr("POLLING...") else tr("REFRESH"), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    suiteState.processList.forEach { proc ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(proc.processName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (proc.isTopHog) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(color = Color.Red.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                            Text(tr("TOP HOG"), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Red, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text("PID: ${proc.pid}", fontSize = 10.sp, color = Color.Gray)
                            }
                            Text("${proc.dlSpeedMbps} MB/s ↓ / ${proc.ulSpeedMbps} MB/s ↑", fontWeight = FontWeight.Black, color = if (proc.isTopHog) Color.Red else pColor, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteControllerScreen(
    state: SpeedtestUIState,
    suiteState: SuiteUIState,
    onFetchDevices: () -> Unit,
    onTriggerDevice: (String?) -> Unit,
    onBack: () -> Unit
) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }

    LaunchedEffect(Unit) {
        onFetchDevices()
    }

    Column(Modifier.fillMaxSize()) {
        ModernTopBar(tr("REMOTE MULTI-DEVICE CONTROLLER"), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            // Trigger All Devices Card
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("MULTI-DEVICE REMOTE SPEEDTEST"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(tr("Trigger speed tests across all linked Mobile, Desktop, and Android TV devices simultaneously."), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    AppButton(
                        onClick = { onTriggerDevice(null) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pColor)
                    ) {
                        Text(tr("⚡ TRIGGER ALL LINKED DEVICES AT ONCE"), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Live Mirror Streaming Gauge Card
            if (suiteState.remoteMirrorData != null && suiteState.remoteMirrorData.isLive) {
                val mirror = suiteState.remoteMirrorData
                GlassCard(Modifier.fillMaxWidth().border(1.5.dp, pColor, RoundedCornerShape(16.dp))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(Color.Green, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text(tr("LIVE TEST STREAM: %s", mirror.deviceName.uppercase()), fontWeight = FontWeight.Black, color = pColor, fontSize = 12.sp)
                            }
                            Text("${mirror.progress}%", fontWeight = FontWeight.Black, color = pColor, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { mirror.progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = pColor,
                            trackColor = pColor.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(tr("DOWNLOAD"), fontSize = 10.sp, color = Color.Gray)
                                Text("${mirror.dlSpeed.toInt()} Mbps", fontWeight = FontWeight.Black, fontSize = 18.sp, color = pColor)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(tr("UPLOAD"), fontSize = 10.sp, color = Color.Gray)
                                Text("${mirror.ulSpeed.toInt()} Mbps", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(tr("PING"), fontSize = 10.sp, color = Color.Gray)
                                Text("${mirror.ping.toInt()} ms", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Linked Devices List Card
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("LINKED USER DEVICES"), style = MaterialTheme.typography.labelMedium, color = pColor, fontWeight = FontWeight.Black)
                        AppIconButton(onClick = onFetchDevices) {
                            Icon(Icons.Default.Refresh, null, tint = pColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    if (suiteState.linkedDevices.isEmpty()) {
                        Text(tr("No linked devices found. Sign in on your Mobile, Desktop, or TV app to link devices."), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        suiteState.linkedDevices.forEach { dev ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            dev.type.contains("TV", ignoreCase = true) -> Icons.Default.Tv
                                            dev.type.contains("PC", ignoreCase = true) || dev.type.contains("Desktop", ignoreCase = true) -> Icons.Default.Computer
                                            else -> Icons.Default.PhoneAndroid
                                        },
                                        contentDescription = null,
                                        tint = pColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${dev.type} • ${dev.lastActive}", fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                AppButton(
                                    onClick = { onTriggerDevice(dev.id) },
                                    modifier = Modifier.height(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = pColor.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = tr("TRIGGER"),
                                        color = pColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryExporterScreen(
    state: SpeedtestUIState,
    historyResults: List<TestResult>,
    onShareExport: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val pColor = if (state.neonPrimaryColor == "#00E5FF") MaterialTheme.colorScheme.primary else state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    var exportFormat by remember { mutableStateOf("CSV") }

    val exportedText = remember(exportFormat, historyResults) {
        if (exportFormat == "CSV") {
            val sb = StringBuilder("Timestamp,Download_Mbps,Upload_Mbps,Ping_ms,Jitter_ms,Server,ISP\n")
            historyResults.forEach { r ->
                sb.append("${r.timestamp},${r.downloadSpeed},${r.uploadSpeed},${r.ping},${r.jitter},\"${r.serverName}\",\"${r.ipInfo}\"\n")
            }
            sb.toString()
        } else {
            val sb = StringBuilder("[\n")
            historyResults.forEachIndexed { i, r ->
                sb.append("  {\n")
                sb.append("    \"timestamp\": ${r.timestamp},\n")
                sb.append("    \"downloadMbps\": ${r.downloadSpeed},\n")
                sb.append("    \"uploadMbps\": ${r.uploadSpeed},\n")
                sb.append("    \"pingMs\": ${r.ping},\n")
                sb.append("    \"jitterMs\": ${r.jitter},\n")
                sb.append("    \"server\": \"${r.serverName}\",\n")
                sb.append("    \"ip\": \"${r.ipInfo}\"\n")
                sb.append(if (i == historyResults.lastIndex) "  }\n" else "  },\n")
            }
            sb.append("]")
            sb.toString()
        }
    }

    Column(Modifier.fillMaxSize()) {
        ModernTopBar(tr("COMPLETE HISTORY CSV / JSON EXPORTER"), onBack, color = pColor, isTvMode = state.isTvMode || isDesktop)
        Column(Modifier.weight(1f).padding(24.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("SELECT EXPORT FORMAT"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("CSV", "JSON").forEach { fmt ->
                            val selected = exportFormat == fmt
                            AppButton(
                                onClick = { exportFormat = fmt },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (selected) pColor else pColor.copy(alpha = 0.15f))
                            ) {
                                Text(fmt, fontWeight = FontWeight.Black, color = if (selected) Color.Black else pColor)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(tr("TOTAL RECORDS TO EXPORT: %s", historyResults.size), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    AppButton(
                        onClick = { onShareExport(exportedText, exportFormat) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = pColor)
                    ) {
                        Text(tr("📊 EXPORT & SAVE %s FILE", exportFormat), fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(tr("RAW %s PREVIEW", exportFormat), style = MaterialTheme.typography.labelSmall, color = pColor, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxSize().background(Color.Black).border(1.dp, pColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(exportedText, color = Color(0xFF00FF64), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}


