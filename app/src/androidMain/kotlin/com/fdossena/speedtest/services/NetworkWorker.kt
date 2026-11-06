package com.fdossena.speedtest.services

import android.content.Context
import android.net.ConnectivityManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fdossena.speedtest.data.SettingsManager
import com.fdossena.speedtest.utils.NotificationHelper

class NetworkWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val settingsManager = SettingsManager(applicationContext)
        if (!settingsManager.autoNotifyNetwork) return Result.success()

        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return Result.success()
        val currentNetworkId = network.toString()

        val sharedPrefs = applicationContext.getSharedPreferences("network_tracking", Context.MODE_PRIVATE)
        val lastNetworkId = sharedPrefs.getString("last_network_id", null)
        
        val isNewNetwork = currentNetworkId != lastNetworkId
        val timeSinceDisconnect = System.currentTimeMillis() - settingsManager.lastDisconnectTime
        val fiveMinutes = 5 * 60 * 1000L
        
        // LOGIC: 
        // 1. If it's a DIFFERENT network from the last one we notified about -> Fire immediately.
        // 2. If it's the SAME network (reconnect) -> Fire only if disconnected for > 5 minutes.
        val shouldNotify = if (isNewNetwork) {
            true
        } else {
            // Check reconnect gap if "Smart Reconnect" mode is on
            settingsManager.networkNotifyMode == 1 && timeSinceDisconnect > fiveMinutes
        }

        if (shouldNotify) {
            sharedPrefs.edit().putString("last_network_id", currentNetworkId).apply()
            NotificationHelper(applicationContext).showNetworkAlert()
        }

        return Result.success()
    }
}
