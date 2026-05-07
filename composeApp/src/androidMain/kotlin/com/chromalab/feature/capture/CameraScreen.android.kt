package com.chromalab.feature.capture

import android.content.Context
import android.view.MotionEvent
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Android CameraX implementation.
 * Features: preview, capture, auto/tap focus, flash, zoom, frame overlay.
 */
@Composable
actual fun CameraScreen(
    onImageCaptured: (imagePath: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var flashMode by remember { mutableStateOf(FlashMode.AUTO) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var isCapturing by remember { mutableStateOf(false) }

    val imageCapture = remember {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .build()
    }

    val preview = remember { Preview.Builder().build() }
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    var camera by remember { mutableStateOf<Camera?>(null) }

    // Update flash mode on imageCapture
    LaunchedEffect(flashMode) {
        imageCapture.flashMode = when (flashMode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    // Update zoom
    LaunchedEffect(zoomRatio) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE

                    // Tap-to-focus
                    previewView.setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point)
                                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            camera?.cameraControl?.startFocusAndMetering(action)
                        }
                        view.performClick()
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                scope.launch {
                    val provider = context.getCameraProvider()
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                    )
                    preview.surfaceProvider = previewView.surfaceProvider

                    // Disable auto-enhance extensions if possible
                    camera?.cameraControl?.enableTorch(false)
                }
            },
        )

        // Frame overlay
        CameraFrameOverlay()

        // Top bar: back + flash
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                )
            }

            IconButton(onClick = {
                flashMode = when (flashMode) {
                    FlashMode.AUTO -> FlashMode.ON
                    FlashMode.ON -> FlashMode.OFF
                    FlashMode.OFF -> FlashMode.AUTO
                }
            }) {
                Icon(
                    imageVector = when (flashMode) {
                        FlashMode.AUTO -> Icons.Filled.FlashAuto
                        FlashMode.ON -> Icons.Filled.FlashOn
                        FlashMode.OFF -> Icons.Filled.FlashOff
                    },
                    contentDescription = "Flash",
                    tint = Color.White,
                )
            }
        }

        // Bottom controls: zoom + capture
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Zoom slider
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("1x", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = zoomRatio,
                    onValueChange = { zoomRatio = it },
                    valueRange = 1f..5f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
                )
                Text("5x", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // Capture button
            IconButton(
                onClick = {
                    if (!isCapturing) {
                        isCapturing = true
                        scope.launch {
                            val path = captureImage(context, imageCapture)
                            isCapturing = false
                            if (path != null) {
                                onImageCaptured(path)
                            }
                        }
                    }
                },
                modifier = Modifier.size(72.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = if (isCapturing) Color.Gray else Color.White,
                            shape = CircleShape,
                        )
                        .padding(4.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.3f),
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener(
                { continuation.resume(future.get()) },
                { it.run() },
            )
        }
    }

private suspend fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
): String? = suspendCoroutine { continuation ->
    val dir = File(context.filesDir, "captures").also { it.mkdirs() }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val file = File(dir, "original_$timestamp.jpg")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
        outputOptions,
        { it.run() },
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                continuation.resume(file.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                continuation.resume(null)
            }
        },
    )
}
