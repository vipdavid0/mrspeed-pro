package com.fdossena.speedtest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fdossena.speedtest.data.*
import com.fdossena.speedtest.utils.PlatformService

class MainViewModelFactory(
    private val repository: HistoryRepository,
    private val settingsManager: SettingsProvider,
    private val assetReader: AssetReader,
    private val syncManager: SyncProvider,
    private val serviceStatusRepository: ServiceStatusRepository,
    private val platformService: PlatformService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, settingsManager, assetReader, syncManager, serviceStatusRepository, platformService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
