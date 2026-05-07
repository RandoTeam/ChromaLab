package com.chromalab.feature.capture

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Android gallery import: pick image → adjust (pan/zoom/rotate) → confirm.
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

    // Transform state
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            // Copy to internal storage immediately (preserve original)
            savedPath = copyToInternal(context, uri)
        }
    }

    // Auto-launch picker on first composition
    LaunchedEffect(Unit) {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedUri != null) "Подгонка изображения" else "Импорт из галереи",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (selectedUri != null) {
                        // Rotate 90°
                        IconButton(onClick = { rotation += 90f }) {
                            Icon(Icons.Filled.RotateRight, contentDescription = "Повернуть")
                        }
                        // Confirm
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
            if (selectedUri != null) {
                // Adjustable image with frame overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, gestureRotation ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                rotation += gestureRotation
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y,
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Imported image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                rotationZ = rotation
                                translationX = offset.x
                                translationY = offset.y
                            },
                    )

                    // Frame overlay
                    CameraFrameOverlay()
                }
            } else {
                // Waiting for picker or picker was cancelled
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    OutlinedButton(onClick = {
                        launcher.launch(
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
