package com.fdossena.speedtest.data

import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.net.ssl.*
import java.security.SecureRandom
import java.security.cert.X509Certificate

class WebSyncManager(private val settings: SettingsProvider) : SyncProvider {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val apiBaseUrl = "https://mrspeed.ca/backend/api"

    override suspend fun initialize() {
        println("[DEBUG] WebSyncManager: Initialized using SQL Backend")
        if (!isAnonymous()) {
            val uid = getUserId()
            try {
                val response = executeRequest("$apiBaseUrl/auth.php?action=get_profile&uid=$uid", "GET")
                if (response != null) {
                    val json = JSONObject(response)
                    if (json.optString("status") == "success") {
                        val dbRole = json.optString("role", "")
                        if (dbRole.isNotEmpty()) {
                            settings.linkedRole = dbRole
                        }
                    }
                }
            } catch (e: Exception) {
                println("[DEBUG] WebSyncManager startup role sync error: ${e.message}")
            }
        }
    }

    override fun getUserId(): String = settings.linkedUid ?: "anonymous_pc"
    override fun getUserEmail(): String? = settings.linkedEmail
    override fun getUserRole(): String = settings.linkedRole ?: "user"
    override fun isAdmin(): Boolean = getUserRole().lowercase() == "admin"
    override fun isAnonymous(): Boolean = settings.linkedUid == null
    override fun getLastSync(): Long = settings.lastSyncTimestamp
    override fun getDeviceId(): String = settings.deviceId

    private fun trustAllSslSockets(conn: HttpsURLConnection) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            })
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, SecureRandom())
            conn.sslSocketFactory = sc.socketFactory
            conn.hostnameVerifier = HostnameVerifier { _, _ -> true }
        } catch (e: Throwable) {
            println("[DEBUG] WebSync SSL Error: ${e.message}")
        }
    }

    private suspend fun executeRequest(urlStr: String, method: String, body: String? = null): String? {
        return withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            var currentUrl = urlStr
            var redirectCount = 0

            while (redirectCount < 5) {
                try {
                    val url = URL(currentUrl)
                    conn = url.openConnection(Proxy.NO_PROXY) as HttpURLConnection
                    if (conn is HttpsURLConnection) {
                        trustAllSslSockets(conn)
                    }
                    conn.instanceFollowRedirects = false
                    conn.requestMethod = method.uppercase()
                    conn.setRequestProperty("Content-Type", "application/json")
                    val userAgent = if (urlStr.contains("mrspeed.ca")) "MrSpeedApp/1.0" else "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    conn.setRequestProperty("User-Agent", userAgent)
                    conn.connectTimeout = 25000
                    conn.readTimeout = 25000

                    if (body != null && method.equals("POST", ignoreCase = true)) {
                        val bytes = body.toByteArray(StandardCharsets.UTF_8)
                        conn.doOutput = true
                        conn.doInput = true
                        conn.setFixedLengthStreamingMode(bytes.size)
                        conn.outputStream.use { it.write(bytes) }
                    }

                    val code = conn.responseCode
                    if (code in 200..299) {
                        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                        return@withContext responseText
                    } else if (code == 301 || code == 302 || code == 307 || code == 308) {
                        val loc = conn.getHeaderField("Location")
                        if (!loc.isNullOrEmpty()) {
                            currentUrl = loc
                            redirectCount++
                            conn.disconnect()
                            continue
                        }
                    } else {
                        val errText = conn.errorStream?.bufferedReader()?.use { it.readText() }
                        if (!errText.isNullOrEmpty() && errText.trim().startsWith("{")) {
                            return@withContext errText
                        }
                    }
                } catch (e: Throwable) {
                    val msg = e.message ?: ""
                    if (!msg.contains("timed out", ignoreCase = true)) {
                        println("[DEBUG] WebSync: Request Error ($currentUrl): $msg")
                    }
                    return@withContext null
                } finally {
                    conn?.disconnect()
                }
            }
            null
        }
    }

    override suspend fun uploadResult(result: TestResult) {
        val uid = getUserId()
        val tsInSeconds = if (result.timestamp > 1000000000000L) result.timestamp / 1000 else result.timestamp
        val body = JSONObject().apply {
            put("downloadSpeed", result.downloadSpeed)
            put("uploadSpeed", result.uploadSpeed)
            put("ping", result.ping)
            put("jitter", result.jitter)
            put("serverName", result.serverName)
            put("sponsorName", result.sponsorName)
            put("timestamp", tsInSeconds)
            put("uid", uid)
            put("source", result.source)
        }.toString()
        
        executeRequest("$apiBaseUrl/sync_results.php?action=upload", "POST", body)
    }

    override suspend fun fetchResults(): List<TestResult> {
        val uid = getUserId()
        val response = executeRequest("$apiBaseUrl/sync_results.php?action=fetch&uid=$uid", "GET")
        if (response == null) return emptyList()
        
        val array = JSONArray(response)
        val list = mutableListOf<TestResult>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(TestResult(
                downloadSpeed = obj.optDouble("download", 0.0),
                uploadSpeed = obj.optDouble("upload", 0.0),
                ping = obj.optDouble("ping", 0.0),
                jitter = obj.optDouble("jitter", 0.0),
                serverName = obj.optString("server", "Unknown"),
                timestamp = obj.optLong("timestamp", 0L) * 1000,
                source = obj.optString("source", "PC")
            ))
        }
        return list
    }

    override suspend fun fetchISPLeaderboard(): List<ISPRecord> {
        val response = executeRequest("$apiBaseUrl/leaderboard.php", "GET")
        if (response == null) return emptyList()
        
        val array = JSONArray(response)
        val list = mutableListOf<ISPRecord>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(ISPRecord(obj.getString("name"), obj.getDouble("avgSpeed"), 95.0))
        }
        return list
    }

    override suspend fun performFirestoreRequest(urlPath: String, method: String, body: String?): String? {
        return executeRequest("$apiBaseUrl/detector.php?path=$urlPath", method, body)
    }

    override suspend fun registerDevice(deviceName: String, deviceType: String) {
        val uid = getUserId()
        val body = JSONObject().apply {
            put("id", getDeviceId())
            put("name", deviceName)
            put("type", deviceType)
        }.toString()
        executeRequest("$apiBaseUrl/devices.php?action=register&uid=$uid", "POST", body)
    }

    override suspend fun getDeviceName(): String = System.getProperty("os.name")

    override suspend fun syncSettings() {
        val uid = getUserId()
        val body = JSONObject().apply {
            put("themeMode", settings.themeMode)
            put("appLanguage", settings.appLanguage)
        }.toString()
        executeRequest("$apiBaseUrl/auth.php?action=update_profile&uid=$uid", "POST", body)
    }

    override suspend fun performManualSync(results: List<TestResult>) {
        results.forEach { uploadResult(it) }
    }

    override suspend fun signIn(email: String, pass: String, captchaToken: String?) {
        val body = JSONObject().apply {
            put("email", email)
            put("password", pass)
            if (!captchaToken.isNullOrEmpty()) {
                put("captcha_token", captchaToken)
            }
        }.toString()
        val response = executeRequest("$apiBaseUrl/auth.php?action=login", "POST", body)
        if (response != null) {
            val json = JSONObject(response)
            val status = json.optString("status", "")
            if (status == "2FA_REQUIRED") {
                val reqId = json.optString("reqId", "")
                val uid = json.optString("uid", "")
                val userEmail = json.optString("email", email)
                throw Exception("2FA_REQUIRED:$reqId:$uid:$userEmail")
            } else if (json.has("uid")) {
                settings.linkedUid = json.getString("uid")
                settings.linkedEmail = json.getString("email")
                settings.linkedRole = json.optString("role", "user")
            } else throw Exception(json.optString("error", "Login failed"))
        } else throw Exception("Server connection failed")
    }

    override suspend fun signUp(email: String, pass: String, name: String, phone: String, captchaToken: String?) {
        val body = JSONObject().apply {
            put("email", email)
            put("password", pass)
            if (name.isNotEmpty()) put("displayName", name)
            if (phone.isNotEmpty()) put("phone", phone)
            if (!captchaToken.isNullOrEmpty()) {
                put("captcha_token", captchaToken)
            }
        }.toString()
        val response = executeRequest("$apiBaseUrl/auth.php?action=signup", "POST", body)
        if (response != null) {
            val json = JSONObject(response)
            if (json.has("uid")) {
                settings.linkedUid = json.getString("uid")
                settings.linkedEmail = json.getString("email")
                settings.linkedRole = json.optString("role", "user")
            } else throw Exception(json.optString("error", "Signup failed"))
        } else throw Exception("Server connection failed")
    }

    override suspend fun updatePhone(phone: String) {
        val uid = getUserId() ?: return
        val body = JSONObject().apply {
            put("uid", uid)
            put("phone", phone)
        }.toString()
        executeRequest("$apiBaseUrl/auth.php?action=update_phone", "POST", body)
    }

    override suspend fun sendTest2FaSms(): String? {
        val uid = getUserId() ?: return null
        val response = executeRequest("$apiBaseUrl/auth.php?action=test_2fa_sms&uid=$uid", "POST")
        if (response != null) {
            val json = JSONObject(response)
            if (json.optString("status") == "sms_sent") {
                return json.optString("otpCode", "849201")
            }
        }
        return null
    }

    override suspend fun approve2Fa(reqId: String, allow: Boolean) {
        val body = JSONObject().apply {
            put("reqId", reqId)
            put("status", if (allow) "ALLOWED" else "DENIED")
        }.toString()
        executeRequest("$apiBaseUrl/auth.php?action=approve_2fa", "POST", body)
    }

    override suspend fun verifyTotpCode(uid: String, code: String): Boolean {
        val body = JSONObject().apply {
            put("uid", uid)
            put("code", code)
        }.toString()
        val response = executeRequest("$apiBaseUrl/auth.php?action=verify_totp_code", "POST", body)
        if (response != null) {
            val json = JSONObject(response)
            if (json.optString("status") == "ALLOWED") {
                val foundUid = json.optString("uid", uid)
                val foundEmail = json.optString("email", "")
                settings.linkedUid = foundUid
                if (foundEmail.isNotEmpty()) settings.linkedEmail = foundEmail
                return true
            }
        }
        return false
    }

    override suspend fun sendEmergencyOtp(uid: String, email: String): Boolean {
        val body = JSONObject().apply {
            put("uid", uid)
            put("email", email)
        }.toString()
        val response = executeRequest("$apiBaseUrl/auth.php?action=send_emergency_otp", "POST", body)
        if (response != null) {
            val json = JSONObject(response)
            return json.optString("status") == "otp_sent"
        }
        return false
    }

    override suspend fun verifyEmergencyOtp(uid: String, otp: String): Boolean {
        val body = JSONObject().apply {
            put("uid", uid)
            put("otp", otp)
        }.toString()
        val response = executeRequest("$apiBaseUrl/auth.php?action=verify_emergency_otp", "POST", body)
        if (response != null) {
            val json = JSONObject(response)
            if (json.optString("status") == "ALLOWED") {
                val foundUid = json.optString("uid", uid)
                val foundEmail = json.optString("email", "")
                settings.linkedUid = foundUid
                if (foundEmail.isNotEmpty()) settings.linkedEmail = foundEmail
                return true
            }
        }
        return false
    }

    override suspend fun verifyRecoveryKey(uid: String, key: String): Boolean {
        val body = JSONObject().apply {
            put("uid", uid)
            put("recoveryKey", key)
        }.toString()
        val response = executeRequest("$apiBaseUrl/auth.php?action=verify_recovery_key", "POST", body)
        if (response != null) {
            val json = JSONObject(response)
            if (json.optString("status") == "ALLOWED") {
                val foundUid = json.optString("uid", uid)
                val foundEmail = json.optString("email", "")
                settings.linkedUid = foundUid
                if (foundEmail.isNotEmpty()) settings.linkedEmail = foundEmail
                return true
            }
        }
        return false
    }

    override suspend fun check2FaStatus(reqId: String): Triple<String, String, String>? {
        val response = executeRequest("$apiBaseUrl/auth.php?action=check_2fa_status&reqId=$reqId", "GET")
        if (response != null) {
            try {
                val json = JSONObject(response)
                val status = json.optString("status", "PENDING")
                val uid = json.optString("uid", "")
                val email = json.optString("email", "")
                return Triple(status, uid, email)
            } catch (e: Exception) {
                println("[DEBUG] WebSync: check2FaStatus error: ${e.message}")
            }
        }
        return Triple("PENDING", "", "")
    }

    override suspend fun fetchProfile(): Pair<String, String>? {
        val uid = getUserId() ?: return null
        val response = executeRequest("$apiBaseUrl/auth.php?action=get_profile&uid=$uid", "GET")
        if (response != null) {
            val json = JSONObject(response)
            if (json.optString("status") == "success") {
                val name = json.optString("displayName", "")
                val phone = json.optString("phone", "")
                val role = json.optString("role", "")
                if (role.isNotEmpty()) {
                    settings.linkedRole = role
                }
                return Pair(name, phone)
            }
        }
        return null
    }

    override suspend fun signOut() {
        settings.linkedUid = null
        settings.linkedEmail = null
    }
    override suspend fun updateEmail(email: String, pass: String) {}
    override suspend fun updatePassword(old: String, new: String) {}
    override suspend fun deleteAccount() {}
    override suspend fun resetPassword(email: String) {}
    override suspend fun toggleWatchlist(serviceId: String) {}

    private var qrPollingJob: Job? = null

    override suspend fun authorizeQRLogin(sessionId: String) {
        val uid = getUserId()
        val email = getUserEmail() ?: ""
        val cleanSid = if (sessionId.contains("sid=")) sessionId.substringAfter("sid=").substringBefore("&") else sessionId

        val body = JSONObject().apply {
            put("sessionId", cleanSid)
            put("uid", uid)
            put("email", email)
        }.toString()

        executeRequest("$apiBaseUrl/auth.php?action=qr_authorize", "POST", body)
    }

    override suspend fun initializeQRSession(sessionId: String) {
        val cleanSid = if (sessionId.contains("sid=")) sessionId.substringAfter("sid=").substringBefore("&") else sessionId
        val body = JSONObject().apply {
            put("sessionId", cleanSid)
        }.toString()

        executeRequest("$apiBaseUrl/auth.php?action=qr_init", "POST", body)
    }

    override fun listenToQRLogin(
        sessionId: String, 
        onAuthorized: (String, String) -> Unit,
        on2FaRequired: ((String, String, String) -> Unit)?
    ) {
        qrPollingJob?.cancel()
        val cleanSid = if (sessionId.contains("sid=")) sessionId.substringAfter("sid=").substringBefore("&") else sessionId

        qrPollingJob = scope.launch {
            while (isActive) {
                delay(2000)
                val response = executeRequest("$apiBaseUrl/auth.php?action=qr_check&sessionId=$cleanSid", "GET")
                if (response != null) {
                    try {
                        val json = JSONObject(response)
                        val status = json.optString("status", "")
                        if (status == "AUTHORIZED") {
                            val uid = json.optString("uid", "")
                            val email = json.optString("email", "")
                            if (uid.isNotEmpty()) {
                                settings.linkedUid = uid
                                settings.linkedEmail = email
                                onAuthorized(uid, email)
                                break
                            }
                        } else if (status == "2FA_REQUIRED") {
                            val reqId = json.optString("reqId", "")
                            val uid = json.optString("uid", "")
                            val email = json.optString("email", "")
                            if (reqId.isNotEmpty()) {
                                on2FaRequired?.invoke(reqId, uid, email)
                                break
                            }
                        }
                    } catch (e: Exception) {
                        println("[DEBUG] WebSync: QR Check error: ${e.message}")
                    }
                }
            }
        }
    }

    override fun stopQRListening() {
        qrPollingJob?.cancel()
        qrPollingJob = null
    }

    override suspend fun checkQRSession(sessionId: String): Pair<String, String>? {
        val cleanSid = if (sessionId.contains("sid=")) sessionId.substringAfter("sid=").substringBefore("&") else sessionId
        val response = executeRequest("$apiBaseUrl/auth.php?action=qr_check&sessionId=$cleanSid", "GET")
        if (response != null) {
            try {
                val json = JSONObject(response)
                if (json.optString("status") == "AUTHORIZED") {
                    val uid = json.optString("uid", "")
                    val email = json.optString("email", "")
                    if (uid.isNotEmpty()) {
                        settings.linkedUid = uid
                        settings.linkedEmail = email
                        return Pair(uid, email)
                    }
                }
            } catch (e: Exception) {
                println("[DEBUG] WebSync: checkQRSession error: ${e.message}")
            }
        }
        return null
    }

    override suspend fun reportServerFailure(serverId: String, serverName: String, reason: String) {
        val body = JSONObject().apply {
            put("serverId", serverId)
            put("serverName", serverName)
            put("reason", reason)
        }.toString()
        executeRequest("$apiBaseUrl/server_health.php?action=report_failure", "POST", body)
    }

    override suspend fun reportServerSuccess(serverId: String, serverName: String, ping: Float, dlSpeed: Double, ulSpeed: Double) {
        val body = JSONObject().apply {
            put("serverId", serverId)
            put("serverName", serverName)
            put("ping", ping)
            put("dlSpeed", dlSpeed)
            put("ulSpeed", ulSpeed)
        }.toString()
        executeRequest("$apiBaseUrl/server_health.php?action=report_success", "POST", body)
    }

    override suspend fun fetchGlobalServerHealth(): String? {
        return executeRequest("$apiBaseUrl/server_health.php?action=get_health", "GET")
    }

    override suspend fun fetchUserDevices(): String? {
        val uid = getUserId()
        if (uid.isEmpty()) return null
        return executeRequest("$apiBaseUrl/devices.php?action=list&uid=$uid", "GET")
    }

    override suspend fun triggerRemoteTest(targetDeviceId: String?): Boolean {
        val uid = getUserId()
        if (uid.isEmpty()) return false
        val targetParam = if (!targetDeviceId.isNullOrBlank()) "&targetDeviceId=$targetDeviceId" else ""
        val response = executeRequest("$apiBaseUrl/remote_command.php?action=trigger&uid=$uid$targetParam", "GET")
        return response != null && response.contains("success")
    }

    override suspend fun fetchRemoteMirror(): String? {
        val uid = getUserId()
        if (uid.isEmpty()) return null
        return executeRequest("$apiBaseUrl/remote_command.php?action=get_mirror&uid=$uid", "GET")
    }

    fun startBackgroundServices(onRemoteTest: () -> Unit) {
        scope.launch {
            while (isActive) {
                try {
                    val uid = getUserId()
                    val devId = getDeviceId()
                    val devName = getDeviceName()
                    val response = executeRequest("$apiBaseUrl/remote_command.php?uid=$uid&deviceId=$devId&deviceName=${URLEncoder.encode(devName, "UTF-8")}", "GET")
                    if (response != null) {
                        val json = JSONObject(response)
                        if (json.optBoolean("triggerTest", false)) {
                            withContext(Dispatchers.Main) {
                                onRemoteTest()
                            }
                        }
                    }
                } catch (e: Exception) {}
                delay(3000)
            }
        }
    }

    override suspend fun fetchIspServiceInfo(ip: String?): Map<String, String>? {
        val targetParam = if (!ip.isNullOrBlank()) "&ip=$ip" else ""
        val response = executeRequest("https://mrspeed.ca/isp_service.php?action=isp$targetParam", "GET")

        if (response != null && response.contains("{")) {
            try {
                val json = JSONObject(response)
                val resolvedIp = json.optString("ip", "")
                val resolvedIsp = json.optString("isp", json.optString("org", "Unknown ISP"))
                val country = json.optString("country", "Global")
                val flag = json.optString("flag", "🌐")
                val city = json.optString("city", "")

                return mapOf(
                    "ip" to resolvedIp,
                    "isp" to resolvedIsp,
                    "country" to country,
                    "flag" to flag,
                    "city" to city,
                    "processedString" to json.optString("processedString", "$resolvedIp - $resolvedIsp, $country $flag")
                )
            } catch (e: Exception) {
                println("[DEBUG] WebSync: fetchIspServiceInfo error: ${e.message}")
            }
        }
        return null
    }

    override suspend fun fetchCaptchaToken(): String? {
        try {
            val response = executeRequest("$apiBaseUrl/captcha.php?action=create", "GET") ?: return null
            val json = JSONObject(response)
            val nonce = json.optString("challenge_id", "")
            val sig = json.optString("sig", "")
            val ts = json.optLong("ts", 0L)
            if (nonce.isEmpty() || sig.isEmpty()) return null

            val solveBody = JSONObject().apply {
                put("challenge_id", nonce)
                put("sig", sig)
                put("ts", ts)
            }.toString()
            val solveResp = executeRequest("$apiBaseUrl/captcha.php?action=solve", "POST", solveBody) ?: return null
            val solveJson = JSONObject(solveResp)
            return solveJson.optString("captcha_token", "").takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            return null
        }
    }
}
