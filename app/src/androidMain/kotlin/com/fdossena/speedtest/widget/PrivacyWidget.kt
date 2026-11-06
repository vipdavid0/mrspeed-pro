package com.fdossena.speedtest.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class PrivacyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isp = prefs.getString("last_widget_isp", "Unknown Provider") ?: "Unknown Provider"
        val ip = prefs.getString("last_widget_ip", "Detecting...") ?: "Detecting..."
        val protected = prefs.getBoolean("privacy_protected", true)

        provideContent {
            PrivacyWidgetContent(isp, ip, protected)
        }
    }

    @Composable
    private fun PrivacyWidgetContent(isp: String, ip: String, protected: Boolean) {
        val cyan = Color(0xFF00E5FF)
        val green = Color(0xFF4CAF50)
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF08080E))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MR SPEED PRIVACY",
                style = TextStyle(color = ColorProvider(cyan), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Text(
                text = isp,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = ip,
                style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.6f)), fontSize = 12.sp)
            )
            
            Spacer(modifier = GlanceModifier.height(12.dp))
            
            Box(
                modifier = GlanceModifier
                    .background(if (protected) green.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (protected) "PRIVACY PROTECTED" else "UNPROTECTED",
                    style = TextStyle(color = ColorProvider(if (protected) green else Color.Red), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

class PrivacyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrivacyWidget()
}
