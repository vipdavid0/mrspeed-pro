package com.fdossena.speedtest.data

import kotlinx.coroutines.flow.Flow

interface SuiteRepository {
    fun getTrackedDomains(): Flow<List<TrackedDomain>>
    suspend fun addDomain(name: String)
    suspend fun removeDomain(domain: TrackedDomain)
    suspend fun refreshDomain(name: String)
}
