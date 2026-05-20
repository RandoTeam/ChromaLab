package com.chromalab.feature.processing.multimodal

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutonomousStageJudgeContractsTest {
    @Test
    fun vlmForbiddenNumericFieldsAreRejected() {
        val contract = VlmStructuredTaskContracts.contractFor(StageJudgeTaskType.OCR_CROP_READ)
        val validation = ForbiddenVlmBoundaryPolicy.validateRawJsonFields(
            rawJson = """{"text":"5.610","text_type":"PEAK_ANNOTATION","confidence":0.81,"rt":5.610,"area":123.0}""",
            contract = contract,
        )

        assertFalse(validation.accepted)
        assertTrue(ForbiddenVlmNumericField.RT in validation.rejectedForbiddenFields)
        assertTrue(ForbiddenVlmNumericField.AREA in validation.rejectedForbiddenFields)
    }

    @Test
    fun localCropVlmFallbackCreatesSemanticEvidenceOnly() {
        val result = VlmOcrCropResult(
            resultId = "crop:1",
            taskId = "stage:1",
            source = StageJudgeSource.VLM,
            localCropPath = "crop.png",
            rawText = "5.610",
            parsedText = "5.610",
            textClass = MultimodalTextRegionClass.PEAK_ANNOTATION,
            rejectedForbiddenFields = listOf(ForbiddenVlmNumericField.RT),
        )

        assertTrue(result.acceptedNumericFields.isEmpty())
        assertEquals("5.610", result.parsedText)
        assertEquals(MultimodalTextRegionClass.PEAK_ANNOTATION, result.textClass)
    }

    @Test
    fun mlKitVlmDisagreementDefaultsToReview() {
        val disagreement = OcrVlmDisagreement(
            disagreementId = "disagreement:1",
            mlKitResultId = "mlkit:1",
            vlmResultId = "vlm:1",
            disagreementType = "text_value",
        )

        assertEquals(StageJudgeVerdict.REVIEW, disagreement.verdict)
    }

    @Test
    fun stageRetryRecommendationCannotFabricateMetrics() {
        val recommendation = StageRetryRecommendation(
            action = StageRetryAction.CREATE_FINAL_METRIC,
            reason = "Forbidden by VLM boundary policy.",
        )

        assertFalse(StageRetryPolicy.isAllowed(recommendation))
    }

    @Test
    fun cropBenchmarkExportsJsonAndMarkdown() {
        val report = OcrVlmCropBenchmarkHarness.buildReport(
            cases = listOf(
                OcrVlmCropBenchmarkCase(
                    caseId = "peak-label-001",
                    cropPath = "crop.png",
                    cropKind = "peak_annotation",
                    expectedText = "5.610",
                    expectedClass = MultimodalTextRegionClass.PEAK_ANNOTATION,
                ),
            ),
            observations = listOf(
                OcrVlmCropBenchmarkObservation(
                    caseId = "peak-label-001",
                    source = StageJudgeSource.ML_KIT,
                    rawText = "5.610",
                    textClass = MultimodalTextRegionClass.PEAK_ANNOTATION,
                    confidence = 0.82f,
                ),
                OcrVlmCropBenchmarkObservation(
                    caseId = "peak-label-001",
                    source = StageJudgeSource.VLM,
                    rawText = "5.610",
                    textClass = MultimodalTextRegionClass.PEAK_ANNOTATION,
                    confidence = 0.75f,
                ),
            ),
            generatedAtEpochMillis = 1L,
        )

        val json = OcrVlmCropBenchmarkHarness.exportJson(report)
        val markdown = OcrVlmCropBenchmarkHarness.renderMarkdown(report)

        assertEquals(0, report.failedCases)
        assertTrue(markdown.contains("OCR/VLM Crop Benchmark"))
        assertEquals("ocr-vlm-crop-benchmark-1.0", Json.decodeFromString<OcrVlmCropBenchmarkReport>(json).schemaVersion)
    }
}

