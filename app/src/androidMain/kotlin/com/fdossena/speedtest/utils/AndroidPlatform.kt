@file:Suppress("OPT_IN_USAGE")
package com.fdossena.speedtest.utils

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

actual fun openUrl(url: String) {
    // Requires context - usually handled in UI
}

actual fun shareResult(text: String) {
}

@Composable
actual fun PlatformBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}

@Composable
actual fun rememberHasCamera(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
}

@Composable
actual fun QRScanner(onScanned: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var scanned by remember { mutableStateOf(false) }
    
    val scanner = remember { 
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val provider = cameraProviderFuture.get()
                provider.unbindAll()
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }
    
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                preview.setSurfaceProvider(previewView.surfaceProvider)
                
                val analysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !scanned) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val raw = barcode.rawValue
                                    if (!raw.isNullOrEmpty() && !scanned) {
                                        scanned = true
                                        Log.d("QRScanner", "QR Content Scanned: $raw")
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            onScanned(raw)
                                        }
                                        break
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("QRScanner", "Scan error", e)
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                    } catch (e: Exception) {
                        Log.e("CameraOverlay", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp, horizontal = 24.dp)
        ) {
            Text(
                "ALIGN QR CODE WITHIN FRAME",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium
            )

            Box(
                modifier = Modifier
                    .size(250.dp)
                    .border(3.dp, Color(0xFF00E5FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF).copy(alpha = 0.3f),
                    modifier = Modifier.size(100.dp)
                )
            }

            AppButton(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CANCEL", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
actual fun PlatformWebView(url: String, modifier: androidx.compose.ui.Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
        modifier = modifier
    )
}
