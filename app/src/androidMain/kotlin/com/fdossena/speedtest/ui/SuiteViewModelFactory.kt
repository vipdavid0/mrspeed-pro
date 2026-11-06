package com.fdossena.speedtest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fdossena.speedtest.data.*

class SuiteViewModelFactory(
    private val repository: SuiteRepository,
    private val settingsManager: SettingsProvider
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuiteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuiteViewModel(repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
