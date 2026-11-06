package com.fdossena.speedtest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdossena.speedtest.data.TestResult
import com.fdossena.speedtest.ui.models.SpeedtestUIState
import com.fdossena.speedtest.ui.models.DuelPhase
import com.fdossena.speedtest.utils.*

@Composable
fun DuelScreen(
    state: SpeedtestUIState,
    onStartWifi: () -> Unit,
    onStartMobile: () -> Unit,
    onGoHome: () -> Unit
) {
    val pColor = state.neonPrimaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.primary }
    val sColor = state.neonSecondaryColor.toColor().takeOrElse { MaterialTheme.colorScheme.secondary }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ModernTopBar("NETWORK DUEL", onGoHome, color = pColor)
        
        Spacer(Modifier.height(24.dp))
        
        Row(Modifier.fillMaxWidth().weight(1f)) {
            DuelResultCard("WI-FI", state.duelWifiResult, pColor, Modifier.weight(1f), onStartWifi, state.duelPhase == DuelPhase.TestingWifi)
            Spacer(Modifier.width(16.dp))
            DuelResultCard("5G / MOBILE", state.duelMobileResult, sColor, Modifier.weight(1f), onStartMobile, state.duelPhase == DuelPhase.TestingMobile)
        }
        
        if (state.duelPhase == DuelPhase.Finished) {
            WinnerBanner(state.duelWifiResult, state.duelMobileResult, pColor, sColor)
        }
        
        Spacer(Modifier.height(24.dp))
        
        AppButton(
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Text("DONE", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DuelResultCard(
    label: String,
    result: TestResult?,
    color: Color,
    modifier: Modifier,
    onStart: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = color, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))
        
        if (isLoading) {
            CircularProgressIndicator(color = color, modifier = Modifier.size(48.dp))
        } else if (result != null) {
            Text(formatSpeed(result.downloadSpeed), color = color, fontSize = 36.sp, fontWeight = FontWeight.Black)
            Text("Mbps", color = color.copy(alpha = 0.5f), fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Text("Ping: ${result.ping.toInt()}ms", color = Color.Gray, fontSize = 14.sp)
        } else {
            AppIconButton(
                onClick = onStart, 
                modifier = Modifier.size(80.dp).background(color.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = if (label == "WI-FI") Icons.Default.Refresh else Icons.Default.Star, 
                    contentDescription = null, 
                    tint = color,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun WinnerBanner(wifi: TestResult?, mobile: TestResult?, pColor: Color, sColor: Color) {
    val wifiSpeed = wifi?.downloadSpeed ?: 0.0
    val mobileSpeed = mobile?.downloadSpeed ?: 0.0
    val winner = if (wifiSpeed > mobileSpeed) "WI-FI WINS!" else if (mobileSpeed > wifiSpeed) "MOBILE WINS!" else "IT'S A TIE!"
    val color = if (wifiSpeed > mobileSpeed) pColor else if (mobileSpeed > wifiSpeed) sColor else Color.White
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(winner, color = color, fontWeight = FontWeight.Black, fontSize = 28.sp)
    }
}
