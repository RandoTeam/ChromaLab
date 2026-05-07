package com.chromalab.feature.processing.export

import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.SmoothedSignal
import com.chromalab.feature.processing.storage.ProcessingParams
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Full export bundle — everything needed to reproduce and re-open.
 */
@Serializable
data class ExportBundle(
    val version: Int = 1,
    val signal: DigitalSignal,
    val calibration: PixelCalibration,
    val processingParams: ProcessingParams,
    val timestamp: Long,
)

/**
 * Builds export files: CSV and JSON.
 * CSV matches what is shown on screen — same order, same values.
 */
object PointExporter {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Export as CSV: index, pixel_x, pixel_y, time, intensity, confidence
     * Matches screen display exactly.
     */
    fun exportCsv(signal: DigitalSignal): String {
        val sb = StringBuilder()
        sb.appendLine("index,pixel_x,pixel_y,time,intensity,confidence")
        for (gp in signal.points) {
            sb.appendLine(
                "${gp.index},${gp.pixelX},${gp.pixelY}," +
                    "${"%.4f".format(gp.time)},${"%.2f".format(gp.intensity)}," +
                    "${"%.2f".format(gp.confidence)}",
            )
        }
        return sb.toString()
    }

    /**
     * Export as JSON with full metadata, calibration, processing params, and points.
     * Can be re-opened to restore the entire session.
     */
    fun exportJson(bundle: ExportBundle): String = json.encodeToString(bundle)

    /**
     * Import a previously exported JSON bundle.
     */
    fun importJson(content: String): ExportBundle = json.decodeFromString(content)
}
