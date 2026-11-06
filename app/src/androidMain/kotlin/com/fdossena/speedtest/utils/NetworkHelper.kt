package com.fdossena.speedtest.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NetworkHelper(private val context: Context) {
    fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun getLocalProviderName(): String? {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val networkName = telephonyManager.networkOperatorName
            if (!networkName.isNullOrBlank() && networkName.lowercase() != "android") {
                return networkName
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun fetchExternalISP(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://mrspeed.ca/isp_service.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)

            if (json.has("rawIspInfo")) {
                val rawInfo = json.getJSONObject("rawIspInfo")
                val org = rawInfo.optString("org")
                if (!org.isNullOrBlank()) return@withContext org
            }

            val proc = json.optString("processedString")
            if (!proc.isNullOrBlank() && proc.contains(" - ")) {
                val org = proc.substringAfter(" - ").substringBefore(",").trim()
                if (org.isNotBlank()) return@withContext org
            }

            return@withContext null
        } catch (e: Exception) {
            try {
                // Fallback API
                val url = URL("https://mrspeed.ca/isp_service.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val org = json.optString("processedString")
                if (org.isNullOrBlank()) return@withContext null
                return@withContext org
            } catch (e2: Exception) {
                null
            }
        }
    }
}
