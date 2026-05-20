package com.chromalab.feature.processing.multimodal

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class OcrVlmCropBenchmarkCase(
    val caseId: String,
    val cropPath: String,
    val cropKind: String,
    val expectedText: String,
    val expectedClass: MultimodalTextRegionClass,
    val notes: String? = null,
)

@Serializable
data class OcrVlmCropBenchmarkObservation(
    val caseId: String,
    val source: StageJudgeSource,
    val rawText: String,
    val normalizedText: String = rawText.trim(),
    val textClass: MultimodalTextRegionClass = MultimodalTextRegionClass.UNKNOWN_TEXT,
    val confidence: Float = 0f,
    val durationMillis: Long? = null,
    val errorCode: String? = null,
)

@Serializable
data class OcrVlmCropBenchmarkRow(
    val caseId: String,
    val cropKind: String,
    val expectedText: String,
    val expectedClass: MultimodalTextRegionClass,
    val mlKitText: String? = null,
    val vlmText: String? = null,
    val mlKitCharacterErrorRate: Double? = null,
    val vlmCharacterErrorRate: Double? = null,
    val mlKitClassMatches: Boolean? = null,
    val vlmClassMatches: Boolean? = null,
    val disagreement: Boolean = false,
    val finalAcceptedSource: StageJudgeSource? = null,
    val finalAcceptedText: String? = null,
    val finalAcceptedClass: MultimodalTextRegionClass? = null,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OcrVlmCropBenchmarkReport(
    val schemaVersion: String = "ocr-vlm-crop-benchmark-1.0",
    val generatedAtEpochMillis: Long,
    val totalCases: Int,
    val rows: List<OcrVlmCropBenchmarkRow>,
) {
    val failedCases: Int
        get() = rows.count { row ->
            row.finalAcceptedText == null ||
                row.finalAcceptedClass != row.expectedClass ||
                normalize(row.finalAcceptedText) != normalize(row.expectedText)
        }
}

object OcrVlmCropBenchmarkHarness {
    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

    fun buildReport(
        cases: List<OcrVlmCropBenchmarkCase>,
        observations: List<OcrVlmCropBenchmarkObservation>,
        generatedAtEpochMillis: Long = currentTimeMillis(),
    ): OcrVlmCropBenchmarkReport {
        val byCase = observations.groupBy { it.caseId }
        val rows = cases.map { case ->
            val mlKit = byCase[case.caseId].orEmpty().firstOrNull { it.source == StageJudgeSource.ML_KIT }
            val vlm = byCase[case.caseId].orEmpty().firstOrNull { it.source == StageJudgeSource.VLM }
            val mlKitCer = mlKit?.normalizedText?.let { characterErrorRate(case.expectedText, it) }
            val vlmCer = vlm?.normalizedText?.let { characterErrorRate(case.expectedText, it) }
            val mlKitClassMatches = mlKit?.let { it.textClass == case.expectedClass }
            val vlmClassMatches = vlm?.let { it.textClass == case.expectedClass }
            val disagreement = mlKit != null &&
                vlm != null &&
                (normalize(mlKit.normalizedText) != normalize(vlm.normalizedText) || mlKit.textClass != vlm.textClass)
            val accepted = chooseAcceptedObservation(case, mlKit, vlm)
            OcrVlmCropBenchmarkRow(
                caseId = case.caseId,
                cropKind = case.cropKind,
                expectedText = case.expectedText,
                expectedClass = case.expectedClass,
                mlKitText = mlKit?.normalizedText,
                vlmText = vlm?.normalizedText,
                mlKitCharacterErrorRate = mlKitCer,
                vlmCharacterErrorRate = vlmCer,
                mlKitClassMatches = mlKitClassMatches,
                vlmClassMatches = vlmClassMatches,
                disagreement = disagreement,
                finalAcceptedSource = accepted?.source,
                finalAcceptedText = accepted?.normalizedText,
                finalAcceptedClass = accepted?.textClass,
                warnings = buildList {
                    if (mlKit == null) add("benchmark.mlkit_missing")
                    if (vlm == null) add("benchmark.vlm_missing")
                    if (disagreement) add("benchmark.mlkit_vlm_disagreement")
                    if (accepted == null) add("benchmark.no_accepted_text")
                },
            )
        }
        return OcrVlmCropBenchmarkReport(
            generatedAtEpochMillis = generatedAtEpochMillis,
            totalCases = cases.size,
            rows = rows,
        )
    }

    fun exportJson(report: OcrVlmCropBenchmarkReport): String = json.encodeToString(report)

    fun renderMarkdown(report: OcrVlmCropBenchmarkReport): String = buildString {
        appendLine("# OCR/VLM Crop Benchmark")
        appendLine()
        appendLine("- Total cases: ${report.totalCases}")
        appendLine("- Failed cases: ${report.failedCases}")
        appendLine()
        appendLine("| Case | Kind | Expected | Class | ML Kit | VLM | Accepted | Warnings |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- |")
        report.rows.forEach { row ->
            appendLine(
                "| ${row.caseId} | ${row.cropKind} | ${row.expectedText} | ${row.expectedClass} | " +
                    "${row.mlKitText ?: "-"} | ${row.vlmText ?: "-"} | " +
                    "${row.finalAcceptedSource ?: "-"}:${row.finalAcceptedText ?: "-"} | " +
                    "${row.warnings.joinToString(", ").ifBlank { "-" }} |",
            )
        }
    }

    private fun chooseAcceptedObservation(
        case: OcrVlmCropBenchmarkCase,
        mlKit: OcrVlmCropBenchmarkObservation?,
        vlm: OcrVlmCropBenchmarkObservation?,
    ): OcrVlmCropBenchmarkObservation? =
        listOfNotNull(mlKit, vlm)
            .filter { it.errorCode == null }
            .filter { it.textClass == case.expectedClass }
            .minByOrNull { characterErrorRate(case.expectedText, it.normalizedText) }
            ?.takeIf { characterErrorRate(case.expectedText, it.normalizedText) <= ACCEPTABLE_CER }

    fun characterErrorRate(expected: String, actual: String): Double {
        val normalizedExpected = normalize(expected)
        val normalizedActual = normalize(actual)
        if (normalizedExpected.isEmpty()) return if (normalizedActual.isEmpty()) 0.0 else 1.0
        return levenshtein(normalizedExpected, normalizedActual).toDouble() / normalizedExpected.length.toDouble()
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        for (i in left.indices) {
            current[0] = i + 1
            for (j in right.indices) {
                val cost = if (left[i] == right[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + cost,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[right.length]
    }

    private const val ACCEPTABLE_CER = 0.05
}

private fun normalize(text: String): String =
    text.trim().lowercase().replace(Regex("\\s+"), " ")

private fun currentTimeMillis(): Long = System.currentTimeMillis()
