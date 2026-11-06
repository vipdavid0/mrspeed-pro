package com.fdossena.speedtest.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import com.fdossena.speedtest.SpeedtestApplication

class NetworkMonitorBootWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        try {
            val intent = Intent(applicationContext, NetworkMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("NetworkMonitorBootWorker", "Failed to start service: ${e.message}")
        }
        return Result.success()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received boot broadcast action: $action")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            try {
                val workRequest = OneTimeWorkRequestBuilder<NetworkMonitorBootWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "NetworkMonitorBootWork",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to enqueue WorkManager on boot: ${e.message}")
            }

            try {
                val serviceIntent = Intent(context, NetworkMonitoringService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to start NetworkMonitoringService on boot: ${e.message}")
            }

            val app = context.applicationContext as? SpeedtestApplication
            app?.updateMonitoringService()
            app?.scheduleMrDetector()
        }
    }
}
