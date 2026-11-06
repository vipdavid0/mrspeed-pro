package com.fdossena.speedtest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdossena.speedtest.ui.models.SpeedtestUIState
import com.fdossena.speedtest.utils.cursorHoverable
import com.fdossena.speedtest.utils.openUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class DesktopAdData(
    val id: Int = 0,
    val title: String = "Mr Speed PRO | Upgrade For Ad-Free Speed Tests & Pro Diagnostics",
    val targetUrl: String = "https://www.mrspeed.ca/pricing.php"
)

@Composable
fun Desktop300x50AdBanner(
    state: SpeedtestUIState,
    isLightMode: Boolean,
    modifier: Modifier = Modifier
) {
    // Exclude ads for paid/pro plan users
    val isPaidUser = state.isProUser || (state.userPlan.lowercase() != "free" && state.userPlan.lowercase() != "free_plan" && state.userPlan.isNotEmpty())
    if (isPaidUser) return

    var adData by remember { mutableStateOf(DesktopAdData()) }

    // Fetch active backup/house ad from backend API
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://www.mrspeed.ca/backend/api/ad_click.php?action=get_ad&size=300x50")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                if (json.optBoolean("success")) {
                    val adObj = json.optJSONObject("ad")
                    if (adObj != null) {
                        adData = DesktopAdData(
                            id = adObj.optInt("id", 0),
                            title = adObj.optString("title", "Mr Speed PRO | Upgrade For Ad-Free Diagnostics"),
                            targetUrl = adObj.optString("target_url", "https://www.mrspeed.ca/pricing.php")
                        )
                    }
                }
            } catch (_: Exception) {
                // Fallback to default Mr Speed Pro House Ad
            }
        }
    }

    val primaryColor = if (isLightMode) Color(0xFFFF9F0A) else Color(0xFF00E5FF)
    val bgColor = if (isLightMode) Color(0xFFF0F2F8) else Color(0xFF080C14)
    val textColor = if (isLightMode) Color(0xFF111115) else Color(0xFFFFFFFF)
    val subTextColor = if (isLightMode) Color(0xFFD97706) else Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .width(300.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        bgColor,
                        bgColor.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.12f)
                    )
                )
            )
            .border(1.5.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .cursorHoverable()
            .clickable {
                try {
                    val clickUrl = if (adData.id > 0) {
                        "https://www.mrspeed.ca/backend/api/ad_click.php?id=${adData.id}&url=${adData.targetUrl}"
                    } else {
                        adData.targetUrl
                    }
                    openUrl(clickUrl)
                } catch (_: Exception) {}
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(primaryColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SPONSORED AD",
                        color = subTextColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = adData.title,
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "VIEW ➔",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
