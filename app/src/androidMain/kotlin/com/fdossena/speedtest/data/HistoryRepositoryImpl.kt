package com.fdossena.speedtest.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(private val testResultDao: TestResultDao) : HistoryRepository {
    override val allResults: Flow<List<TestResult>> = testResultDao.getAllResults().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun getLastResult(): TestResult? {
        return testResultDao.getLastResult()?.toDomain()
    }

    override suspend fun exists(timestamp: Long): Boolean {
        return testResultDao.countByTimestamp(timestamp) > 0
    }

    override suspend fun insert(testResult: TestResult) {
        testResultDao.insert(testResult.toEntity())
    }

    override suspend fun delete(testResult: TestResult) {
        testResultDao.delete(testResult.toEntity())
    }

    override suspend fun deleteAll() {
        testResultDao.deleteAll()
    }
}
