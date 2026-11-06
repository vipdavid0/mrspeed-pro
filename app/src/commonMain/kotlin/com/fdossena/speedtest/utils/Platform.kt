package com.fdossena.speedtest.utils

import androidx.compose.runtime.Composable

expect fun openUrl(url: String)
expect fun shareResult(text: String)

@Composable
expect fun PlatformBackHandler(onBack: () -> Unit)

@Composable
expect fun QRScanner(onScanned: (String) -> Unit, onDismiss: () -> Unit)

@Composable
expect fun rememberHasCamera(): Boolean

@Composable
expect fun PlatformWebView(url: String, modifier: androidx.compose.ui.Modifier)
