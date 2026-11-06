package com.fdossena.speedtest.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fdossena.speedtest.ui.MainActivity

class SpeedtestWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lastDl = prefs.getString("last_widget_dl", "0.0")
        val lastUl = prefs.getString("last_widget_ul", "0.0")
        val lastPing = prefs.getString("last_widget_ping", "0")
        val lastHealth = prefs.getInt("last_widget_health", 0)

        provideContent {
            SpeedtestWidgetContent(lastDl ?: "0.0", lastUl ?: "0.0", lastPing ?: "0", lastHealth)
        }
    }

    @Composable
    private fun SpeedtestWidgetContent(dl: String, ul: String, ping: String, health: Int) {
        val cyan = Color(0xFF00E5FF)
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF08080E))
                .padding(12.dp)
                // Fixed the click action to ensure MainActivity is properly referenced and launched.
                .clickable(actionStartActivity<MainActivity>()), 
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mr",
                    style = TextStyle(color = ColorProvider(cyan), fontSize = 16.sp, fontWeight = FontWeight.Normal)
                )
                Spacer(modifier = GlanceModifier.width(2.dp))
                Text(
                    text = "Speed",
                    style = TextStyle(color = ColorProvider(cyan), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
                
                if (health > 0) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Box(modifier = GlanceModifier.background(Color(0xFFE5E4E2).copy(alpha = 0.2f)).padding(horizontal = 4.dp)) {
                        Text(
                            text = "HEALTH: $health%",
                            style = TextStyle(color = ColorProvider(Color(0xFFE5E4E2)), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Box(modifier = GlanceModifier.defaultWeight().padding(4.dp), contentAlignment = Alignment.Center) {
                    WidgetStatItem("DL", dl, cyan)
                }
                Box(modifier = GlanceModifier.defaultWeight().padding(4.dp), contentAlignment = Alignment.Center) {
                    WidgetStatItem("UL", ul, Color(0xFF5E5CE6))
                }
                Box(modifier = GlanceModifier.defaultWeight().padding(4.dp), contentAlignment = Alignment.Center) {
                    WidgetStatItem("PING", ping, Color(0xFFFF9F0A))
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Box(
                modifier = GlanceModifier
                    .background(cyan)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "START TEST",
                    style = TextStyle(color = ColorProvider(Color.Black), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    @Composable
    private fun WidgetStatItem(label: String, value: String, color: Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.5f)), fontSize = 10.sp)
            )
            Text(
                text = value,
                style = TextStyle(color = ColorProvider(color), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

class SpeedtestWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpeedtestWidget()
}