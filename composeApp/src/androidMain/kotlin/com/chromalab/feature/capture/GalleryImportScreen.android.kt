package com.chromalab.feature.capture

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.feature.processing.document.MlKitDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

private const val CAPTURE_TAG = "ChromaLabCapture"

/**
 * Android photo import.
 *
 * Preferred path matches CameraScreen: ML Kit Smart Scan with gallery import,
 * crop/deskew/filter editor, then a prepared JPEG for the analysis pipeline.
 * Raw gallery import is kept only as a fallback when Smart Scan is unavailable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun GalleryImportScreen(
    onImageSelected: (imagePath: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var savedPath by remember { mutableStateOf<String?>(null) }
    var smartScanLaunched by remember { mutableStateOf(false) }
    var smartScanInProgress by remember { mutableStateOf(true) }
    var useRawFallback by remember { mutableStateOf(false) }
    var scannerError by remember { mutableStateOf<String?>(null) }

    val rawGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            savedPath = copyToInternal(context, uri)
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        smartScanInProgress = false
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val outputDir = File(context.filesDir, "captures").absolutePath
            val path = scanResult?.let {
                MlKitDocumentScanner.copyResultToStorage(context, it, outputDir)
            }
            if (path != null) {
                Log.i(CAPTURE_TAG, "Photo import prepared by Smart Scan: $path")
                onImageSelected(path)
            } else {
                scannerError = "Smart Scan did not return a prepared image."
                Log.w(CAPTURE_TAG, scannerError!!)
                useRawFallback = true
            }
        } else {
            Log.i(CAPTURE_TAG, "Smart Scan cancelled for photo import; showing raw fallback.")
            useRawFallback = true
        }
    }

    LaunchedEffect(Unit) {
        if (!smartScanLaunched) {
            smartScanLaunched = true
            smartScanInProgress = true
            val activity = context as? Activity
            if (activity == null) {
                scannerError = "Activity context is unavailable for Smart Scan."
                smartScanInProgress = false
                useRawFallback = true
                return@LaunchedEffect
            }

            MlKitDocumentScanner.getScanner()
                .getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build(),
                    )
                }
                .addOnFailureListener { error ->
                    scannerError = "Smart Scan unavailable: ${error.message}"
                    Log.w(CAPTURE_TAG, "Smart Scan unavailable for photo import.", error)
                    smartScanInProgress = false
                    useRawFallback = true
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            selectedUri != null -> "Предпросмотр фото"
                            useRawFallback -> "Импорт фото"
                            else -> "Smart Scan"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (selectedUri != null) {
                        IconButton(onClick = {
                            savedPath?.let { onImageSelected(it) }
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = "Подтвердить")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                selectedUri != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = "Imported image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                smartScanInProgress && !useRawFallback -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Запуск Smart Scan",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 24.dp),
                    ) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Выберите изображение из галереи",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        scannerError?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        OutlinedButton(onClick = {
                            rawGalleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }) {
                            Text("Открыть галерею")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Copy URI content to internal storage, preserving the original unmodified.
 */
private fun copyToInternal(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "imports").also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())
        val file = File(dir, "imported_$timestamp.jpg")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (_: Exception) {
        null
    }
}
