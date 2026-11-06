package com.fdossena.speedtest.ui.heatmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import com.fdossena.speedtest.ui.models.SpeedtestUIState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fdossena.speedtest.core.floorplan.FloorPlanManager
import com.fdossena.speedtest.ui.ModernTopBar
import com.fdossena.speedtest.utils.*
import java.util.UUID

@Composable
fun CornerMappingScreen(
    state: SpeedtestUIState,
    manager: FloorPlanManager,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val hasCameraPermission = true

    val activePoints by manager.activePoints.collectAsState()

    Scaffold(
        topBar = {
            ModernTopBar(
                title = "Floor Plan Mapper",
                onBack = onBack
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (hasCameraPermission) {
                // In a real app, this would be the ARSurfaceView
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AR CAMERA VIEW (Simulated)", color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Camera permission required for AR mapping", color = Color.White)
                }
            }

            // HUD
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (activePoints.isEmpty()) "Go to the first corner and tap Mark" 
                           else "Marked ${activePoints.size} corners. Move to next corner.",
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo Button
                    Surface(
                        onClick = { manager.undoLastPoint() },
                        enabled = activePoints.isNotEmpty(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Undo",
                                tint = if (activePoints.isNotEmpty()) Color.White else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Mark Point Button
                    AppButton(
                        onClick = {
                            // Simulate getting coordinates from ARCore
                            manager.addPoint(0f, 0f, 0f)
                        },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Mark Corner",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Place Portal Button (NEW)
                    AppButton(
                        onClick = {
                            manager.addPortal(0f, 0f, 0f, state.dlSpeed)
                        },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Place Portal",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Finish Room Button
                    Surface(
                        onClick = {
                            manager.finishRoom("Room ${UUID.randomUUID().toString().take(4)}")
                            onFinished()
                        },
                        enabled = activePoints.size >= 3,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Finish Room",
                                tint = if (activePoints.size >= 3) Color.Green else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
