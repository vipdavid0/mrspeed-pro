package com.fdossena.speedtest.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val MultiConnectionShuffleIcon: ImageVector
    get() {
        if (_multiConnectionShuffleIcon != null) return _multiConnectionShuffleIcon!!
        _multiConnectionShuffleIcon = ImageVector.Builder(
            name = "MultiConnectionShuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 3f)
                lineTo(21f, 3f)
                lineTo(21f, 8f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 20f)
                lineTo(21f, 3f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 16f)
                lineTo(21f, 21f)
                lineTo(16f, 21f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 15f)
                lineTo(21f, 21f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                lineTo(9f, 9f)
            }
        }.build()
        return _multiConnectionShuffleIcon!!
    }

private var _multiConnectionShuffleIcon: ImageVector? = null

val SingleConnectionArrowIcon: ImageVector
    get() {
        if (_singleConnectionArrowIcon != null) return _singleConnectionArrowIcon!!
        _singleConnectionArrowIcon = ImageVector.Builder(
            name = "SingleConnectionArrow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12f)
                lineTo(19f, 12f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 5f)
                lineTo(19f, 12f)
                lineTo(12f, 19f)
            }
        }.build()
        return _singleConnectionArrowIcon!!
    }

private var _singleConnectionArrowIcon: ImageVector? = null

val ChatBubbleSvgIcon: ImageVector
    get() {
        if (_chatBubbleSvgIcon != null) return _chatBubbleSvgIcon!!
        _chatBubbleSvgIcon = ImageVector.Builder(
            name = "ChatBubbleSvg",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 11.5f)
                curveTo(21f, 16.75f, 16.75f, 21f, 11.5f, 21f)
                curveTo(9.85f, 21f, 8.3f, 20.55f, 7f, 19.75f)
                lineTo(3f, 21f)
                lineTo(4.25f, 17f)
                curveTo(3.45f, 15.7f, 3f, 14.15f, 3f, 12.5f)
                curveTo(3f, 7.25f, 7.25f, 3f, 12.5f, 3f)
                curveTo(17.75f, 3f, 21f, 7.25f, 21f, 11.5f)
                close()
            }
        }.build()
        return _chatBubbleSvgIcon!!
    }

private var _chatBubbleSvgIcon: ImageVector? = null

class CursorHoverManager {
    var hoverCount by mutableStateOf(0)
    var disabledHoverCount by mutableStateOf(0)
    fun increment() { hoverCount++ }
    fun decrement() { if (hoverCount > 0) hoverCount-- }
    fun incrementDisabled() { disabledHoverCount++ }
    fun decrementDisabled() { if (disabledHoverCount > 0) disabledHoverCount-- }
}

val LocalCursorHoverManager = compositionLocalOf { CursorHoverManager() }

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.cursorHoverable(enabled: Boolean = true): Modifier = this.composed {
    val hoverManager = LocalCursorHoverManager.current
    this.pointerInput(enabled) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                when (event.type) {
                    PointerEventType.Enter -> {
                        if (enabled) hoverManager.increment()
                        else hoverManager.incrementDisabled()
                    }
                    PointerEventType.Exit -> {
                        if (enabled) hoverManager.decrement()
                        else hoverManager.decrementDisabled()
                    }
                }
            }
        }
    }
}

fun Modifier.appClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: androidx.compose.ui.semantics.Role? = null,
    onClick: () -> Unit
): Modifier = this.composed {
    this.clickable(enabled = enabled, onClickLabel = onClickLabel, role = role, onClick = onClick).cursorHoverable()
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: androidx.compose.foundation.BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered, enabled) {
        if (isHovered) {
            if (enabled) hoverManager.increment() else hoverManager.incrementDisabled()
        } else {
            if (enabled) hoverManager.decrement() else hoverManager.decrementDisabled()
        }
    }

    Button(
        onClick = onClick,
        modifier = modifier.cursorHoverable(enabled),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered, enabled) {
        if (isHovered) {
            if (enabled) hoverManager.increment() else hoverManager.incrementDisabled()
        } else {
            if (enabled) hoverManager.decrement() else hoverManager.decrementDisabled()
        }
    }

    IconButton(
        onClick = onClick,
        modifier = modifier.cursorHoverable(enabled),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: androidx.compose.foundation.BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered, enabled) {
        if (isHovered) {
            if (enabled) hoverManager.increment() else hoverManager.incrementDisabled()
        } else {
            if (enabled) hoverManager.decrement() else hoverManager.decrementDisabled()
        }
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.cursorHoverable(enabled),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: androidx.compose.foundation.BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered, enabled) {
        if (isHovered) {
            if (enabled) hoverManager.increment() else hoverManager.incrementDisabled()
        } else {
            if (enabled) hoverManager.decrement() else hoverManager.decrementDisabled()
        }
    }

    TextButton(
        onClick = onClick,
        modifier = modifier.cursorHoverable(enabled),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered, enabled) {
        if (isHovered) {
            if (enabled) hoverManager.increment() else hoverManager.incrementDisabled()
        } else {
            if (enabled) hoverManager.decrement() else hoverManager.decrementDisabled()
        }
    }

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.cursorHoverable(enabled),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered, enabled) {
        if (isHovered) {
            if (enabled) hoverManager.increment() else hoverManager.incrementDisabled()
        } else {
            if (enabled) hoverManager.decrement() else hoverManager.decrementDisabled()
        }
    }

    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.cursorHoverable(enabled),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}

@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered) {
        if (isHovered) hoverManager.increment()
        else hoverManager.decrement()
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.cursorHoverable(),
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        interactionSource = interactionSource
    )
}

@Composable
fun AppSuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = SuggestionChipDefaults.shape,
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
    elevation: ChipElevation = SuggestionChipDefaults.suggestionChipElevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    icon: @Composable (() -> Unit)? = null
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered) {
        if (isHovered) hoverManager.increment()
        else hoverManager.decrement()
    }

    SuggestionChip(
        onClick = onClick,
        label = label,
        modifier = modifier.cursorHoverable(),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = SuggestionChipDefaults.suggestionChipBorder(enabled),
        interactionSource = interactionSource,
        icon = icon
    )
}

@Composable
fun AppDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered) {
        if (isHovered) hoverManager.increment()
        else hoverManager.decrement()
    }

    DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier.cursorHoverable(),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    )
}

@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: androidx.compose.ui.graphics.Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered) {
        if (isHovered) hoverManager.increment()
        else hoverManager.decrement()
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.cursorHoverable(),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverManager = LocalCursorHoverManager.current
    LaunchedEffect(isHovered) {
        if (isHovered) hoverManager.increment()
        else hoverManager.decrement()
    }

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.cursorHoverable(),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource
    )
}

@Composable
fun CustomOpalCursorOverlay(
    isLightMode: Boolean,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }

    val hoverManager = remember { CursorHoverManager() }
    var mousePosition by remember { mutableStateOf(Offset(-100f, -100f)) }
    var isVisible by remember { mutableStateOf(false) }
    var isPressedState by remember { mutableStateOf(false) }

    val isHovering = isPressedState || (hoverManager.hoverCount > 0)

    val basePrimaryColor = if (isLightMode) Color(0xFFFF9F0A) else Color(0xFF00E5FF)
    val baseSecondaryColor = if (isLightMode) Color(0xFFFF6400) else Color(0xFFB066FF)
    
    // Dark theme hover: glow purple with a green inner circle
    val hoverPrimaryColor = if (isLightMode) Color(0xFF4F46E5) else Color(0xFFB066FF)
    val hoverSecondaryColor = if (isLightMode) Color(0xFFFF9F0A) else Color(0xFF00E5FF)

    val currentPrimaryColor by animateColorAsState(
        targetValue = if (isHovering) hoverPrimaryColor else basePrimaryColor,
        animationSpec = tween(200)
    )
    val currentSecondaryColor by animateColorAsState(
        targetValue = if (isHovering) hoverSecondaryColor else baseSecondaryColor,
        animationSpec = tween(200)
    )

    val targetSize = if (isHovering) 46.dp else 30.dp
    val animatedRingSize by animateDpAsState(
        targetValue = targetSize,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val spinTransition = rememberInfiniteTransition(label = "disabledSpin")
    val disabledSpinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    CompositionLocalProvider(LocalCursorHoverManager provides hoverManager) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                mousePosition = change.position
                                isVisible = true
                                isPressedState = change.pressed
                            }
                        }
                    }
                }
        ) {
            content()

            if (isVisible) {
                val isHoveringDisabled = (hoverManager.disabledHoverCount > 0)
                val ringSize = animatedRingSize * pulseScale

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (mousePosition.x - ringSize.toPx() / 2).toInt(),
                                (mousePosition.y - ringSize.toPx() / 2).toInt()
                            )
                        }
                        .size(ringSize)
                ) {
                    if (isHoveringDisabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(2.5.dp, Color(0xFFFF3232), CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF3232).copy(alpha = 0.5f),
                                            Color(0xFFFF0055).copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin.Center
                                        rotationZ = disabledSpinAngle
                                    }
                            ) {
                                val symbolColor = Color(0xFFFF3232)
                                val strokeWidth = 2.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2f
                                val centerPt = Offset(size.width / 2f, size.height / 2f)

                                // Red Circle
                                drawCircle(
                                    color = symbolColor,
                                    radius = radius,
                                    center = centerPt,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                )

                                // Red Diagonal Slash
                                val offsetAmount = radius * 0.7071f
                                drawLine(
                                    color = symbolColor,
                                    start = Offset(centerPt.x - offsetAmount, centerPt.y - offsetAmount),
                                    end = Offset(centerPt.x + offsetAmount, centerPt.y + offsetAmount),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(2.5.dp, currentPrimaryColor, CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            currentPrimaryColor.copy(alpha = if (isHovering) 0.7f else 0.4f),
                                            currentSecondaryColor.copy(alpha = if (isHovering) 0.4f else 0.2f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(if (isHovering) 9.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isHovering && !isLightMode) Color(0xFF00FF64) else (if (isLightMode) currentPrimaryColor else Color.White))
                            )
                        }
                    }
                }
            }
        }
    }
}
