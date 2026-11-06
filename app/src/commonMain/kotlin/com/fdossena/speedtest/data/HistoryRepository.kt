package com.fdossena.speedtest.data

import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    val allResults: Flow<List<TestResult>>
    suspend fun insert(testResult: TestResult)
    suspend fun delete(testResult: TestResult)
    suspend fun deleteAll()
    suspend fun exists(timestamp: Long): Boolean
    suspend fun getLastResult(): TestResult?
}
