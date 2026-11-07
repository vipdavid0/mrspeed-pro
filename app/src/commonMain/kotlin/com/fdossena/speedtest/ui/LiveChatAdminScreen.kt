package com.fdossena.speedtest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdossena.speedtest.ui.models.SpeedtestUIState
import com.fdossena.speedtest.ui.DesktopLazyScrollbar
import com.fdossena.speedtest.utils.appClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AdminChatSession(
    val sessionId: String,
    val userName: String,
    val userEmail: String,
    val ipAddress: String,
    val status: String,
    val lastMessage: String,
    val currentPage: String,
    val updatedAt: String
)

data class AdminChatMessage(
    val id: Int,
    val senderType: String,
    val senderName: String,
    val senderAvatar: String,
    val messageText: String,
    val createdAt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChatAdminScreen(
    state: SpeedtestUIState,
    onGoHome: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var sessionsList by remember { mutableStateOf<List<AdminChatSession>>(emptyList()) }
    var selectedSessionId by remember { mutableStateOf<String?>(state.pendingAdminChatSessionId?.takeIf { it.isNotEmpty() }) }
    var messagesList by remember { mutableStateOf<List<AdminChatMessage>>(emptyList()) }
    var myAgentStatus by remember { mutableStateOf("ONLINE") }
    var statusFilter by remember { mutableStateOf("AGENT_REQUESTED") }
    var messageInput by remember { mutableStateOf("") }
    var accountDetailsText by remember { mutableStateOf("Loading user account details...") }

    val defaultPrimary = if (state.themeMode == 1) Color(0xFFFF9F0A) else Color(0xFF00E5FF)
    val pColor = if ((state.neonPrimaryColor == "#00E5FF" || state.neonPrimaryColor.isEmpty()) && state.themeMode == 1) {
        Color(0xFFFF9F0A)
    } else {
        state.neonPrimaryColor.toColor().takeOrElse { defaultPrimary }
    }

    val isLight = state.themeMode == 1 || (state.themeMode == 0 && !androidx.compose.foundation.isSystemInDarkTheme())
    val screenBg = if (isLight) Color(0xFFF4F4F8) else Color(0xFF08080E)
    val cardBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF12121C)
    val cardBorder = if (isLight) Color(0xFFE2E2E8) else Color(0xFF222233)
    val textColor = if (isLight) Color(0xFF111115) else Color.White
    val transcriptBg = if (isLight) Color(0xFFEFEFF4) else Color(0xFF0A0A12)
    val subCardBg = if (isLight) Color(0xFFF8F9FC) else Color(0xFF1A1A28)
    val textMuted = if (isLight) Color(0xFF556677) else Color(0xFF8899A6)

    val adminEmail = state.userEmail ?: "admin@mrspeed.ca"
    val isDesktop = com.fdossena.speedtest.utils.isDesktop

    suspend fun fetchSessions() {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://mrspeed.ca/backend/api/livechat.php?action=admin_get_sessions&status=$statusFilter&admin_uid=$adminEmail")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    val arr = json.optJSONArray("sessions") ?: JSONArray()
                    val list = mutableListOf<AdminChatSession>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            AdminChatSession(
                                sessionId = obj.optString("session_id", ""),
                                userName = obj.optString("user_name", "Visitor"),
                                userEmail = obj.optString("user_email", ""),
                                ipAddress = obj.optString("ip_address", ""),
                                status = obj.optString("status", "AI"),
                                lastMessage = obj.optString("last_message", ""),
                                currentPage = obj.optString("current_page", ""),
                                updatedAt = obj.optString("updated_at", "")
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        sessionsList = list
                        if (isDesktop && selectedSessionId == null && list.isNotEmpty()) {
                            selectedSessionId = list[0].sessionId
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun pollMessages(sessionId: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://mrspeed.ca/backend/api/livechat.php?action=get_messages&session_id=$sessionId")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    val arr = json.optJSONArray("messages") ?: JSONArray()
                    val list = mutableListOf<AdminChatMessage>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            AdminChatMessage(
                                id = obj.optInt("id", 0),
                                senderType = obj.optString("sender_type", "user"),
                                senderName = obj.optString("sender_name", "Visitor"),
                                senderAvatar = obj.optString("sender_avatar", ""),
                                messageText = obj.optString("message_text", ""),
                                createdAt = obj.optString("created_at", "")
                            )
                        )
                    }
                    val userObj = json.optJSONObject("user_details")
                    if (userObj != null) {
                        val name = userObj.optString("name", "Unknown")
                        val email = userObj.optString("email", "Anonymous")
                        val plan = userObj.optString("plan", "free")
                        val role = userObj.optString("role", "user")
                        val ip = userObj.optString("last_ip", "")
                        withContext(Dispatchers.Main) {
                            accountDetailsText = "User: $name ($email) | Plan: $plan | Role: $role | IP: $ip"
                        }
                    }
                    withContext(Dispatchers.Main) {
                        messagesList = list
                    }
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(statusFilter) {
        fetchSessions()
    }

    LaunchedEffect(selectedSessionId) {
        if (selectedSessionId != null) {
            pollMessages(selectedSessionId!!)
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            fetchSessions()
            if (selectedSessionId != null) {
                pollMessages(selectedSessionId!!)
            }
            delay(3000)
        }
    }

    fun setAgentStatus(status: String) {
        myAgentStatus = status
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://mrspeed.ca/backend/api/livechat.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                val payload = JSONObject().put("action", "set_agent_status").put("admin_uid", adminEmail).put("status", status)
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                conn.inputStream.bufferedReader().use { it.readText() }
            } catch (_: Exception) {}
        }
    }

    fun performAgentAction(actionName: String, payload: JSONObject) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://mrspeed.ca/backend/api/livechat.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                payload.put("action", actionName).put("admin_uid", adminEmail)
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                conn.inputStream.bufferedReader().use { it.readText() }
                fetchSessions()
                if (selectedSessionId != null) pollMessages(selectedSessionId!!)
            } catch (_: Exception) {}
        }
    }

    fun sendMessage() {
        val text = messageInput.trim()
        val sid = selectedSessionId
        if (text.isEmpty() || sid == null) return
        messageInput = ""
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://mrspeed.ca/backend/api/livechat.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                val payload = JSONObject()
                    .put("action", "send_message")
                    .put("session_id", sid)
                    .put("sender_type", "agent")
                    .put("sender_name", "Support Agent")
                    .put("message_text", text)
                    .put("admin_uid", adminEmail)
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                conn.inputStream.bufferedReader().use { it.readText() }
                pollMessages(sid)
            } catch (_: Exception) {}
        }
    }

    val selectedSession = sessionsList.firstOrNull { item -> item.sessionId == selectedSessionId }

    Box(Modifier.fillMaxSize().background(screenBg).statusBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                Modifier.fillMaxWidth()
                    .background(cardBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (!isDesktop && selectedSessionId != null) {
                            selectedSessionId = null
                        } else {
                            onGoHome()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = pColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (!isDesktop && selectedSessionId != null) "💬 ${selectedSession?.userName ?: "Chat"}" else "💬 ADMIN CHAT DASHBOARD",
                        color = pColor,
                        fontWeight = FontWeight.Black,
                        fontSize = if (!isDesktop && selectedSessionId != null) 14.sp else 16.sp
                    )
                }
                IconButton(onClick = { scope.launch { fetchSessions() } }) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = pColor)
                }
            }

            // Agent Availability Status Row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("MY STATUS:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                listOf("ONLINE" to "🟢 ONLINE", "AWAY" to "🟡 AWAY", "OFFLINE" to "🔴 OFFLINE").forEach { (key, label) ->
                    FilterChip(
                        selected = (myAgentStatus == key),
                        onClick = { setAgentStatus(key) },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (key == "ONLINE") Color(0xFF00FF64) else if (key == "AWAY") Color(0xFFFF9F0A) else Color(0xFFFF3232),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Status Filter Chips
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("AGENT_REQUESTED" to "🙋 Requested", "AGENT_ACTIVE" to "👨‍💻 Active", "AI" to "🤖 AI", "ALL" to "All").forEach { (key, label) ->
                    FilterChip(
                        selected = (statusFilter == key),
                        onClick = { statusFilter = key },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = pColor,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            if (isDesktop) {
                // Desktop 2-Column Split
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Column 1: Sessions List
                    Column(Modifier.weight(0.35f).fillMaxHeight().background(cardBg, RoundedCornerShape(12.dp)).border(1.dp, cardBorder, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text("SESSIONS (${sessionsList.size})", color = pColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))

                        if (sessionsList.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No sessions found", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            val sessionsListState = rememberLazyListState()
                            Box(Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = sessionsListState,
                                    modifier = Modifier.fillMaxSize().padding(end = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(sessionsList) { item ->
                                        val isSelected = (item.sessionId == selectedSessionId)
                                        val bg = if (isSelected) pColor.copy(alpha = 0.2f) else subCardBg
                                        val borderC = if (isSelected) pColor else cardBorder

                                        Column(
                                            Modifier.fillMaxWidth().background(bg, RoundedCornerShape(8.dp)).border(1.dp, borderC, RoundedCornerShape(8.dp)).appClickable { selectedSessionId = item.sessionId }.padding(10.dp)
                                        ) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(item.userName, fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)
                                                Text(item.status, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (item.status == "AGENT_REQUESTED") Color.Yellow else pColor)
                                            }
                                            Text(item.userEmail.ifEmpty { item.ipAddress }, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                DesktopLazyScrollbar(
                                    lazyListState = sessionsListState,
                                    isLight = state.themeMode == 1,
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Column 2: Active Session Transcript & Inspector
                    Column(Modifier.weight(0.65f).fillMaxHeight().background(cardBg, RoundedCornerShape(12.dp)).border(1.dp, cardBorder, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        if (selectedSessionId == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select a session on the left to start responding", color = Color.Gray, fontSize = 14.sp)
                            }
                        } else {
                            // Session Header & Action Buttons
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(selectedSession?.userName ?: "Visitor", fontWeight = FontWeight.Black, color = textColor, fontSize = 16.sp)
                                    Text("🌐 Current Page: ${selectedSession?.currentPage?.ifEmpty { "Homepage" } ?: "Homepage"}", color = pColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { performAgentAction("admin_takeover", JSONObject().put("session_id", selectedSessionId)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = pColor, contentColor = Color.Black)
                                    ) {
                                        Text("⚡ Take Over", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { performAgentAction("admin_transfer_ai", JSONObject().put("session_id", selectedSessionId)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)
                                    ) {
                                        Text("🤖 AI", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { performAgentAction("admin_close_session", JSONObject().put("session_id", selectedSessionId)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)
                                    ) {
                                        Text("🔒 Close", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val sid = selectedSessionId
                                            if (sid != null) {
                                                performAgentAction("admin_delete_session", JSONObject().put("session_id", sid))
                                                selectedSessionId = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3232), contentColor = Color.White)
                                    ) {
                                        Text("🗑️ Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Live User Inspector Box
                            Column(Modifier.fillMaxWidth().background(subCardBg, RoundedCornerShape(8.dp)).border(1.dp, pColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(accountDetailsText, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(Modifier.height(8.dp))

                            // Transcript Stream
                            val listState = rememberLazyListState()
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize().background(transcriptBg, RoundedCornerShape(8.dp)).padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(messagesList) { m ->
                                        val isUser = (m.senderType == "user")
                                        val isAgent = (m.senderType == "agent")
                                        val isAi = (m.senderType == "ai")
                                        val msgBg = if (isUser) pColor.copy(alpha = 0.15f) else if (isAgent) Color(0xFFFF9F0A).copy(alpha = 0.2f) else if (isLight) Color(0xFFE2E2E8) else Color(0xFF1E1E2C)

                                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                            Column(Modifier.weight(1f).background(msgBg, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text(m.senderName, fontWeight = FontWeight.Black, fontSize = 10.sp, color = if (isUser) pColor else Color.Yellow)
                                                    if (isAi) {
                                                        Text("🤖 AI", fontSize = 9.sp, color = pColor, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(m.messageText, color = textColor, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                                DesktopLazyScrollbar(
                                    lazyListState = listState,
                                    isLight = state.themeMode == 1,
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 2.dp)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Input Bar
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = messageInput,
                                    onValueChange = { messageInput = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Type reply...", color = Color.Gray, fontSize = 12.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = pColor,
                                        unfocusedBorderColor = cardBorder,
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    singleLine = true
                                )
                                Button(
                                    onClick = { sendMessage() },
                                    colors = ButtonDefaults.buttonColors(containerColor = pColor, contentColor = Color.Black)
                                ) {
                                    Text("SEND", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // Mobile View
                if (selectedSessionId == null) {
                    // Full-screen Sessions List
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Text("SESSIONS (${sessionsList.size})", color = pColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))

                        if (sessionsList.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No sessions found", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            val sessionsListState = rememberLazyListState()
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                LazyColumn(
                                    state = sessionsListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(sessionsList) { item ->
                                        val isSelected = (item.sessionId == selectedSessionId)
                                        val bg = if (isSelected) pColor.copy(alpha = 0.2f) else subCardBg
                                        val borderC = if (isSelected) pColor else cardBorder

                                        Column(
                                            Modifier.fillMaxWidth().background(bg, RoundedCornerShape(12.dp)).border(1.dp, borderC, RoundedCornerShape(12.dp)).appClickable { selectedSessionId = item.sessionId }.padding(14.dp)
                                        ) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(item.userName, fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                                                Text(item.status, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (item.status == "AGENT_REQUESTED") Color.Yellow else pColor)
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(item.userEmail.ifEmpty { item.ipAddress }, fontSize = 12.sp, color = Color.Gray)
                                            Spacer(Modifier.height(4.dp))
                                            Text(item.lastMessage, fontSize = 11.sp, color = textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Full-screen Active Session Transcript & Input
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        // Session Header & Action Buttons
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(selectedSession?.userName ?: "Visitor", fontWeight = FontWeight.Black, color = textColor, fontSize = 15.sp)
                                Text("🌐 ${selectedSession?.currentPage?.ifEmpty { "Homepage" } ?: "Homepage"}", color = pColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { performAgentAction("admin_takeover", JSONObject().put("session_id", selectedSessionId)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = pColor, contentColor = Color.Black),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("⚡ Take Over", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { performAgentAction("admin_close_session", JSONObject().put("session_id", selectedSessionId)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("🔒 Close", fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Live User Inspector Box
                        Column(Modifier.fillMaxWidth().background(subCardBg, RoundedCornerShape(8.dp)).border(1.dp, pColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(6.dp)) {
                            Text(accountDetailsText, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(6.dp))

                        // Transcript Stream occupying full remaining height
                        val listState = rememberLazyListState()
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().background(transcriptBg, RoundedCornerShape(8.dp)).padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(messagesList) { m ->
                                    val isUser = (m.senderType == "user")
                                    val isAgent = (m.senderType == "agent")
                                    val isAi = (m.senderType == "ai")
                                    val msgBg = if (isUser) pColor.copy(alpha = 0.15f) else if (isAgent) Color(0xFFFF9F0A).copy(alpha = 0.2f) else if (isLight) Color(0xFFE2E2E8) else Color(0xFF1E1E2C)

                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                        Column(Modifier.weight(1f).background(msgBg, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(m.senderName, fontWeight = FontWeight.Black, fontSize = 10.sp, color = if (isUser) pColor else Color.Yellow)
                                                if (isAi) {
                                                    Text("🤖 AI", fontSize = 9.sp, color = pColor, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(m.messageText, color = textColor, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Input Bar
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type reply...", color = Color.Gray, fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = pColor,
                                    unfocusedBorderColor = cardBorder,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                ),
                                singleLine = true
                            )
                            Button(
                                onClick = { sendMessage() },
                                colors = ButtonDefaults.buttonColors(containerColor = pColor, contentColor = Color.Black)
                            ) {
                                Text("SEND", fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
