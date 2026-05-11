package com.chromalab.feature.capture

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import android.view.Surface
import android.view.OrientationEventListener

/**
 * Premium manual CameraX screen with:
 * - Pinch-to-zoom (replaces slider)
 * - Exposure compensation slider
 * - Flash toggle (Auto/On/Off)
 * - Tap-to-focus with animated ring
 * - Document frame overlay
 */
@Composable
fun ManualCameraScreen(
    onImageCaptured: (imagePath: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }
    // Request permission on first composition if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Show permission rationale if denied
    if (!hasCameraPermission) {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White.copy(alpha = 0.6f),
                )
                Text(
                    "Требуется разрешение на камеру",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    "Для съёмки хроматограмм необходим доступ к камере",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                )
                Button(onClick = {
                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                }) {
                    Text("Разрешить")
                }
                TextButton(onClick = onBack) {
                    Text("Назад", color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
        return
    }


    var flashMode by remember { mutableStateOf(FlashMode.AUTO) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var exposureIndex by remember { mutableIntStateOf(0) }
    var isCapturing by remember { mutableStateOf(false) }
    var showExposureSlider by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }

    // Focus indicator state
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusRing by remember { mutableStateOf(false) }

    val imageCapture = remember {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .build()
    }

    // Track display rotation for correct EXIF orientation
    DisposableEffect(context) {
        val orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when {
                    orientation >= 315 || orientation < 45 -> Surface.ROTATION_0
                    orientation in 45..134 -> Surface.ROTATION_270
                    orientation in 135..224 -> Surface.ROTATION_180
                    else -> Surface.ROTATION_90
                }
                imageCapture.targetRotation = rotation
            }
        }
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }
        onDispose { orientationListener.disable() }
    }

    val preview = remember { Preview.Builder().build() }
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    var camera by remember { mutableStateOf<Camera?>(null) }
    var exposureRange by remember { mutableStateOf(-12..12) }

    // Flash mode
    LaunchedEffect(flashMode) {
        imageCapture.flashMode = when (flashMode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    // Zoom
    LaunchedEffect(zoomRatio) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    // Exposure
    LaunchedEffect(exposureIndex) {
        camera?.cameraControl?.setExposureCompensationIndex(exposureIndex)
    }

    // Focus ring auto-dismiss
    LaunchedEffect(showFocusRing) {
        if (showFocusRing) {
            delay(1500)
            showFocusRing = false
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Camera preview with pinch-to-zoom + tap-to-focus
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE

                    // Pinch-to-zoom
                    val scaleDetector = ScaleGestureDetector(ctx,
                        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                val newZoom = (zoomRatio * detector.scaleFactor)
                                    .coerceIn(1f, 8f)
                                zoomRatio = newZoom
                                return true
                            }
                        }
                    )

                    previewView.setOnTouchListener { view, event ->
                        scaleDetector.onTouchEvent(event)
                        if (event.action == MotionEvent.ACTION_DOWN && event.pointerCount == 1) {
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point)
                                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            camera?.cameraControl?.startFocusAndMetering(action)
                            focusPoint = Offset(event.x, event.y)
                            showFocusRing = true
                        }
                        view.performClick()
                        true
                    }

                    // Bind camera once
                    scope.launch {
                        try {
                            val provider = ctx.getCameraProvider()
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, preview, imageCapture,
                            )
                            preview.surfaceProvider = previewView.surfaceProvider

                            // Read exposure compensation range
                            camera?.cameraInfo?.exposureState?.let { state ->
                                exposureRange = state.exposureCompensationRange.let {
                                    it.lower..it.upper
                                }
                            }
                            cameraReady = true
                            println("CAMERA[MANUAL] Camera bound successfully")
                        } catch (e: Exception) {
                            println("CAMERA[MANUAL] Camera bind failed: ${e.message}")
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Document frame overlay
        CameraFrameOverlay()

        // Focus ring animation
        if (showFocusRing && focusPoint != null) {
            FocusRingIndicator(focusPoint!!)
        }

        // Top bar: gradient fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                ),
        )

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }

            Row {
                // Flash toggle
                IconButton(onClick = {
                    flashMode = when (flashMode) {
                        FlashMode.AUTO -> FlashMode.ON
                        FlashMode.ON -> FlashMode.OFF
                        FlashMode.OFF -> FlashMode.AUTO
                    }
                }) {
                    Icon(
                        when (flashMode) {
                            FlashMode.AUTO -> Icons.Filled.FlashAuto
                            FlashMode.ON -> Icons.Filled.FlashOn
                            FlashMode.OFF -> Icons.Filled.FlashOff
                        },
                        contentDescription = "Flash",
                        tint = when (flashMode) {
                            FlashMode.ON -> Color(0xFFFFC107)
                            else -> Color.White
                        },
                    )
                }

                // Exposure toggle
                IconButton(onClick = { showExposureSlider = !showExposureSlider }) {
                    Icon(
                        Icons.Filled.BrightnessHigh,
                        contentDescription = "Exposure",
                        tint = if (showExposureSlider) Color(0xFFFFC107) else Color.White,
                    )
                }
            }
        }

        // Exposure slider (right side, vertical)
        AnimatedVisibility(
            visible = showExposureSlider,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 16.dp),
            ) {
                Icon(
                    Icons.Filled.BrightnessHigh,
                    null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Vertical slider using rotated Slider — using Column + clickable segments
                val steps = exposureRange.last - exposureRange.first
                Slider(
                    value = exposureIndex.toFloat(),
                    onValueChange = { exposureIndex = it.toInt() },
                    valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                    steps = steps - 1,
                    modifier = Modifier
                        .height(200.dp)
                        .width(32.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFFFC107),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (exposureIndex >= 0) "+$exposureIndex" else "$exposureIndex",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }

        // Bottom bar gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                ),
        )

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Zoom indicator
            val zoomText = "%.1fx".format(zoomRatio)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.5f),
            ) {
                Text(
                    zoomText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Capture button
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Outer ring
                Canvas(modifier = Modifier.size(76.dp)) {
                    drawCircle(
                        Color.White.copy(alpha = 0.3f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
                // Inner button
                IconButton(
                    onClick = {
                        if (!isCapturing && cameraReady) {
                            isCapturing = true
                            scope.launch {
                                val path = captureImage(context, imageCapture)
                                isCapturing = false
                                if (path != null) {
                                    println("CAMERA[MANUAL] Captured: $path")
                                    onImageCaptured(path)
                                } else {
                                    println("CAMERA[MANUAL] Capture failed")
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                if (isCapturing) Color.Gray else Color.White,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

/**
 * Animated focus ring at tap point.
 */
@Composable
private fun FocusRingIndicator(point: Offset) {
    val animScale = remember { Animatable(1.5f) }
    val animAlpha = remember { Animatable(1f) }

    LaunchedEffect(point) {
        animScale.snapTo(1.5f)
        animAlpha.snapTo(1f)
        launch { animScale.animateTo(1f, tween(300, easing = EaseOutCubic)) }
        launch {
            delay(800)
            animAlpha.animateTo(0f, tween(500))
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = 28.dp.toPx() * animScale.value
        drawCircle(
            Color(0xFF58A6FF).copy(alpha = animAlpha.value * 0.8f),
            radius = radius,
            center = point,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
        // Center dot
        drawCircle(
            Color(0xFF58A6FF).copy(alpha = animAlpha.value),
            radius = 3.dp.toPx(),
            center = point,
        )
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
