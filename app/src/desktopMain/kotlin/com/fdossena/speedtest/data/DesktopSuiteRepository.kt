package com.fdossena.speedtest.data

import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class DesktopSuiteRepository : SuiteRepository {
    private val file = File(System.getProperty("user.home"), ".mrspeed_suite.json")
    private val _domains = MutableStateFlow<List<TrackedDomain>>(load())
    override fun getTrackedDomains(): Flow<List<TrackedDomain>> = _domains.asStateFlow()

    private fun load(): List<TrackedDomain> {
        return if (file.exists()) {
            try {
                Json.decodeFromString<List<TrackedDomain>>(file.readText())
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    private fun save(list: List<TrackedDomain>) {
        try {
            file.writeText(Json.encodeToString(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun addDomain(name: String) {
        // Simplified: just add without whois for now on desktop
        val domain = TrackedDomain(name)
        val newList = (_domains.value + domain).distinctBy { it.domainName }
        _domains.value = newList
        save(newList)
    }

    override suspend fun removeDomain(domain: TrackedDomain) {
        val newList = _domains.value.filter { it.domainName != domain.domainName }
        _domains.value = newList
        save(newList)
    }

    override suspend fun refreshDomain(name: String) {
    }
}
