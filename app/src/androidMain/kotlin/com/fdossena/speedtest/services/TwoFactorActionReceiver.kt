package com.fdossena.speedtest.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.fdossena.speedtest.data.AndroidSyncManager
import com.fdossena.speedtest.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TwoFactorActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val reqId = intent.getStringExtra("reqId") ?: return
        val action = intent.action ?: return

        NotificationManagerCompat.from(context).cancel(2001)

        val isApproved = (action == "ACTION_APPROVE_2FA")
        val settings = SettingsManager(context)
        val sync = AndroidSyncManager(settings)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                sync.approve2Fa(reqId, isApproved)
                launch(Dispatchers.Main) {
                    val msg = if (isApproved) "🛡️ 2FA Login Approved!" else "⛔ 2FA Login Denied!"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Throwable) {}
        }
    }
}
