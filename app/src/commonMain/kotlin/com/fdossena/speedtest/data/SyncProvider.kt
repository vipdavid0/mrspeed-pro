package com.fdossena.speedtest.data

interface SyncProvider {
    suspend fun initialize()
    fun getUserId(): String?
    fun getUserEmail(): String?
    fun getUserRole(): String = "user"
    fun isAdmin(): Boolean = getUserRole().lowercase() == "admin"
    fun isAnonymous(): Boolean
    fun getLastSync(): Long
    fun getDeviceId(): String
    suspend fun registerDevice(deviceName: String, deviceType: String)
    suspend fun getDeviceName(): String
    suspend fun syncSettings()
    suspend fun uploadResult(result: TestResult)
    suspend fun fetchResults(): List<TestResult>
    suspend fun performManualSync(results: List<TestResult>)
    suspend fun signIn(email: String, pass: String, captchaToken: String? = null)
    suspend fun signUp(email: String, pass: String, name: String = "", phone: String = "", captchaToken: String? = null)
    suspend fun updatePhone(phone: String) {}
    suspend fun sendTest2FaSms(): String? = null
    suspend fun approve2Fa(reqId: String, allow: Boolean) {}
    suspend fun verifyTotpCode(uid: String, code: String): Boolean = false
    suspend fun sendEmergencyOtp(uid: String, email: String): Boolean = false
    suspend fun verifyEmergencyOtp(uid: String, otp: String): Boolean = false
    suspend fun verifyRecoveryKey(uid: String, key: String): Boolean = false
    suspend fun check2FaStatus(reqId: String): Triple<String, String, String>? = Triple("PENDING", "", "")
    suspend fun fetchProfile(): Pair<String, String>? = null
    suspend fun signOut()
    suspend fun updateEmail(email: String, pass: String)
    suspend fun updatePassword(old: String, new: String)
    suspend fun deleteAccount()
    suspend fun resetPassword(email: String)
    suspend fun toggleWatchlist(serviceId: String)
    suspend fun authorizeQRLogin(sessionId: String)
    suspend fun initializeQRSession(sessionId: String)
    fun listenToQRLogin(sessionId: String, onAuthorized: (uid: String, email: String) -> Unit, on2FaRequired: ((reqId: String, uid: String, email: String) -> Unit)? = null)
    fun stopQRListening()
    suspend fun checkQRSession(sessionId: String): Pair<String, String>? = null
    
    suspend fun fetchISPLeaderboard(): List<ISPRecord>
    
    suspend fun performFirestoreRequest(urlPath: String, method: String, body: String? = null): String?

    suspend fun reportServerFailure(serverId: String, serverName: String, reason: String) {}
    suspend fun reportServerSuccess(serverId: String, serverName: String, ping: Float, dlSpeed: Double, ulSpeed: Double) {}
    suspend fun fetchGlobalServerHealth(): String? = null

    suspend fun fetchUserDevices(): String? = null
    suspend fun triggerRemoteTest(targetDeviceId: String? = null): Boolean = false
    suspend fun fetchRemoteMirror(): String? = null
    suspend fun fetchIspServiceInfo(ip: String? = null): Map<String, String>? = null
    suspend fun fetchCaptchaToken(): String? = null
}
