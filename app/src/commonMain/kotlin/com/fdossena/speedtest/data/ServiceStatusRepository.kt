package com.fdossena.speedtest.data

import kotlinx.coroutines.flow.Flow

interface ServiceStatusRepository {
    fun getServiceStatuses(): Flow<List<ServiceStatus>>
    val isSyncing: Flow<Boolean>
    var isTesting: Boolean
    suspend fun addCustomService(name: String, url: String)
    suspend fun reportIssue(serviceId: String)
    suspend fun seedInitialServices(jsonString: String? = null)
}
