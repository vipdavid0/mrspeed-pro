package com.fdossena.speedtest.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector

// --- COMPATIBILITY SHIMS FOR MULTIPLATFORM ---

class AutofillNode(
    val id: Int = 0, 
    var boundingBox: Rect = Rect.Zero, 
    val autofillTypes: List<AutofillType> = emptyList(), 
    val onFill: (String) -> Unit = {}
)

object LocalAutofill { val current: Any? = null }
object LocalAutofillTree { val current: AutofillTree = AutofillTree() }
class AutofillTree { 
    val children = mutableMapOf<Int, AutofillNode>() 
    operator fun plusAssign(node: AutofillNode) { children[node.id] = node }
}
enum class AutofillType { EmailAddress, Username, Password, NewUsername, NewPassword }
object QRGenerator { fun generateQRCode(data: String?, size: Int, color: Int): Any? = null }
fun Any?.asImageBitmap(): ImageBitmap = ImageBitmap(1, 1)

typealias Autofill = Any

fun Any?.requestAutofillForNode(node: AutofillNode) {}

annotation class ExperimentalComposeUiApi

@Composable
expect fun DesktopLazyScrollbar(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    isLight: Boolean,
    modifier: Modifier = Modifier
)

@Composable
expect fun DesktopVerticalScrollbar(
    scrollState: ScrollState,
    isLight: Boolean,
    modifier: Modifier = Modifier
)

@Composable
expect fun DesktopScrollableContainer(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    isLight: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
)
