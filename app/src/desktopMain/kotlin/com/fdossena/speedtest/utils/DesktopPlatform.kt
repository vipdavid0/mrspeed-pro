package com.fdossena.speedtest.utils

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import java.awt.Desktop
import java.net.URI
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual fun openUrl(url: String) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI(url))
    }
}

actual fun shareResult(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val selection = StringSelection(text)
    clipboard.setContents(selection, selection)
}

@Composable
actual fun PlatformBackHandler(onBack: () -> Unit) {
}

@Composable
actual fun QRScanner(onScanned: (String) -> Unit, onDismiss: () -> Unit) {
    Box { Text("QR Scanning not supported on Desktop. Use manual link.") }
}

@Composable
actual fun rememberHasCamera(): Boolean = false

@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    Box(modifier) { Text("Browser: $url") }
}

fun showDesktopNotification(title: String, message: String) {
    if (SystemTray.isSupported()) {
        val tray = SystemTray.getSystemTray()
        val icon = tray.trayIcons.firstOrNull()
        if (icon != null) {
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO)
        }
    }
}
