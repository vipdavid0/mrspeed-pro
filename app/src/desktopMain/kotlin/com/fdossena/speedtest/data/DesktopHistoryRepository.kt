package com.fdossena.speedtest.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class DesktopHistoryRepository(
    private val syncManager: SyncProvider
) : HistoryRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val file = File(System.getProperty("user.home"), ".mrspeed_history.json")
    private val _results = MutableStateFlow<List<TestResult>>(load())
    override val allResults: Flow<List<TestResult>> = _results.asStateFlow()

    init {
        scope.launch {
            delay(5000) // Wait for auth initialization
            syncFromCloud()
        }
    }

    private suspend fun syncFromCloud() {
        println("[DEBUG] Repository: Fetching history from cloud...")
        try {
            val cloudResults = syncManager.fetchResults()
            if (cloudResults.isNotEmpty()) {
                val current = _results.value
                val merged = (cloudResults + current).distinctBy { it.timestamp }.sortedByDescending { it.timestamp }
                _results.value = merged
                save(merged)
                println("[DEBUG] Repository: Merged ${cloudResults.size} cloud results.")
            }
        } catch (e: Exception) {
            println("[DEBUG] Repository: Cloud fetch failed: ${e.message}")
        }
    }

    private fun load(): List<TestResult> {
        return if (file.exists()) {
            try {
                Json.decodeFromString<List<TestResult>>(file.readText())
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    private fun save(list: List<TestResult>) {
        try {
            file.writeText(Json.encodeToString(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun insert(testResult: TestResult) {
        val newList = (listOf(testResult) + _results.value).distinctBy { it.timestamp }
        _results.value = newList
        save(newList)
    }

    override suspend fun delete(testResult: TestResult) {
        val newList = _results.value.filter { it.timestamp != testResult.timestamp }
        _results.value = newList
        save(newList)
    }

    override suspend fun deleteAll() {
        _results.value = emptyList()
        if (file.exists()) file.delete()
    }

    override suspend fun exists(timestamp: Long): Boolean {
        return _results.value.any { it.timestamp == timestamp }
    }

    override suspend fun getLastResult(): TestResult? {
        return _results.value.maxByOrNull { it.timestamp }
    }
}
