package com.fdossena.speedtest

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.app.UiModeManager
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import com.fdossena.speedtest.data.SettingsManager
import com.fdossena.speedtest.services.DailyTestWorker
import com.fdossena.speedtest.services.MrDetectorWorker
import com.fdossena.speedtest.services.NetworkMonitoringService
import com.fdossena.speedtest.utils.NotificationHelper
import com.fdossena.speedtest.utils.TvChannelHelper
import com.mr.speed.test.R
import java.util.*
import java.util.concurrent.TimeUnit

class SpeedtestApplication : Application() {

    companion object {
        lateinit var instance: SpeedtestApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        updateMonitoringService()
        updateDailyRoutine()
        scheduleMrDetector()
        
        // Safely update TV channels in background after a short delay to ensure system readiness
        Thread {
            try {
                Thread.sleep(15000) // 15s delay for system services to settle
                val uiModeManager = getSystemService(UI_MODE_SERVICE) as? UiModeManager
                val isTv = uiModeManager?.let { 
                    it.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
                } ?: packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                
                if (isTv) {
                    Log.d("MrSpeed", "Bootstrapping Fire TV Home Screen Channels...")
                    TvChannelHelper.updateChannel(this)
                }
            } catch (e: Exception) {
                Log.e("MrSpeed", "TvChannel update failed during boot", e)
            }
        }.start()
    }

    fun scheduleMrDetector() {
        val workManager = WorkManager.getInstance(this)
        val detectorRequest = PeriodicWorkRequestBuilder<MrDetectorWorker>(
            15, TimeUnit.MINUTES 
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        ).setBackoffCriteria(
            BackoffPolicy.LINEAR,
            WorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "MrDetectorBackgroundCheck",
            ExistingPeriodicWorkPolicy.UPDATE, 
            detectorRequest
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notif_channel_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NotificationHelper.CHANNEL_ID, name, importance).apply {
                description = "Alerts when a new network connection is detected"
                
                val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/${R.raw.notification}")
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateDailyRoutine() {
        val settings = SettingsManager(applicationContext)
        val workManager = WorkManager.getInstance(this)
        
        if (!settings.autoDailyTestEnabled) {
            workManager.cancelUniqueWork("DailySpeedTest")
            return
        }

        val timeParts = settings.autoDailyTestTime.split(":")
        val targetHour = timeParts[0].toInt()
        val targetMinute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

        val dailyRequest = PeriodicWorkRequestBuilder<DailyTestWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(if (settings.autoDailyTestWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailySpeedTest",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyRequest
        )
    }

    fun updateMonitoringService() {
        val intent = Intent(this, NetworkMonitoringService::class.java)
        // Service now handles its own startForeground
        ContextCompat.startForegroundService(this, intent)
    }
}
