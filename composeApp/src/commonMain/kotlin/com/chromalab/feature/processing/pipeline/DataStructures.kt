package com.chromalab.feature.processing.pipeline

import com.chromalab.feature.capture.CaptureMetadata
import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.calibration.XAxisCalibration
import com.chromalab.feature.processing.calibration.YAxisCalibration
import com.chromalab.feature.processing.curve.CurveExtractionResult
import com.chromalab.feature.processing.curve.CurvePoint
import com.chromalab.feature.processing.curve.ManualEditLog
import com.chromalab.feature.processing.document.DocumentBounds
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.perspective.PerspectiveCorrectionResult
import com.chromalab.feature.processing.quality.DigitizationQualityReport
import com.chromalab.feature.processing.quality.ImageQualityReport
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.GraphPoint
import com.chromalab.feature.processing.storage.ProcessingParams
import kotlinx.serialization.Serializable

/**
 * Phase 1 — canonical data structures registry.
 *
 * This file maps the spec-defined names (PHASE_1.md §1.33) to their
 * actual implementations across the codebase.
 *
 * Spec name                → Implementation
 * ─────────────────────────────────────────────────────
 * ImageCaptureResult       → ImageCaptureResult (this file)
 * ImageQualityReport       → quality.ImageQualityReport
 * DocumentBounds           → document.DocumentBounds
 * PerspectiveCorrectionData→ PerspectiveCorrectionData (this file, wraps PerspectiveCorrectionResult)
 * GraphRegion              → graph.GraphRegion
 * AxisCalibration          → AxisCalibration (this file, combines X+Y)
 * PixelCalibration         → calibration.PixelCalibration
 * CurveExtractionParams    → CurveExtractionParams (this file)
 * ExtractedPixelPoint      → curve.CurvePoint (alias)
 * GraphPoint               → signal.GraphPoint
 * ExtractedSignal          → signal.DigitalSignal (alias)
 * DigitizationQualityReport→ quality.DigitizationQualityReport
 * ManualEditLog            → curve.ManualEditLog
 * ProcessingParams         → storage.ProcessingParams
 */

// --- Missing structures ---

/**
 * Result of image capture — path + metadata.
 */
@Serializable
data class ImageCaptureResult(
    val imagePath: String,
    val metadata: CaptureMetadata,
    val source: CaptureSource,
)

enum class CaptureSource {
    CAMERA,
    GALLERY,
    FILE_IMPORT,
}

/**
 * Combined perspective correction data.
 */
@Serializable
data class PerspectiveCorrectionData(
    val originalCorners: List<FloatArray>,
    val correctedCorners: List<FloatArray>,
    val wasApplied: Boolean,
    val correctedImagePath: String?,
)

/**
 * Combined axis calibration (X + Y together).
 */
@Serializable
data class AxisCalibration(
    val xCalibration: XAxisCalibration,
    val yCalibration: YAxisCalibration,
    val pixelCalibration: PixelCalibration,
)

/**
 * Parameters used for curve extraction.
 */
@Serializable
data class CurveExtractionParams(
    val binarizationThreshold: Int = 128,
    val morphologyKernelSize: Int = 3,
    val minContourArea: Float = 10f,
    val outlierSigma: Float = 2.5f,
    val interpolationEnabled: Boolean = true,
    val columnScanDirection: ScanDirection = ScanDirection.TOP_TO_BOTTOM,
)

enum class ScanDirection {
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
}

// --- Type aliases for spec compatibility ---

/** Spec: ExtractedPixelPoint → CurvePoint */
typealias ExtractedPixelPoint = CurvePoint

/** Spec: ExtractedSignal → DigitalSignal */
typealias ExtractedSignal = DigitalSignal
