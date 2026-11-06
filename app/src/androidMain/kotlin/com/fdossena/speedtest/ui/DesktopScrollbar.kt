package com.fdossena.speedtest.ui

import android.view.InputDevice
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun rememberIsMouseOrChromeOS(): Boolean {
    val context = LocalContext.current
    var isMouseOrChromeOS by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val isChromeOS = pm.hasSystemFeature("org.chromium.arc") || 
                         pm.hasSystemFeature("org.chromium.arc.device_management") ||
                         pm.hasSystemFeature("android.hardware.type.pc")
        if (isChromeOS) {
            isMouseOrChromeOS = true
        } else {
            val ids = InputDevice.getDeviceIds()
            for (id in ids) {
                val dev = InputDevice.getDevice(id)
                if (dev != null && !dev.isVirtual && 
                    (dev.supportsSource(InputDevice.SOURCE_MOUSE) || dev.supportsSource(InputDevice.SOURCE_TOUCHPAD) || dev.supportsSource(InputDevice.SOURCE_TRACKBALL))) {
                    isMouseOrChromeOS = true
                    break
                }
            }
        }
    }
    return isMouseOrChromeOS
}

@Composable
actual fun DesktopLazyScrollbar(
    lazyListState: LazyListState,
    isLight: Boolean,
    modifier: Modifier
) {
    val showScrollbar = rememberIsMouseOrChromeOS()
    if (showScrollbar) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(6.dp)
                .padding(vertical = 4.dp)
                .background(
                    if (isLight) Color(0xFFFF9F0A).copy(alpha = 0.6f) else Color(0xFF00E5FF).copy(alpha = 0.6f),
                    RoundedCornerShape(3.dp)
                )
        )
    }
}

@Composable
actual fun DesktopVerticalScrollbar(
    scrollState: ScrollState,
    isLight: Boolean,
    modifier: Modifier
) {
    val showScrollbar = rememberIsMouseOrChromeOS()
    if (showScrollbar) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(6.dp)
                .padding(vertical = 4.dp)
                .background(
                    if (isLight) Color(0xFFFF9F0A).copy(alpha = 0.6f) else Color(0xFF00E5FF).copy(alpha = 0.6f),
                    RoundedCornerShape(3.dp)
                )
        )
    }
}

@Composable
actual fun DesktopScrollableContainer(
    modifier: Modifier,
    scrollState: ScrollState,
    isLight: Boolean,
    horizontalAlignment: Alignment.Horizontal,
    content: @Composable ColumnScope.() -> Unit
) {
    val showScrollbar = rememberIsMouseOrChromeOS()
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (showScrollbar) 8.dp else 0.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
        if (showScrollbar) {
            DesktopVerticalScrollbar(
                scrollState = scrollState,
                isLight = isLight,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp)
            )
        }
    }
}
