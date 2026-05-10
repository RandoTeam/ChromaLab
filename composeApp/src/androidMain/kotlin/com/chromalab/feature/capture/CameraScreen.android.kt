package com.chromalab.feature.capture

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.feature.processing.document.MlKitDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

/**
 * Capture entry screen.
 *
 * Two capture modes:
 * 1. **Smart Scan** (ML Kit) — launches Google's document scanner with
 *    built-in camera, edge detection, crop, deskew, and shadow removal.
 *    This is the recommended path for best results.
 * 2. **Manual Camera** — our CameraX camera for when ML Kit is unavailable
 *    or the user wants raw photo control.
 *
 * ML Kit Scanner runs via Google Play Services (0 MB in APK).
 */
@Composable
actual fun CameraScreen(
    onImageCaptured: (imagePath: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var showManualCamera by remember { mutableStateOf(false) }
    var scannerError by remember { mutableStateOf<String?>(null) }

    // ML Kit Scanner launcher
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (scanResult != null) {
                // Copy to persistent storage
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
        }
        // If cancelled, just stay on this screen
    }

    // Launch ML Kit Scanner
    fun launchScanner() {
        val activity = context as? Activity ?: return
        val scanner = MlKitDocumentScanner.getScanner()
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            .addOnFailureListener { e ->
                scannerError = "Сканер недоступен: ${e.message}"
                // Fall back to manual camera
                showManualCamera = true
            }
    }

    if (showManualCamera) {
        // Manual CameraX camera
        ManualCameraScreen(
            onImageCaptured = onImageCaptured,
            onBack = { showManualCamera = false },
        )
    } else {
        // Main capture selector screen
        CaptureMethodSelector(
            scannerError = scannerError,
            onSmartScan = { launchScanner() },
            onManualCamera = { showManualCamera = true },
            onBack = onBack,
        )
    }
}

@Composable
private fun CaptureMethodSelector(
    scannerError: String?,
    onSmartScan: () -> Unit,
    onManualCamera: () -> Unit,
    onBack: () -> Unit,
) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

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
        // Back button
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
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title
            Text(
                "Оцифровка",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Выберите способ захвата хроматограммы",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Smart Scan card (recommended)
            Card(
                onClick = onSmartScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFF58A6FF).copy(alpha = pulseAlpha),
                                Color(0xFF1F6FEB).copy(alpha = pulseAlpha),
                            ),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C2333),
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.DocumentScanner,
                            contentDescription = null,
                            tint = Color(0xFF58A6FF),
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Smart Scan",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Рекомендуется",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF58A6FF),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Автоматическое обнаружение документа, обрезка, выравнивание и удаление теней. Работает через Google ML Kit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        FeatureChip("Авто-обрезка")
                        FeatureChip("Выравнивание")
                        FeatureChip("Убирает тени")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual camera card
            Card(
                onClick = onManualCamera,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C2333).copy(alpha = 0.6f),
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Ручная съёмка",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                            Text(
                                "CameraX — полный контроль",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Вспышка, зум, экспозиция. Для сложных условий съёмки.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            // Error message
            if (scannerError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    scannerError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FeatureChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF58A6FF).copy(alpha = 0.12f),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF58A6FF),
        )
    }
}
