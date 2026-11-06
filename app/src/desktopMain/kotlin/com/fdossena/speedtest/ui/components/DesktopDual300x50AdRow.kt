package com.fdossena.speedtest.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import org.jetbrains.skia.Image as SkiaImage

data class SingleAdUnitData(
    val id: Int = 0,
    val title: String = "Mr Speed PRO | Ad-Free Speed Tests",
    val imageUrl: String = "",
    val targetUrl: String = "https://www.mrspeed.ca/pricing.php"
)

@Composable
fun SingleAdUnit(
    adData: SingleAdUnitData,
    isLightMode: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isLightMode) Color(0xFFFF9F0A) else Color(0xFF00E5FF)
    val bgColor = if (isLightMode) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val textColor = if (isLightMode) Color(0xFF111115) else Color(0xFFFFFFFF)
    val subTextColor = if (isLightMode) Color(0xFFD97706) else Color(0xFF00E5FF)

    var imageBitmap by remember(adData.imageUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(adData.imageUrl) {
        if (adData.imageUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val cleanImgPath = adData.imageUrl.removePrefix("/")
                val candidateUrls = if (adData.imageUrl.startsWith("http")) {
                    listOf(adData.imageUrl)
                } else {
                    listOf(
                        "https://www.mrspeed.ca/" + cleanImgPath,
                        "https://www.mrspeed.ca/backend/" + cleanImgPath,
                        "https://www.mrspeed.ca/backend/api/" + cleanImgPath
                    )
                }

                for (fullImgUrl in candidateUrls) {
                    try {
                        println("[DEBUG FETCHING AD IMAGE]: $fullImgUrl")
                        val url = URL(fullImgUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 4000
                        conn.readTimeout = 4000
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                        conn.setRequestProperty("Accept", "image/webp,image/apng,image/png,image/*,*/*;q=0.8")
                        if (conn.responseCode in 200..299) {
                            val bytes = conn.inputStream.use { it.readBytes() }
                            val skiaImage = SkiaImage.makeFromEncoded(bytes)
                            imageBitmap = skiaImage.toComposeImageBitmap()
                            println("[DEBUG AD IMAGE LOAD SUCCESS]: ${skiaImage.width}x${skiaImage.height} from $fullImgUrl")
                            break
                        } else {
                            println("[DEBUG AD IMAGE HTTP ERROR ${conn.responseCode}]: $fullImgUrl")
                        }
                    } catch (e: Exception) {
                        println("[DEBUG AD IMAGE LOAD ERROR]: ${e.message} for $fullImgUrl")
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .width(300.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, primaryColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
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
            },
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = adData.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        } else {
            // Fallback Text Ad UI
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SPONSORED AD",
                        color = subTextColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = adData.title.ifEmpty { "Mr Speed PRO | Ad-Free Speed Tests" },
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VIEW ➔",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopDual300x50AdRow(
    state: SpeedtestUIState,
    isLightMode: Boolean,
    modifier: Modifier = Modifier
) {
    // 1. Paid plan exclusion: Ads excluded for users on paid / pro / ad-free plans
    val isPaidUser = state.isProUser || (state.userPlan.lowercase() != "free" && state.userPlan.lowercase() != "free_plan" && state.userPlan.isNotEmpty())
    if (isPaidUser) return

    var ad1 by remember { mutableStateOf(SingleAdUnitData(1, "Mr Speed PRO | Ad-Free Speed Tests", "", "https://www.mrspeed.ca/pricing.php")) }
    var ad2 by remember { mutableStateOf(SingleAdUnitData(2, "Supreme Host | High Speed Cloud Infrastructure", "", "https://www.mrspeed.ca/")) }

    // 2. Ad Refresh on Page Navigation / Screen Change (Updated on Main Thread)
    LaunchedEffect(Unit, state.screen) {
        if (!state.testRunning) {
            var fetchedAd1: SingleAdUnitData? = null
            var fetchedAd2: SingleAdUnitData? = null

            withContext(Dispatchers.IO) {
                try {
                    val url1 = URL("https://www.mrspeed.ca/backend/api/ad_click.php?action=get_ad&size=300x50&slot=1")
                    val conn1 = url1.openConnection() as HttpURLConnection
                    conn1.connectTimeout = 4000
                    conn1.readTimeout = 4000
                    conn1.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    val text1 = conn1.inputStream.bufferedReader().use { it.readText() }
                    println("[DEBUG AD 1 RESPONSE]: $text1")
                    val json1 = JSONObject(text1)
                    val obj1 = json1.optJSONObject("ad") ?: json1.optJSONArray("ad")?.optJSONObject(0)
                    if (obj1 != null || json1.has("title")) {
                        val title: String = obj1?.optString("title") ?: json1.optString("title") ?: ""
                        val imageUrl: String = obj1?.optString("image_url") ?: json1.optString("imageUrl") ?: ""
                        val targetUrl: String = obj1?.optString("target_url") ?: json1.optString("targetUrl") ?: "https://www.mrspeed.ca/pricing.php"
                        val id: Int = obj1?.optInt("id") ?: json1.optInt("id") ?: 0
                        if (title.isNotEmpty() || imageUrl.isNotEmpty()) {
                            fetchedAd1 = SingleAdUnitData(id = id, title = title, imageUrl = imageUrl, targetUrl = targetUrl)
                        }
                    }
                } catch (e: Exception) {
                    println("[DEBUG AD 1 ERROR]: ${e.message}")
                }

                try {
                    val url2 = URL("https://www.mrspeed.ca/backend/api/ad_click.php?action=get_ad&size=300x50&slot=2")
                    val conn2 = url2.openConnection() as HttpURLConnection
                    conn2.connectTimeout = 4000
                    conn2.readTimeout = 4000
                    conn2.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    val text2 = conn2.inputStream.bufferedReader().use { it.readText() }
                    println("[DEBUG AD 2 RESPONSE]: $text2")
                    val json2 = JSONObject(text2)
                    val obj2 = json2.optJSONObject("ad") ?: json2.optJSONArray("ad")?.optJSONObject(0)
                    if (obj2 != null || json2.has("title")) {
                        val title: String = obj2?.optString("title") ?: json2.optString("title") ?: ""
                        val imageUrl: String = obj2?.optString("image_url") ?: json2.optString("imageUrl") ?: ""
                        val targetUrl: String = obj2?.optString("target_url") ?: json2.optString("targetUrl") ?: "https://www.mrspeed.ca/"
                        val id: Int = obj2?.optInt("id") ?: json2.optInt("id") ?: 0
                        if (title.isNotEmpty() || imageUrl.isNotEmpty()) {
                            fetchedAd2 = SingleAdUnitData(id = id, title = title, imageUrl = imageUrl, targetUrl = targetUrl)
                        }
                    }
                } catch (e: Exception) {
                    println("[DEBUG AD 2 ERROR]: ${e.message}")
                }
            }

            withContext(Dispatchers.Main) {
                val houseAds = listOf(
                    SingleAdUnitData(1, "Mr Speed PRO | Ad-Free Speed Tests", "", "https://www.mrspeed.ca/pricing.php"),
                    SingleAdUnitData(2, "Supreme Host | High Speed Cloud Infrastructure", "", "https://www.mrspeed.ca/")
                )
                ad1 = fetchedAd1 ?: houseAds.random()
                ad2 = fetchedAd2 ?: houseAds.filter { it.id != ad1.id }.random()
            }
        }
    }

    // Theme-matching background color for Ad Bar Container
    val barBg = if (isLightMode) Color(0xFFF0F2F8) else Color(0xFF08080E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(barBg)
            .padding(vertical = 6.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SingleAdUnit(adData = ad1, isLightMode = isLightMode)
            Spacer(modifier = Modifier.width(16.dp))
            SingleAdUnit(adData = ad2, isLightMode = isLightMode)
        }
    }
}
