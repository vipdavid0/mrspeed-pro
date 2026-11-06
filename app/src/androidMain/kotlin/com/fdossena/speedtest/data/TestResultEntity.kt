package com.fdossena.speedtest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val downloadSpeed: Double,
    val uploadSpeed: Double,
    val ping: Double,
    val jitter: Double,
    val serverName: String,
    val timestamp: Long,
    val testId: String? = null,
    val downloadDataUsed: Long = 0,
    val uploadDataUsed: Long = 0,
    val sponsorName: String? = null,
    val ipInfo: String? = null,
    val source: String = "Android App"
)

fun TestResultEntity.toDomain() = TestResult(
    id = id,
    downloadSpeed = downloadSpeed,
    uploadSpeed = uploadSpeed,
    ping = ping,
    jitter = jitter,
    serverName = serverName,
    timestamp = timestamp,
    testId = testId,
    downloadDataUsed = downloadDataUsed,
    uploadDataUsed = uploadDataUsed,
    sponsorName = sponsorName,
    ipInfo = ipInfo,
    source = source
)

fun TestResult.toEntity() = TestResultEntity(
    id = id,
    downloadSpeed = downloadSpeed,
    uploadSpeed = uploadSpeed,
    ping = ping,
    jitter = jitter,
    serverName = serverName,
    timestamp = timestamp,
    testId = testId,
    downloadDataUsed = downloadDataUsed,
    uploadDataUsed = uploadDataUsed,
    sponsorName = sponsorName,
    ipInfo = ipInfo,
    source = source
)
