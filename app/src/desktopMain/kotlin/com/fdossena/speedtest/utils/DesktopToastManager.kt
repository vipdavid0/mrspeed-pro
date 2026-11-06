package com.fdossena.speedtest.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DesktopToastData(
    val title: String,
    val message: String,
    val isLightMode: Boolean,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null,
    val persistent: Boolean = false
)

object DesktopToastManager {
    private val _activeToast = MutableStateFlow<DesktopToastData?>(null)
    val activeToast: StateFlow<DesktopToastData?> = _activeToast.asStateFlow()

    fun showToast(
        title: String,
        message: String,
        isLightMode: Boolean,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
        persistent: Boolean = false
    ) {
        _activeToast.value = DesktopToastData(title, message, isLightMode, actionLabel, onAction, onDismiss, persistent)
    }

    fun clearToast() {
        _activeToast.value = null
    }
}
