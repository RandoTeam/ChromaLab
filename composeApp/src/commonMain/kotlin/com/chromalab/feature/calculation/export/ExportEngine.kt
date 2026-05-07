package com.chromalab.feature.calculation.export

import com.chromalab.feature.calculation.algorithm.*
import com.chromalab.feature.calculation.core.ManualEditLog

/**
 * Export engine for calculation results (§2.30).
 *
 * Formats:
 * - peaks.csv: all peak metrics in tabular form
 * - calculation.json: full run metadata + params + results
 * - corrected_signal.csv: time,intensity pairs
 * - baseline.csv: time,baseline pairs
 * - warnings.json: structured warning list
 *
 * All export functions are pure — return String, no I/O.
 * Writing to file / sharing is handled by the platform layer.
 */

// ─── Data container for export ──────────────────────────────────

data class ExportData(
    val runId: String,
    val sourceSignalId: String,
    val pipelineVersion: String,
    val algorithmVersion: String,
    val presetName: String,
    val paramsJson: String,
    val peaks: List<ExportPeak>,
    val correctedSignal: List<ExportPoint>,
    val baseline: List<ExportPoint>,
    val noiseRegions: List<ExportNoiseRegion>,
    val manualEdits: ManualEditLog,
    val warnings: List<ExportWarning>,
    val timestamp: Long,
)

data class ExportPeak(
    val peakId: Int,
    val status: String,
    val rtApex: Double,
    val rtCentroid: Double,
    val height: Double,
    val area: Double,
    val widthBase: Double,
    val widthHalfHeight: Double,
    val prominence: Double,
    val snr: Double,
    val snrMethod: String,
    val baselineMethod: String,
    val integrationMethod: String,
    val confidenceGrade: String,
    val confidenceScore: Double,
    val overlapStatus: String,
    val boundaryMethod: String,
    val leftBoundary: Double,
    val rightBoundary: Double,
    val positiveArea: Double,
    val negativeArea: Double,
    val isManuallyEdited: Boolean,
    val warnings: List<String>,
)

data class ExportPoint(val time: Double, val intensity: Double)

data class ExportNoiseRegion(
    val startTime: Double,
    val endTime: Double,
    val method: String,
    val noiseValue: Double,
)

data class ExportWarning(
    val code: String,
    val severity: String,
    val message: String,
    val peakIndex: Int?,
    val stage: String,
)

// ─── CSV: peaks ─────────────────────────────────────────────────

object PeaksCsvExporter {

    private val HEADER = listOf(
        "PeakId", "Status", "RT_Apex", "RT_Centroid",
        "Height", "Area", "Width_Base", "Width_HalfHeight",
        "Prominence", "SNR", "SNR_Method",
        "Baseline_Method", "Integration_Method",
        "Confidence", "Confidence_Score",
        "Overlap", "Boundary_Method",
        "Left_Boundary", "Right_Boundary",
        "Positive_Area", "Negative_Area",
        "Manual", "Warnings",
    ).joinToString(",")

    fun export(peaks: List<ExportPeak>): String {
        val lines = peaks.map { p ->
            listOf(
                p.peakId,
                p.status,
                "%.4f".format(p.rtApex),
                "%.4f".format(p.rtCentroid),
                "%.2f".format(p.height),
                "%.2f".format(p.area),
                "%.4f".format(p.widthBase),
                "%.4f".format(p.widthHalfHeight),
                "%.2f".format(p.prominence),
                "%.2f".format(p.snr),
                p.snrMethod,
                p.baselineMethod,
                p.integrationMethod,
                p.confidenceGrade,
                "%.3f".format(p.confidenceScore),
                p.overlapStatus,
                p.boundaryMethod,
                "%.4f".format(p.leftBoundary),
                "%.4f".format(p.rightBoundary),
                "%.2f".format(p.positiveArea),
                "%.2f".format(p.negativeArea),
                if (p.isManuallyEdited) "yes" else "no",
                "\"${p.warnings.joinToString("; ")}\"",
            ).joinToString(",")
        }
        return (listOf(HEADER) + lines).joinToString("\n")
    }
}

// ─── CSV: corrected signal ──────────────────────────────────────

object SignalCsvExporter {

    fun export(points: List<ExportPoint>, label: String = "Intensity"): String {
        val header = "Time,$label"
        val lines = points.map { "%.6f,%.4f".format(it.time, it.intensity) }
        return (listOf(header) + lines).joinToString("\n")
    }
}

// ─── JSON: full calculation ─────────────────────────────────────

object CalculationJsonExporter {

    /**
     * Manual JSON serialization — no kotlinx.serialization dependency needed.
     * Produces a readable, indented JSON string.
     */
    fun export(data: ExportData): String = buildString {
        appendLine("{")
        appendLine("  \"runId\": \"${data.runId}\",")
        appendLine("  \"sourceSignalId\": \"${data.sourceSignalId}\",")
        appendLine("  \"pipelineVersion\": \"${data.pipelineVersion}\",")
        appendLine("  \"algorithmVersion\": \"${data.algorithmVersion}\",")
        appendLine("  \"presetName\": \"${data.presetName}\",")
        appendLine("  \"timestamp\": ${data.timestamp},")
        appendLine("  \"params\": ${data.paramsJson},")

        // Peaks
        appendLine("  \"peaks\": [")
        data.peaks.forEachIndexed { i, p ->
            val comma = if (i < data.peaks.size - 1) "," else ""
            appendLine("    {")
            appendLine("      \"peakId\": ${p.peakId},")
            appendLine("      \"status\": \"${p.status}\",")
            appendLine("      \"rtApex\": ${p.rtApex},")
            appendLine("      \"rtCentroid\": ${p.rtCentroid},")
            appendLine("      \"height\": ${p.height},")
            appendLine("      \"area\": ${p.area},")
            appendLine("      \"widthBase\": ${p.widthBase},")
            appendLine("      \"widthHalfHeight\": ${p.widthHalfHeight},")
            appendLine("      \"prominence\": ${p.prominence},")
            appendLine("      \"snr\": ${p.snr},")
            appendLine("      \"confidence\": \"${p.confidenceGrade}\",")
            appendLine("      \"confidenceScore\": ${p.confidenceScore},")
            appendLine("      \"overlap\": \"${p.overlapStatus}\",")
            appendLine("      \"leftBoundary\": ${p.leftBoundary},")
            appendLine("      \"rightBoundary\": ${p.rightBoundary},")
            appendLine("      \"manual\": ${p.isManuallyEdited}")
            appendLine("    }$comma")
        }
        appendLine("  ],")

        // Noise regions
        appendLine("  \"noiseRegions\": [")
        data.noiseRegions.forEachIndexed { i, n ->
            val comma = if (i < data.noiseRegions.size - 1) "," else ""
            appendLine("    {\"start\": ${n.startTime}, \"end\": ${n.endTime}, \"method\": \"${n.method}\", \"noise\": ${n.noiseValue}}$comma")
        }
        appendLine("  ],")

        // Manual edits (CSV string)
        appendLine("  \"manualEditsCsv\": \"${data.manualEdits.toCsv().replace("\"", "\\\"").replace("\n", "\\n")}\",")

        // Warnings
        appendLine("  \"warnings\": [")
        data.warnings.forEachIndexed { i, w ->
            val comma = if (i < data.warnings.size - 1) "," else ""
            appendLine("    {\"code\": \"${w.code}\", \"severity\": \"${w.severity}\", \"message\": \"${w.message.replace("\"", "\\\"")}\", \"peak\": ${w.peakIndex ?: "null"}, \"stage\": \"${w.stage}\"}$comma")
        }
        appendLine("  ]")

        appendLine("}")
    }
}

// ─── JSON: warnings only ────────────────────────────────────────

object WarningsJsonExporter {

    fun export(warnings: List<ExportWarning>): String = buildString {
        appendLine("[")
        warnings.forEachIndexed { i, w ->
            val comma = if (i < warnings.size - 1) "," else ""
            appendLine("  {\"code\": \"${w.code}\", \"severity\": \"${w.severity}\", \"message\": \"${w.message.replace("\"", "\\\"")}\", \"peak\": ${w.peakIndex ?: "null"}, \"stage\": \"${w.stage}\"}$comma")
        }
        appendLine("]")
    }
}

// ─── Export bundle ───────────────────────────────────────────────

/**
 * Generate all export files as a map of filename → content.
 * Platform layer writes these to disk and triggers share intent.
 */
fun generateExportBundle(data: ExportData): Map<String, String> = mapOf(
    "peaks.csv" to PeaksCsvExporter.export(data.peaks),
    "calculation.json" to CalculationJsonExporter.export(data),
    "corrected_signal.csv" to SignalCsvExporter.export(data.correctedSignal, "Corrected"),
    "baseline.csv" to SignalCsvExporter.export(data.baseline, "Baseline"),
    "warnings.json" to WarningsJsonExporter.export(data.warnings),
)
