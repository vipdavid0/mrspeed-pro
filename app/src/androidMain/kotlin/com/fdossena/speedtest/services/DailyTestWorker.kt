package com.fdossena.speedtest.services

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fdossena.speedtest.core.SpeedtestManager
import com.fdossena.speedtest.data.*

class DailyTestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsManager(applicationContext)
        if (!settings.autoDailyTestEnabled) return Result.success()

        // Battery Check Logic
        val batteryStatus: Intent? = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()

        // Only run if charging OR battery > 20%
        if (!isCharging && batteryPct <= 20) {
            return Result.success() // Skip this run to save power
        }

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = HistoryRepositoryImpl(db.testResultDao())
        val syncManager = AndroidSyncManager(settings)
        
        // Initialize sync (anonymous sign in if needed)
        syncManager.initialize()

        val speedtestManager = SpeedtestManager(applicationContext, repository, syncManager, settings)
        speedtestManager.runSilentTest()

        return Result.success()
    }
}
