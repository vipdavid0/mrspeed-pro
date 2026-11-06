package com.fdossena.speedtest.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.ImageLoader
import com.fdossena.speedtest.ui.models.SpeedtestUIState
import com.fdossena.speedtest.utils.*
import com.fdossena.speedtest.utils.AvatarCacheManager
import com.fdossena.speedtest.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import javax.swing.JFileChooser

data class UserChatMessageItem(
    val id: Int,
    val senderType: String,
    val senderName: String,
    val messageText: String,
    val senderAvatar: String,
    val createdAt: String
)

// Real Desktop Audio Recording Engine
private var activeTargetDataLine: TargetDataLine? = null
private var recordedVoiceFile: File? = null

fun startRealAudioRecording(): File? {
    try {
        val format = javax.sound.sampled.AudioFormat(16000f, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        if (AudioSystem.isLineSupported(info)) {
            val line = AudioSystem.getLine(info) as TargetDataLine
            line.open(format)
            line.start()
            activeTargetDataLine = line

            val audioFile = File.createTempFile("mrspeed_voice_", ".wav")
            recordedVoiceFile = audioFile

            Thread {
                try {
                    AudioSystem.write(AudioInputStream(line), AudioFileFormat.Type.WAVE, audioFile)
                } catch (e: Exception) {}
            }.start()

            return audioFile
        }
    } catch (e: Exception) {
        println("[DEBUG] Start audio recording error: ${e.message}")
    }
    return null
}

fun stopRealAudioRecording(): File? {
    try {
        activeTargetDataLine?.stop()
        activeTargetDataLine?.close()
        activeTargetDataLine = null
    } catch (e: Exception) {}
    return recordedVoiceFile
}

fun playRealAudioFile(file: File) {
    try {
        if (file.exists() && file.length() > 0) {
            val audioIn = AudioSystem.getAudioInputStream(file)
            val clip = AudioSystem.getClip()
            clip.open(audioIn)
            clip.start()
        }
    } catch (e: Exception) {
        println("[DEBUG] Audio playback error: ${e.message}")
    }
}

suspend fun uploadFileToBackend(file: File, onProgress: (Float) -> Unit): JSONObject? {
    return withContext(Dispatchers.IO) {
        try {
            val boundary = "Boundary-" + System.currentTimeMillis()
            val url = URL("https://mrspeed.ca/backend/api/livechat.php?action=upload_file")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.doOutput = true

            val fileSize = file.length()
            var bytesWritten = 0L

            conn.outputStream.use { out ->
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                out.write(("$twoHyphens$boundary$lineEnd").toByteArray())
                out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"$lineEnd").toByteArray())
                out.write(("Content-Type: application/octet-stream$lineEnd$lineEnd").toByteArray())

                file.inputStream().use { input ->
                    val buffer = ByteArray(4096)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        bytesWritten += read
                        if (fileSize > 0) {
                            onProgress(bytesWritten.toFloat() / fileSize.toFloat())
                        }
                    }
                }

                out.write(lineEnd.toByteArray())
                out.write(("$twoHyphens$boundary$twoHyphens$lineEnd").toByteArray())
            }

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(responseText)
        } catch (e: Exception) {
            println("[DEBUG] Upload file error: ${e.message}")
            null
        }
    }
}

@Composable
fun VoiceNotePlayerCard(text: String, voiceFile: File?, pColor: Color) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            if (voiceFile != null && voiceFile.exists()) {
                playRealAudioFile(voiceFile)
            }
            progress = 0f
            while (progress < 1f && isPlaying) {
                delay(100)
                progress += 0.05f
            }
            isPlaying = false
            progress = 0f
        }
    }

    Row(
        modifier = Modifier
            .background(Color(0xFF161B26), RoundedCornerShape(12.dp))
            .border(1.dp, pColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppIconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier.size(32.dp).background(pColor.copy(alpha = 0.2f), CircleShape)
        ) {
            Text(if (isPlaying) "⏸️" else "▶️", fontSize = 14.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = pColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun WebsiteTypingIndicator(pColor: Color, isLight: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse))
    val alpha2 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse))
    val alpha3 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse))

    Row(
        modifier = Modifier
            .background(if (isLight) Color.White else Color(0xFF161B22), RoundedCornerShape(14.dp))
            .border(1.dp, pColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(pColor.copy(alpha = alpha1)))
        Box(Modifier.size(8.dp).clip(CircleShape).background(pColor.copy(alpha = alpha2)))
        Box(Modifier.size(8.dp).clip(CircleShape).background(pColor.copy(alpha = alpha3)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLiveSupportChatScreen(
    state: SpeedtestUIState,
    onGoHome: () -> Unit
) {
    val primaryScheme = MaterialTheme.colorScheme.primary
    val pColor = remember(state.neonPrimaryColor, primaryScheme) {
        if (state.neonPrimaryColor == "#00E5FF") primaryScheme else state.neonPrimaryColor.toColor().takeOrElse { primaryScheme }
    }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var sessionId by remember { mutableStateOf<String?>(null) }
    var sessionStatus by remember { mutableStateOf("AI") }
    var agentIsTyping by remember { mutableStateOf(false) }
    var isWaitingForAiReply by remember { mutableStateOf(true) } // Show typing immediately on open before hello

    // Voice note state
    var isRecordingVoice by remember { mutableStateOf(false) }
    var hasVoiceNoteToReview by remember { mutableStateOf(false) }
    var isPlayingVoiceReview by remember { mutableStateOf(false) }
    var recordingTimeSec by remember { mutableStateOf(0) }
    var pendingVoiceFile by remember { mutableStateOf<File?>(null) }

    // File attachment review state
    var pendingAttachmentFile by remember { mutableStateOf<File?>(null) }
    var isUploadingFile by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    var sendOnEnter by remember { mutableStateOf(true) }
    var messagesList by remember { mutableStateOf<List<UserChatMessageItem>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var lastMsgId by remember { mutableStateOf(0) }
    var isPolling by remember { mutableStateOf(true) }

    val apiBaseUrl = "https://mrspeed.ca/backend/api/livechat.php"
    val siteId = "mrspeed_main"

    // Auto-scroll to bottom when messages change
    LaunchedEffect(messagesList.size, isWaitingForAiReply, agentIsTyping) {
        if (messagesList.isNotEmpty() || isWaitingForAiReply) {
            listState.animateScrollToItem((messagesList.size).coerceAtLeast(0))
        }
    }

    // Voice note recording timer
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingTimeSec = 0
            pendingVoiceFile = startRealAudioRecording()
            while (isRecordingVoice) {
                delay(1000)
                recordingTimeSec++
            }
        }
    }

    // Init Session Function
    suspend fun initSupportSession() {
        withContext(Dispatchers.IO) {
            try {
                AvatarCacheManager.preloadAllAvatarsOnStartup(state.userEmail)

                val url = URL(apiBaseUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val emailVal = state.userEmail ?: ""
                val nameVal = if (emailVal.isNotEmpty()) emailVal.substringBefore("@") else "App User"

                val body = JSONObject()
                body.put("action", "init_session")
                body.put("site_id", siteId)
                body.put("user_email", emailVal)
                body.put("user_name", nameVal)
                body.put("lang_code", state.appLanguage)
                body.put("current_page", "App: ${state.screen}")

                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)

                if (json.optBoolean("success")) {
                    val sid = json.optString("session_id")
                    val st = json.optString("status", "AI")
                    withContext(Dispatchers.Main) {
                        sessionId = sid
                        sessionStatus = st
                    }
                }
            } catch (e: Exception) {
                println("[DEBUG] UserLiveSupportChat init error: ${e.message}")
            }
        }
    }

    // Poll Messages Function
    suspend fun pollMessages(sid: String) {
        withContext(Dispatchers.IO) {
            try {
                val currentPageStr = "App: ${state.screen}"
                val encodedPage = java.net.URLEncoder.encode(currentPageStr, "UTF-8")
                val url = URL("$apiBaseUrl?action=poll_messages&session_id=$sid&last_id=$lastMsgId&current_page=$encodedPage")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 20000
                conn.readTimeout = 20000

                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)

                if (json.optBoolean("success")) {
                    val sessObj = json.optJSONObject("session") as? JSONObject
                    if (sessObj != null) {
                        val st = sessObj.optString("status", "AI")
                        val isTyping = sessObj.optString("agent_is_typing", "false").toBoolean()
                        withContext(Dispatchers.Main) {
                            sessionStatus = st
                            agentIsTyping = isTyping
                        }
                    }

                    val msgsArr = json.optJSONArray("messages")
                    if (msgsArr != null && msgsArr.length() > 0) {
                        withContext(Dispatchers.Main) {
                            isWaitingForAiReply = false
                        }
                        val existingIds = messagesList.map { it.id }.toSet()
                        val newMsgs = mutableListOf<UserChatMessageItem>()
                        var maxId = lastMsgId
                        for (m in 0 until msgsArr.length()) {
                            val msg = msgsArr.getJSONObject(m)
                            val mid = msg.optInt("id")
                            if (mid > maxId) maxId = mid
                            if (!existingIds.contains(mid)) {
                                val rawAvatar = msg.optString("sender_avatar", "")
                                val resolvedAvatar = if (rawAvatar.startsWith("http")) {
                                    rawAvatar
                                } else if (rawAvatar.isNotEmpty()) {
                                    "https://mrspeed.ca/backend/api/$rawAvatar"
                                } else {
                                    ""
                                }
                                val cachedLocalPath = if (resolvedAvatar.isNotEmpty()) {
                                    AvatarCacheManager.downloadAndCacheAvatar(resolvedAvatar)
                                } else {
                                    ""
                                }

                                newMsgs.add(
                                    UserChatMessageItem(
                                        id = mid,
                                        senderType = msg.optString("sender_type", "user"),
                                        senderName = msg.optString("sender_name", "User"),
                                        messageText = msg.optString("message_text", ""),
                                        senderAvatar = cachedLocalPath.ifEmpty { resolvedAvatar },
                                        createdAt = msg.optString("created_at", "")
                                    )
                                )
                            }
                        }
                        if (newMsgs.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                val filteredOld = messagesList.filter { it.id < 10000 }
                                messagesList = filteredOld + newMsgs
                                lastMsgId = maxId
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e.message?.contains("timed out", ignoreCase = true) != true) {
                    println("[DEBUG] UserLiveSupportChat poll error: ${e.message}")
                }
            }
        }
    }

    // Send Message Function
    fun sendMessage(textToSend: String) {
        if (textToSend.isBlank()) return
        val currentSid = sessionId ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val url = URL(apiBaseUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 20000
                conn.readTimeout = 20000
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val body = JSONObject()
                body.put("action", "send_message")
                body.put("site_id", siteId)
                body.put("session_id", currentSid)
                body.put("message", textToSend)
                body.put("current_page", "App: ${state.screen}")

                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                conn.inputStream.bufferedReader().use { it.readText() }

                pollMessages(currentSid)
            } catch (e: Exception) {
                println("[DEBUG] UserLiveSupportChat send error: ${e.message}")
            }
        }
    }

    // Open Native File Picker
    fun openFilePicker() {
        scope.launch(Dispatchers.IO) {
            try {
                val chooser = JFileChooser()
                chooser.dialogTitle = "Select File / Image Attachment"
                val res = chooser.showOpenDialog(null)
                if (res == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
                    val file = chooser.selectedFile
                    withContext(Dispatchers.Main) {
                        pendingAttachmentFile = file
                    }
                }
            } catch (e: Exception) {
                println("[DEBUG] FilePicker error: ${e.message}")
            }
        }
    }

    // Request Human Agent Escalation
    fun requestHumanAgent() {
        sendMessage("talk to agent")
    }

    // Initialize and Start Polling
    LaunchedEffect(Unit) {
        initSupportSession()
    }

    LaunchedEffect(sessionId) {
        val currentSid = sessionId
        if (currentSid != null) {
            while (isPolling) {
                pollMessages(currentSid)
                delay(3000)
            }
        }
    }

    val isLight = state.themeMode == 1 || (state.themeMode == 0 && !androidx.compose.foundation.isSystemInDarkTheme())
    val screenBg = if (isLight) Color(0xFFF4F4F8) else Color(0xFF08080E)
    val cardBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF12121C)

    val isDesktop = com.fdossena.speedtest.utils.isDesktop
    val topBarPadding = if (isDesktop) 48.dp else 0.dp
    val endPadding = if (isDesktop) 110.dp else 16.dp

    Column(Modifier.fillMaxSize().background(screenBg)) {
        // Top Bar
        Row(
            Modifier.fillMaxWidth()
                .background(cardBg)
                .padding(top = topBarPadding + 12.dp, bottom = 12.dp, start = 16.dp, end = endPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconButton(onClick = onGoHome) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = pColor)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("💬 LIVE SUPPORT CHAT", color = pColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    val statusText = if (sessionStatus == "AGENT_ACTIVE") "👨‍💻 Support Agent Connected" else if (sessionStatus == "AGENT_REQUESTED") "🙋 Waiting for Support Agent..." else "🤖 Alex - Alex Support Specialist Online"
                    Text(statusText, color = if (sessionStatus == "AGENT_ACTIVE") Color(0xFFFF9F0A) else Color(0xFF00FF64), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            AppButton(
                onClick = { requestHumanAgent() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A), contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🙋 Human Agent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Messages Stream with Custom Desktop Scrollbar matching website theme
        val isLight = state.themeMode == 1 || (state.themeMode == 0 && !androidx.compose.foundation.isSystemInDarkTheme())
        val screenBg = if (isLight) Color(0xFFF5F7FC) else Color(0xFF08080E)
        val cardBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF12121C)
        val cardBorder = if (isLight) Color(0xFFE2E6EE) else Color(0xFF222233)
        val textColor = if (isLight) Color(0xFF111115) else Color.White
        val textMuted = if (isLight) Color(0xFF556677) else Color(0xFF8899A6)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messagesList) { m ->
                val isUser = (m.senderType == "user")
                val isAgent = (m.senderType == "agent")
                val isSystem = (m.senderType == "system")

                val msgBg = when {
                    isUser -> if (isLight) pColor.copy(alpha = 0.15f) else pColor.copy(alpha = 0.25f)
                    isAgent -> if (isLight) Color(0xFFFF9F0A).copy(alpha = 0.15f) else Color(0xFFFF9F0A).copy(alpha = 0.2f)
                    isSystem -> if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f)
                    else -> if (isLight) Color(0xFFEFEFF4) else Color(0xFF161B22)
                }
                val borderC = when {
                    isUser -> pColor.copy(alpha = 0.4f)
                    isAgent -> Color(0xFFFF9F0A).copy(alpha = 0.4f)
                    else -> if (isLight) Color(0xFFD2D6EE) else Color(0xFF30363D)
                }

                val avatarModel = if (m.senderAvatar.isNotEmpty() && File(m.senderAvatar).exists()) {
                    File(m.senderAvatar)
                } else {
                    AvatarCacheManager.getCachedModelOrRemote(
                        m.senderAvatar.ifEmpty {
                            if (isUser) "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&s=128"
                            else "https://mrspeed.ca/images/Futuristic Robot Executive Portrait.png"
                        }
                    )
                }

                val imageUrlMatch = remember(m.messageText) {
                    val regex = Regex("https?://[^\\s]+\\.(?:png|jpg|jpeg|gif|webp)", RegexOption.IGNORE_CASE)
                    regex.find(m.messageText)?.value
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    if (!isUser) {
                        SubcomposeAsyncImage(
                            model = avatarModel,
                            contentDescription = m.senderName,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.5f.dp, if (isAgent) Color(0xFFFF9F0A) else pColor, CircleShape),
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(if (isAgent) "👨‍💻" else "🤖", fontSize = 14.sp)
                                }
                            },
                            error = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(if (isAgent) "👨‍💻" else "🤖", fontSize = 14.sp)
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    Column(
                        modifier = Modifier.widthIn(min = 120.dp, max = 750.dp)
                    ) {
                        Text(
                            text = if (isUser) "You" else m.senderName,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = if (isUser) pColor else Color(0xFFFFCC00),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        if (m.messageText.contains("🎙️")) {
                            VoiceNotePlayerCard(m.messageText, pendingVoiceFile, pColor)
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(msgBg, RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (isUser) 14.dp else 3.dp,
                                        bottomEnd = if (isUser) 3.dp else 14.dp
                                    ))
                                    .border(1.dp, borderC, RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (isUser) 14.dp else 3.dp,
                                        bottomEnd = if (isUser) 3.dp else 14.dp
                                    ))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    MarkdownMessageContent(m.messageText, pColor, isLight)
                                    if (imageUrlMatch != null) {
                                        Spacer(Modifier.height(8.dp))
                                        SubcomposeAsyncImage(
                                            model = imageUrlMatch,
                                            contentDescription = "Attachment Image Preview",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, pColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                            loading = {
                                                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = pColor, modifier = Modifier.size(24.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isUser) {
                        Spacer(Modifier.width(8.dp))
                        SubcomposeAsyncImage(
                            model = avatarModel,
                            contentDescription = "You",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.5f.dp, pColor, CircleShape),
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("👤", fontSize = 14.sp)
                                }
                            },
                            error = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("👤", fontSize = 14.sp)
                                }
                            }
                        )
                    }
                }
            }

            // Exact Website Typing Indicator Animation
            if (isWaitingForAiReply || agentIsTyping) {
                item {
                    val aiAvatarModel = AvatarCacheManager.getCachedModelOrRemote("https://mrspeed.ca/images/Futuristic Robot Executive Portrait.png")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        SubcomposeAsyncImage(
                            model = aiAvatarModel,
                            contentDescription = "AI Assistant",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.5f.dp, pColor, CircleShape),
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("🤖", fontSize = 14.sp)
                                }
                            },
                            error = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("🤖", fontSize = 14.sp)
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Alex - AI Support Specialist",
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = Color(0xFFFFCC00),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            WebsiteTypingIndicator(pColor, isLight)
                        }
                    }
                }
            }
        }
        DesktopLazyScrollbar(
            lazyListState = listState,
            isLight = isLight,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 16.dp)
        )
      }

        // Quick Reply Chips
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("🔐 Recover Account", "📊 Speed Test History", "👤 My Account Info", "📡 Outage Detector", "🙋 Talk to Agent").forEach { qrText ->
                AppSuggestionChip(
                    onClick = { sendMessage(qrText) },
                    label = { Text(qrText, fontSize = 11.sp, color = pColor, fontWeight = FontWeight.Bold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = pColor.copy(alpha = 0.15f))
                )
            }
        }

        // Pre-Send Voice Note Review Card with Playback & Delete
        if (hasVoiceNoteToReview) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).background(Color(0xFF161B26)).border(1.dp, pColor, RoundedCornerShape(12.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIconButton(
                        onClick = {
                            isPlayingVoiceReview = !isPlayingVoiceReview
                            if (isPlayingVoiceReview && pendingVoiceFile != null) {
                                playRealAudioFile(pendingVoiceFile!!)
                            }
                        },
                        modifier = Modifier.size(36.dp).background(pColor.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Text(if (isPlayingVoiceReview) "⏸️" else "▶️", fontSize = 16.sp)
                    }
                    Text("🎙️ Voice Note (${recordingTimeSec}s)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppOutlinedButton(
                        onClick = {
                            hasVoiceNoteToReview = false
                            isPlayingVoiceReview = false
                            recordingTimeSec = 0
                            pendingVoiceFile?.delete()
                            pendingVoiceFile = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF453A))
                    ) {
                        Text("🗑️ Delete")
                    }
                    AppButton(
                        onClick = {
                            val vFile = pendingVoiceFile
                            hasVoiceNoteToReview = false
                            isPlayingVoiceReview = false
                            recordingTimeSec = 0
                            pendingVoiceFile = null
                            isUploadingFile = true
                            uploadProgress = 0f
                            scope.launch(Dispatchers.IO) {
                                var fileUrl = ""
                                if (vFile != null && vFile.exists()) {
                                    val uploadRes = uploadFileToBackend(vFile) { prog ->
                                        scope.launch(Dispatchers.Main) { uploadProgress = prog }
                                    }
                                    fileUrl = uploadRes?.optString("file_url") ?: ""
                                }
                                withContext(Dispatchers.Main) {
                                    isUploadingFile = false
                                    uploadProgress = 0f
                                }
                                val msgText = if (fileUrl.isNotEmpty()) {
                                    "🎙️ Voice Note (${recordingTimeSec}s)\n$fileUrl"
                                } else {
                                    "🎙️ [Voice Note: ${recordingTimeSec}s]"
                                }
                                sendMessage(msgText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = pColor, contentColor = Color.Black)
                    ) {
                        Text("➤ Send")
                    }
                }
            }
        }

        // Pre-Send Attachment Review Card with Image Thumbnail Preview & Upload Progress Bar
        if (pendingAttachmentFile != null) {
            val isImageFile = remember(pendingAttachmentFile) {
                val name = pendingAttachmentFile!!.name.lowercase()
                name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp") || name.endsWith(".gif")
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).background(Color(0xFF161B26)).border(1.dp, pColor, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isImageFile) {
                            SubcomposeAsyncImage(
                                model = pendingAttachmentFile!!,
                                contentDescription = "Preview",
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, pColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            )
                        } else {
                            Text("📄", fontSize = 24.sp)
                        }
                        Column {
                            Text(pendingAttachmentFile!!.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("${pendingAttachmentFile!!.length() / 1024} KB", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppOutlinedButton(
                            onClick = { pendingAttachmentFile = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF453A)),
                            enabled = !isUploadingFile
                        ) {
                            Text("🗑️ Discard")
                        }
                        AppButton(
                            onClick = {
                                val f = pendingAttachmentFile!!
                                isUploadingFile = true
                                uploadProgress = 0f
                                scope.launch(Dispatchers.IO) {
                                    val uploadRes = uploadFileToBackend(f) { prog ->
                                        scope.launch(Dispatchers.Main) { uploadProgress = prog }
                                    }
                                    val fileUrl = uploadRes?.optString("file_url") ?: ""
                                    val isImg = (uploadRes?.optString("file_type") == "image") || isImageFile
                                    withContext(Dispatchers.Main) {
                                        isUploadingFile = false
                                        uploadProgress = 0f
                                        pendingAttachmentFile = null
                                    }
                                    val msgText = if (fileUrl.isNotEmpty()) {
                                        if (isImg) "📸 Attached Image:\n$fileUrl" else "📎 Attached Document: ${f.name}\n$fileUrl"
                                    } else {
                                        "📎 Attached File: ${f.name}"
                                    }
                                    sendMessage(msgText)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = pColor, contentColor = Color.Black),
                            enabled = !isUploadingFile
                        ) {
                            Text(if (isUploadingFile) "Uploading (${(uploadProgress * 100).toInt()}%)..." else "⬆️ Upload & Send")
                        }
                    }
                }

                if (isUploadingFile) {
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = pColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }

        // Bottom Input Bar
        Column(
            Modifier.fillMaxWidth().background(cardBg).border(width = 1.dp, color = cardBorder)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Voice Note Button (🎤)
                AppIconButton(
                    onClick = {
                        if (isRecordingVoice) {
                            // Stop recording
                            isRecordingVoice = false
                            pendingVoiceFile = stopRealAudioRecording()
                            hasVoiceNoteToReview = true
                        } else {
                            isRecordingVoice = true
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (isRecordingVoice) Color(0xFFFF0055).copy(alpha = 0.3f) else (if (isLight) Color(0xFFFFFFFF) else Color(0xFF161B26)), RoundedCornerShape(12.dp))
                        .border(1.dp, if (isRecordingVoice) Color(0xFFFF0055) else (if (isLight) Color(0xFFD2D8E5) else Color(0xFF2A3447)), RoundedCornerShape(12.dp))
                ) {
                    Text(if (isRecordingVoice) "⏹️" else "🎤", fontSize = 18.sp)
                }

                // 2. Enter-to-Send Toggle Button (⌨️)
                AppIconButton(
                    onClick = {
                        sendOnEnter = !sendOnEnter
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (sendOnEnter) pColor.copy(alpha = 0.2f) else (if (isLight) Color(0xFFFFFFFF) else Color(0xFF161B26)), RoundedCornerShape(12.dp))
                        .border(1.dp, if (sendOnEnter) pColor else (if (isLight) Color(0xFFD2D8E5) else Color(0xFF2A3447)), RoundedCornerShape(12.dp))
                ) {
                    Text("⌨️", fontSize = 18.sp)
                }

                // 3. File Attachment Button (📎)
                AppIconButton(
                    onClick = {
                        openFilePicker()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF161B26), RoundedCornerShape(12.dp))
                        .border(1.dp, if (isLight) Color(0xFFD2D8E5) else Color(0xFF2A3447), RoundedCornerShape(12.dp))
                ) {
                    Text("📎", fontSize = 18.sp)
                }

                // 4. Text Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF121622), RoundedCornerShape(14.dp))
                        .border(1.dp, if (isLight) Color(0xFFD2D8E5) else Color(0xFF283244), RoundedCornerShape(14.dp))
                        .cursorHoverable()
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = if (isRecordingVoice) "🎙️ Recording voice note (${recordingTimeSec}s)..." else inputText,
                        onValueChange = { inputText = it },
                        readOnly = isRecordingVoice,
                        textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 13.5.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { keyEvent ->
                                if (sendOnEnter && keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                    if (!keyEvent.isShiftPressed) {
                                        val txt = inputText.trim()
                                        if (txt.isNotBlank()) {
                                            inputText = ""
                                            sendMessage(txt)
                                        }
                                        true
                                    } else false
                                } else false
                            },
                        singleLine = sendOnEnter
                    )
                    if (inputText.isEmpty() && !isRecordingVoice) {
                        Text("Type your message...", color = if (isLight) Color(0xFF778899) else Color(0xFF8899A6), fontSize = 13.5.sp)
                    }
                }

                // 5. Bright Neon Cyan Send Button (➤)
                AppButton(
                    onClick = {
                        if (isRecordingVoice) {
                            isRecordingVoice = false
                            pendingVoiceFile = stopRealAudioRecording()
                            hasVoiceNoteToReview = true
                        } else if (inputText.isNotBlank()) {
                            val txt = inputText
                            inputText = ""
                            sendMessage(txt)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("➤", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun MarkdownMessageContent(text: String, pColor: Color, isLight: Boolean) {
    val bubbleTextColor = if (isLight) Color(0xFF000000) else Color.White
    val subTextColor = if (isLight) Color(0xFF111115) else Color(0xFFF1F5F9)

    val lines = text.split("\r\n", "\n")
    val parsedBlocks = mutableListOf<Any>()

    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        val isTableRow = line.startsWith("|")

        if (isTableRow) {
            val tableRows = mutableListOf<List<String>>()
            while (i < lines.size) {
                var rowLine = lines[i].trim()
                if (rowLine.isEmpty() && i + 1 < lines.size && lines[i + 1].trim().startsWith("|")) {
                    i++
                    continue
                }
                if (!rowLine.startsWith("|")) break
                if (!rowLine.endsWith("|")) rowLine = "$rowLine |"

                if (rowLine.contains("---") || rowLine.contains(":---") || rowLine.contains("---:") || rowLine.contains("|---|")) {
                    i++
                    continue
                }
                val cells = rowLine.split("|").filterIndexed { idx, _ -> idx > 0 && idx < rowLine.split("|").size - 1 }.map { it.trim() }
                if (cells.isNotEmpty()) {
                    tableRows.add(cells)
                }
                i++
            }
            if (tableRows.isNotEmpty()) {
                parsedBlocks.add(tableRows)
            }
            continue
        }

        parsedBlocks.add(lines[i])
        i++
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (block in parsedBlocks) {
            when (block) {
                is List<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val rows = block as List<List<String>>
                    if (rows.isNotEmpty()) {
                        val colCount = rows[0].size
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, pColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .background(if (isLight) Color(0xFFEFEFF4) else Color(0xFF04060C), RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(2.dp)) {
                                rows.forEachIndexed { rIndex, rawCells ->
                                    val isHeader = rIndex == 0
                                    val rowBg = if (isHeader) pColor.copy(alpha = 0.22f) else Color.Transparent

                                    val rowCells = mutableListOf<String>()
                                    for (c in 0 until colCount) {
                                        rowCells.add(rawCells.getOrNull(c) ?: "")
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                            .background(rowBg, RoundedCornerShape(4.dp))
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        rowCells.forEachIndexed { cIndex, cell ->
                                            val cleanCell = cell.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 4.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    text = cleanCell,
                                                    color = if (isHeader) pColor else subTextColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                                    lineHeight = 14.sp,
                                                    softWrap = true
                                                )
                                            }
                                            if (cIndex < rowCells.size - 1) {
                                                VerticalDivider(
                                                    color = if (isLight) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f),
                                                    modifier = Modifier.fillMaxHeight().width(1.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (rIndex < rows.size - 1) {
                                        HorizontalDivider(color = if (isLight) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f), thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }
                }
                is String -> {
                    val lineStr = block
                    when {
                        lineStr.startsWith("### ") -> {
                            Text(
                                text = lineStr.removePrefix("### ").replace(Regex("\\*\\*(.*?)\\*\\*"), "$1"),
                                color = pColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        lineStr.startsWith("## ") -> {
                            Text(
                                text = lineStr.removePrefix("## ").replace(Regex("\\*\\*(.*?)\\*\\*"), "$1"),
                                color = pColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        else -> {
                            val cleanText = lineStr.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                            if (cleanText.isNotBlank()) {
                                Text(
                                    text = cleanText,
                                    color = bubbleTextColor,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
