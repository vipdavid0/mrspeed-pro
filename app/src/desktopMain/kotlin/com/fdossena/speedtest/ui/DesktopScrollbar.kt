package com.fdossena.speedtest.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.fdossena.speedtest.utils.cursorHoverable

@Composable
actual fun DesktopLazyScrollbar(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    isLight: Boolean,
    modifier: Modifier
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(lazyListState),
        modifier = modifier
            .cursorHoverable()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        style = ScrollbarStyle(
            minimalHeight = 36.dp,
            thickness = 12.dp,
            shape = RoundedCornerShape(6.dp),
            hoverColor = if (isLight) Color(0xFFFF9F0A) else Color(0xFF00E5FF),
            unhoverColor = if (isLight) Color(0xFFFF9F0A).copy(alpha = 0.85f) else Color(0xFF00E5FF).copy(alpha = 0.85f),
            hoverDurationMillis = 0
        )
    )
}

@Composable
actual fun DesktopVerticalScrollbar(
    scrollState: ScrollState,
    isLight: Boolean,
    modifier: Modifier
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier
            .cursorHoverable()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        style = ScrollbarStyle(
            minimalHeight = 36.dp,
            thickness = 12.dp,
            shape = RoundedCornerShape(6.dp),
            hoverColor = if (isLight) Color(0xFFFF9F0A) else Color(0xFF00E5FF),
            unhoverColor = if (isLight) Color(0xFFFF9F0A).copy(alpha = 0.85f) else Color(0xFF00E5FF).copy(alpha = 0.85f),
            hoverDurationMillis = 0
        )
    )
}

@Composable
actual fun DesktopScrollableContainer(
    modifier: Modifier,
    scrollState: ScrollState,
    isLight: Boolean,
    horizontalAlignment: Alignment.Horizontal,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
        if (com.fdossena.speedtest.utils.isDesktop) {
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
