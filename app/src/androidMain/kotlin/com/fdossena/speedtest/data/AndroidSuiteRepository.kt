package com.fdossena.speedtest.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AndroidSuiteRepository(private val dao: TrackedDomainDao) : SuiteRepository {
    override fun getTrackedDomains(): Flow<List<TrackedDomain>> = 
        dao.getAllTrackedDomains().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addDomain(name: String) {
        val domain = fetchWhoisData(name)
        dao.insert(domain.toEntity())
    }

    override suspend fun removeDomain(domain: TrackedDomain) = 
        dao.delete(domain.toEntity())

    override suspend fun refreshDomain(name: String) {
        val domain = fetchWhoisData(name)
        dao.insert(domain.toEntity())
    }

    private fun fetchWhoisData(name: String): TrackedDomain {
        return try {
            val encoded = URLEncoder.encode(name, "UTF-8")
            val urlStr = "https://mrspeed.ca/isp_service.php?action=whois&query=$encoded"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "MrSpeed-Android")
            
            val text = if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else ""
            
            val json = Json.parseToJsonElement(text).jsonObject
            val raw = json["raw"]?.jsonPrimitive?.content ?: ""
            var registrar = "Unknown"
            var regDate = "Unknown"
            var expDate = "Unknown"
            
            if (raw.isNotEmpty()) {
                val lines = raw.split("\n")
                for (line in lines) {
                    val l = line.trim()
                    if (l.startsWith("Registrar:", ignoreCase = true)) registrar = l.substring(10).trim()
                    if (l.startsWith("Creation Date:", ignoreCase = true)) regDate = l.substring(14).trim().take(10)
                    if (l.startsWith("Registry Expiry Date:", ignoreCase = true)) expDate = l.substring(21).trim().take(10)
                }
            }
            
            TrackedDomain(
                domainName = name,
                registrar = registrar,
                registrationDate = regDate,
                expirationDate = expDate,
                lastChecked = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            TrackedDomain(name)
        }
    }
}
