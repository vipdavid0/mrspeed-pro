package com.fdossena.speedtest.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.content.ContentResolver
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fdossena.speedtest.data.SettingsManager
import com.fdossena.speedtest.utils.NotificationHelper
import com.mr.speed.test.R
import android.util.Log
import com.fdossena.speedtest.core.SpeedtestManager
import com.fdossena.speedtest.data.AppDatabase
import com.fdossena.speedtest.data.HistoryRepositoryImpl
import com.fdossena.speedtest.data.AndroidSyncManager
import com.fdossena.speedtest.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class NetworkMonitoringService : Service() {

    companion object {
        private const val NOTIF_ID = 999
        private const val CHANNEL_ID = "network_monitor_v9"
    }

    private var isInitializing = true
    private lateinit var connectivityManager: ConnectivityManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        private fun getLastNetworkId(): String? {
            return getSharedPreferences("network_monitoring", MODE_PRIVATE)
                .getString("last_notified_network", null)
        }

        private fun saveLastNetworkId(id: String) {
            getSharedPreferences("network_monitoring", MODE_PRIVATE)
                .edit().putString("last_notified_network", id).apply()
        }

        override fun onAvailable(network: Network) {
            val settingsManager = SettingsManager(applicationContext)
            if (!settingsManager.autoNotifyNetwork) return

            val currentNetworkId = network.toString()
            val lastNetworkId = getLastNetworkId()
            val lastDisconnect = settingsManager.lastDisconnectTime
            val timeSinceDisconnect = System.currentTimeMillis() - lastDisconnect
            val fiveMinutes = 5 * 60 * 1000L

            if (isInitializing) {
                saveLastNetworkId(currentNetworkId)
                isInitializing = false
                return
            }

            val isSameNetwork = currentNetworkId == lastNetworkId
            
            val shouldNotify = if (isSameNetwork) {
                settingsManager.networkNotifyMode == 1 && lastDisconnect > 0 && timeSinceDisconnect > fiveMinutes
            } else {
                true
            }

            if (shouldNotify) {
                saveLastNetworkId(currentNetworkId)
                NotificationHelper(applicationContext).showNetworkAlert()
            }
        }

        override fun onLost(network: Network) {
            SettingsManager(applicationContext).lastDisconnectTime = System.currentTimeMillis()
        }
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        
        try {
            startForeground(NOTIF_ID, createServiceNotification())
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "startForeground error", e)
        }
        
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "registerNetworkCallback error", e)
        }

        setupRemoteTriggerListener()
    }

    private fun executeHttpReq(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private var last2FaReqId: String = ""

    private fun show2FaPushNotification(reqId: String, ip: String) {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel2FaId = "2fa_auth_channel_sound_v2"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/${R.raw.notification}")
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                val channel = NotificationChannel(
                    channel2FaId,
                    "2FA Login Authorization Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Custom sound alert when 2FA login attempts occur"
                    setSound(soundUri, audioAttributes)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    setShowBadge(true)
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("2fa_req_id", reqId)
                putExtra("2fa_ip", ip)
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 2001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val approveIntent = Intent(this, TwoFactorActionReceiver::class.java).apply {
                action = "ACTION_APPROVE_2FA"
                putExtra("reqId", reqId)
            }
            val approvePendingIntent = PendingIntent.getBroadcast(
                this, 2002, approveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val denyIntent = Intent(this, TwoFactorActionReceiver::class.java).apply {
                action = "ACTION_DENY_2FA"
                putExtra("reqId", reqId)
            }
            val denyPendingIntent = PendingIntent.getBroadcast(
                this, 2003, denyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/${R.raw.notification}")

            val notif = NotificationCompat.Builder(this, channel2FaId)
                .setSmallIcon(R.drawable.speed_test)
                .setContentTitle("🛡️ 2FA LOGIN AUTHORIZATION REQUEST")
                .setContentText("New login attempt from $ip. Tap ALLOW or DENY.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_CALL)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setContentIntent(pendingIntent)
                .addAction(0, "✅ ALLOW LOGIN", approvePendingIntent)
                .addAction(0, "⛔ DENY ACCESS", denyPendingIntent)
                .setAutoCancel(true)
                .setFullScreenIntent(pendingIntent, true)
                .build()

            nm.notify(2001, notif)
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Failed to show 2FA push notification: ${e.message}")
        }
    }

    private fun setupRemoteTriggerListener() {
        pollJob?.cancel()
        pollJob = serviceScope.launch {
            val settingsManager = SettingsManager(applicationContext)
            val sync = AndroidSyncManager(settingsManager)
            val apiBaseUrl = "https://mrspeed.ca/backend/api"
            
            while (isActive) {
                try {
                    val uid = sync.getUserId()
                    val devId = sync.getDeviceId()
                    val devName = sync.getDeviceName()
                    
                    if (uid.isNotEmpty()) {
                        // 1. Poll Remote Commands
                        val urlStr = "$apiBaseUrl/remote_command.php?uid=$uid&deviceId=$devId&deviceName=${URLEncoder.encode(devName, "UTF-8")}"
                        val response = executeHttpReq(urlStr)
                        if (response != null && response.trim().startsWith("{")) {
                            try {
                                val json = JSONObject(response)
                                if (json.optBoolean("triggerTest", false)) {
                                    Log.d("NetworkMonitor", "Background speed test triggered remotely!")
                                    runBackgroundRemoteTest(settingsManager, sync, devName)
                                }
                            } catch (t: Throwable) {
                                Log.e("NetworkMonitor", "JSON parse error: ${t.message}")
                            }
                        }

                        // 2. Poll Pending 2FA Push Authorization Requests
                        val email = sync.getUserEmail() ?: ""
                        val twoFactorUrl = "$apiBaseUrl/auth.php?action=check_pending_2fa&uid=$uid&email=${URLEncoder.encode(email, "UTF-8")}"
                        val twoFactorResp = executeHttpReq(twoFactorUrl)
                        if (twoFactorResp != null && twoFactorResp.trim().startsWith("{")) {
                            try {
                                val tfJson = JSONObject(twoFactorResp)
                                if (tfJson.optBoolean("pending", false)) {
                                    val reqId = tfJson.optString("reqId")
                                    val ip = tfJson.optString("ip", "Unknown Location")
                                    if (reqId.isNotEmpty() && reqId != last2FaReqId) {
                                        last2FaReqId = reqId
                                        show2FaPushNotification(reqId, ip)
                                    }
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("NetworkMonitor", "Poll error: ${t.message}")
                }
                delay(5000)
            }
        }
    }

    private fun runBackgroundRemoteTest(settingsManager: SettingsManager, sync: AndroidSyncManager, deviceName: String) {
        serviceScope.launch {
            try {
                val uiModeManager = getSystemService(UI_MODE_SERVICE) as? UiModeManager
                val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                
                val initialBuilder = NotificationCompat.Builder(this@NetworkMonitoringService, CHANNEL_ID)
                    .setSmallIcon(R.drawable.speed_test)
                    .setContentTitle("Remote Speed Test Triggered")
                    .setContentText("Running silent test...")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                
                nm.notify(NOTIF_ID, initialBuilder.build())

                val dbLocal = AppDatabase.getDatabase(applicationContext)
                val repo = HistoryRepositoryImpl(dbLocal.testResultDao())
                
                val mgr = SpeedtestManager(applicationContext, repo, sync, settingsManager)
                val testSource = if (isTv) "TV APP ($deviceName)" else "MOBILE APP ($deviceName)"
                
                mgr.runSilentTest(customSource = testSource) { status, progress ->
                    val progressBuilder = NotificationCompat.Builder(this@NetworkMonitoringService, CHANNEL_ID)
                        .setSmallIcon(R.drawable.speed_test)
                        .setContentTitle("Remote Speed Test In Progress")
                        .setContentText(status)
                        .setProgress(100, progress, false)
                        .setOngoing(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                    
                    nm.notify(NOTIF_ID, progressBuilder.build())
                }
                
                nm.notify(NOTIF_ID, createServiceNotification())
                
                val resultNotif = NotificationCompat.Builder(this@NetworkMonitoringService, CHANNEL_ID)
                    .setSmallIcon(R.drawable.speed_test)
                    .setContentTitle("Remote Test Complete")
                    .setContentText("Check dashboard for detailed results.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                nm.notify(1002, resultNotif)
            } catch (e: Exception) {
                Log.e("NetworkMonitor", "Background remote test failed: ${e.message}")
            }
        }
    }

    private fun createServiceNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mr Speed Pro Suite",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mr Speed Pro Engine Active")
            .setContentText("Listening for remote commands & network shifts.")
            .setSmallIcon(R.drawable.speed_test)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIF_ID, createServiceNotification())
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "onStartCommand startForeground error", e)
        }
        setupRemoteTriggerListener()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {}
        pollJob?.cancel()
    }
}
