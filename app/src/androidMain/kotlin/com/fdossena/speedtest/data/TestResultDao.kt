package com.fdossena.speedtest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestResultDao {
    @Insert
    suspend fun insert(testResult: TestResultEntity)

    @Query("SELECT * FROM test_results ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastResult(): TestResultEntity?

    @Query("SELECT * FROM test_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<TestResultEntity>>

    @Delete
    suspend fun delete(testResult: TestResultEntity)

    @Query("DELETE FROM test_results")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM test_results WHERE timestamp = :timestamp")
    suspend fun countByTimestamp(timestamp: Long): Int
}
