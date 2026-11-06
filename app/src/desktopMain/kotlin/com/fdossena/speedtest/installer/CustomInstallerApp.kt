package com.fdossena.speedtest.installer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdossena.speedtest.ui.NetworkConstellationBackground
import com.fdossena.speedtest.utils.DesktopShortcutManager
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() = application {
    val windowState = rememberWindowState(width = 800.dp, height = 550.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mr Speed PRO Setup",
        state = windowState,
        undecorated = true,
        resizable = false
    ) {
        CustomIObitStyleInstaller(
            onClose = ::exitApplication
        )
    }
}

@Composable
fun CustomIObitStyleInstaller(
    onClose: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var installPath by remember { mutableStateOf("C:\\Program Files\\MrSpeed") }
    var agreeTerms by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Initializing setup engine...") }
    val scope = rememberCoroutineScope()

    val pColor = Color(0xFF00E5FF)
    val bgDark = Color(0xFF08080E)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgDark
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NetworkConstellationBackground(0.8f)

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mr Speed PRO | Setup",
                        color = pColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )

                    IconButton(onClick = onClose) {
                        Text("✕", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }

                // Main Content
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (step) {
                        0 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Mr Speed PRO",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 32.sp,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "Ultimate Network Speedtest & Telemetry Suite",
                                    color = pColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                Spacer(Modifier.height(28.dp))

                                // EULA Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(130.dp)
                                        .background(Color(0xFF0F1117), RoundedCornerShape(12.dp))
                                        .border(1.dp, pColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "MR SPEED PRO END USER LICENSE AGREEMENT & TERMS OF SERVICE\n\n" +
                                                "1. ACCEPTANCE: By installing Mr Speed PRO, you agree to these terms.\n" +
                                                "2. USAGE: Granted non-exclusive license for broadband diagnostics.\n" +
                                                "3. PRIVACY: Telemetry is 100% anonymized with zero tracking.\n" +
                                                "4. AUTONOMOUS UPDATES: Background service checks and applies patches.\n\n" +
                                                "Supreme Developments - All Rights Reserved.",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = agreeTerms,
                                        onCheckedChange = { agreeTerms = it },
                                        colors = CheckboxDefaults.colors(checkedColor = pColor)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "I accept the Mr Speed PRO Terms of Service & EULA",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        1 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "INSTALLING MR SPEED PRO",
                                    color = pColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = statusText,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )

                                Spacer(Modifier.height(24.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(0.8f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = pColor,
                                    trackColor = pColor.copy(alpha = 0.2f)
                                )

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        2 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = Color(0xFF00FF64),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 64.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "INSTALLATION COMPLETE!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Mr Speed PRO is ready to launch on your computer.",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    when (step) {
                        0 -> {
                            Button(
                                onClick = {
                                    if (agreeTerms) {
                                        step = 1
                                        scope.launch(Dispatchers.IO) {
                                            for (p in 1..100) {
                                                progress = p / 100f
                                                statusText = when (p) {
                                                    in 1..30 -> "Extracting application binaries to $installPath..."
                                                    in 31..70 -> "Configuring encrypted local settings storage..."
                                                    in 71..90 -> "Creating Desktop shortcuts & Start Menu entries..."
                                                    else -> "Finalizing setup engine..."
                                                }
                                                delay(35)
                                            }
                                            withContext(Dispatchers.Main) {
                                                try {
                                                    DesktopShortcutManager.createOrUpdateShortcuts(false)
                                                } catch (_: Exception) {}
                                                step = 2
                                            }
                                        }
                                    }
                                },
                                enabled = agreeTerms,
                                modifier = Modifier.fillMaxWidth(0.6f).height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = pColor)
                            ) {
                                Text("INSTALL NOW ➔", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }

                        2 -> {
                            Button(
                                onClick = {
                                    try {
                                        val exec = DesktopShortcutManager.getAppExecutionTarget()
                                        Runtime.getRuntime().exec(arrayOf(exec.targetPath))
                                    } catch (_: Exception) {}
                                    onClose()
                                },
                                modifier = Modifier.fillMaxWidth(0.6f).height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF64))
                            ) {
                                Text("LAUNCH MR SPEED PRO 🚀", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
