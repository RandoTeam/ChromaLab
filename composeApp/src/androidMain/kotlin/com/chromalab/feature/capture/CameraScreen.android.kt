package com.chromalab.feature.capture

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chromalab.feature.processing.document.MlKitDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

/**
 * Capture entry screen — Smart Scan only.
 *
 * Launches Google ML Kit document scanner immediately on entry.
 * ML Kit provides built-in camera, edge detection, crop, deskew,
 * shadow removal and image filters — all via Google Play Services (0 MB in APK).
 *
 * If ML Kit is unavailable, falls back to ManualCameraScreen.
 */
@Composable
actual fun CameraScreen(
    onImageCaptured: (imagePath: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var scannerLaunched by remember { mutableStateOf(false) }
    var scannerError by remember { mutableStateOf<String?>(null) }
    var showFallbackCamera by remember { mutableStateOf(false) }

    // ML Kit Scanner launcher
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (scanResult != null) {
                val outputDir = File(context.filesDir, "captures").absolutePath
                val path = MlKitDocumentScanner.copyResultToStorage(
                    context, scanResult, outputDir,
                )
                if (path != null) {
                    onImageCaptured(path)
                } else {
                    scannerError = "Не удалось сохранить результат сканирования"
                }
            }
        } else {
            // User cancelled scanning — go back
            onBack()
        }
    }

    // Auto-launch Smart Scan on first composition
    LaunchedEffect(Unit) {
        if (!scannerLaunched) {
            scannerLaunched = true
            val activity = context as? Activity ?: return@LaunchedEffect
            val scanner = MlKitDocumentScanner.getScanner()
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
                .addOnFailureListener { e ->
                    scannerError = "Smart Scan недоступен: ${e.message}"
                    showFallbackCamera = true
                }
        }
    }

    if (showFallbackCamera) {
        // Fallback: manual camera if ML Kit is unavailable
        ManualCameraScreen(
            onImageCaptured = onImageCaptured,
            onBack = onBack,
        )
    } else if (scannerError != null && !showFallbackCamera) {
        // Error state — should rarely happen
        ScannerErrorScreen(
            error = scannerError!!,
            onRetry = {
                scannerError = null
                scannerLaunched = false
            },
            onBack = onBack,
        )
    }
    // Otherwise: empty screen while scanner is launching (brief flash)
}

/**
 * Error screen shown when Smart Scan fails to initialize.
 */
@Composable
private fun ScannerErrorScreen(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22),
                        Color(0xFF0D1117),
                    )
                )
            ),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFF85149),
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Ошибка сканера",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Убедитесь, что Google Play Services обновлены.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238636),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Повторить", color = Color.White)
            }
        }
    }
}
