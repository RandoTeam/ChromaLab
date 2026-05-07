package com.chromalab.feature.processing.storage

import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.curve.CurveExtractionResult
import com.chromalab.feature.processing.curve.ManualEditLog
import com.chromalab.feature.processing.quality.DigitizationQualityReport
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.SmoothedSignal
import com.chromalab.feature.processing.signal.SmoothingParams
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Processing parameters snapshot — everything needed to reproduce the result.
 */
@Serializable
data class ProcessingParams(
    val calibration: PixelCalibration,
    val smoothingParams: SmoothingParams,
    val maskBlockSize: Int = 31,
    val maskThresholdC: Int = 10,
    val gridSuppression: Boolean = true,
    val textBlobSuppression: Boolean = true,
    val outlierWindow: Int = 15,
    val outlierThreshold: Float = 3.0f,
    val timestamp: Long,
)

/**
 * Saves all intermediate pipeline files to a session directory.
 * Pure Kotlin — uses kotlinx.serialization for JSON output.
 */
object IntermediateFileSaver {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * File manifest — declares all intermediate files.
     * Each file is optional — null path means it was not produced.
     */
    @Serializable
    data class FileManifest(
        val original: String? = null,
        val normalized: String? = null,
        val cropped: String? = null,
        val documentCorrected: String? = null,
        val grayscale: String? = null,
        val clahe: String? = null,
        val binary: String? = null,
        val morphology: String? = null,
        val graphRoi: String? = null,
        val curveMaskRaw: String? = null,
        val curveMaskClean: String? = null,
        val curveOverlay: String? = null,
        val extractedPointsCsv: String? = null,
        val extractedPointsJson: String? = null,
        val processingParams: String? = null,
        val qualityReport: String? = null,
        val manualEdits: String? = null,
    )

    /**
     * Save extracted points as CSV.
     */
    fun savePointsCsv(signal: DigitalSignal): String {
        val sb = StringBuilder()
        sb.appendLine("index,pixel_x,pixel_y,time,intensity,confidence,interpolated")
        for (gp in signal.points) {
            sb.appendLine(
                "${gp.index},${gp.pixelX},${gp.pixelY},${gp.time},${gp.intensity},${gp.confidence},${gp.isInterpolated}",
            )
        }
        return sb.toString()
    }

    /**
     * Save extracted points as JSON.
     */
    fun savePointsJson(signal: DigitalSignal): String = json.encodeToString(signal)

    /**
     * Save processing parameters as JSON.
     */
    fun saveProcessingParams(params: ProcessingParams): String = json.encodeToString(params)

    /**
     * Save quality report as JSON.
     */
    fun saveQualityReport(report: DigitizationQualityReport): String = json.encodeToString(report)

    /**
     * Save manual edits as JSON.
     */
    fun saveManualEdits(edits: ManualEditLog): String = json.encodeToString(edits)

    /**
     * Save file manifest as JSON.
     */
    fun saveManifest(manifest: FileManifest): String = json.encodeToString(manifest)
}
