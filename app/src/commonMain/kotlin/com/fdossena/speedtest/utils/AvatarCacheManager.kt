package com.fdossena.speedtest.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object AvatarCacheManager {
    private val localPathMap = mutableMapOf<String, String>()

    private fun getCacheDir(): File {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = File(userHome, ".mrspeed_cache/avatars")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun md5(str: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(str.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            str.hashCode().toString()
        }
    }

    suspend fun downloadAndCacheAvatar(remoteUrl: String): String {
        if (remoteUrl.isBlank()) return ""
        val fileName = "avatar_" + md5(remoteUrl) + ".png"
        val destFile = File(getCacheDir(), fileName)

        if (destFile.exists() && destFile.length() > 0) {
            localPathMap[remoteUrl] = destFile.absolutePath
            return destFile.absolutePath
        }

        return withContext(Dispatchers.IO) {
            try {
                val conn = URL(remoteUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "MrSpeedApp/1.0")

                if (conn.responseCode == 200) {
                    conn.inputStream.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (destFile.exists() && destFile.length() > 0) {
                        localPathMap[remoteUrl] = destFile.absolutePath
                        return@withContext destFile.absolutePath
                    }
                }
            } catch (e: Exception) {
                println("[DEBUG] AvatarCacheManager download error: ${e.message}")
            }
            return@withContext remoteUrl
        }
    }

    suspend fun preloadAllAvatarsOnStartup(userEmail: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                // Default fallback URLs
                val defaultUrls = listOf(
                    "https://mrspeed.ca/images/Futuristic Robot Executive Portrait.png",
                    "https://mrspeed.ca/backend/api/images/user_default_option3_gradient.svg",
                    "https://mrspeed.ca/backend/api/images/agent_avatar.svg"
                )
                defaultUrls.forEach { downloadAndCacheAvatar(it) }

                // User Gravatar if email present
                if (!userEmail.isNullOrBlank() && userEmail.contains("@")) {
                    val cleanEmail = userEmail.trim().lowercase()
                    val hash = md5(cleanEmail)
                    val gravatarUrl = "https://www.gravatar.com/avatar/$hash?s=128&d=mp"
                    downloadAndCacheAvatar(gravatarUrl)
                }

                // Fetch site avatar configuration
                val emailParam = if (!userEmail.isNullOrBlank()) "?user_email=${java.net.URLEncoder.encode(userEmail, "UTF-8")}" else ""
                val url = URL("https://mrspeed.ca/backend/api/livechat.php?action=get_all_avatars$emailParam")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val rawText = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                val jsonStart = rawText.indexOf('{')
                if (jsonStart >= 0) {
                    val jsonText = rawText.substring(jsonStart)
                    val json = JSONObject(jsonText)

                    if (json.optBoolean("success")) {
                        val aiAvatar = json.optString("ai_avatar", "")
                        val userAvatar = json.optString("user_avatar", "")
                        val agentAvatars = json.optJSONArray("agent_avatars") as? JSONArray

                        if (aiAvatar.isNotEmpty()) downloadAndCacheAvatar(aiAvatar)
                        if (userAvatar.isNotEmpty()) downloadAndCacheAvatar(userAvatar)

                        if (agentAvatars != null) {
                            for (i in 0 until agentAvatars.length()) {
                                val aUrl = agentAvatars.optString(i, "")
                                if (aUrl.isNotEmpty()) downloadAndCacheAvatar(aUrl)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("[DEBUG] AvatarCacheManager startup error: ${e.message}")
            }
        }
    }

    fun getCachedModelOrRemote(remoteUrl: String): Any {
        if (remoteUrl.isBlank()) return ""
        val cachedPath = localPathMap[remoteUrl]
        if (cachedPath != null) {
            val file = File(cachedPath)
            if (file.exists() && file.length() > 0) {
                return file
            }
        }
        val fileName = "avatar_" + md5(remoteUrl) + ".png"
        val file = File(getCacheDir(), fileName)
        if (file.exists() && file.length() > 0) {
            localPathMap[remoteUrl] = file.absolutePath
            return file
        }
        return remoteUrl
    }
}
