package com.chromalab.feature.processing.model

import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.reports.ReportSeverity
import com.chromalab.feature.reports.ReportValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelAssistedAnalysisContractTest {

    @Test
    fun strictPhotoContractKeepsModelsOutOfNumericCalculationAndFinalAssignments() {
        val numeric = ModelAssistedAnalysisContract.stage(ModelAssistedStageRole.NUMERIC_CALCULATION)
        val interpretation = ModelAssistedAnalysisContract.stage(ModelAssistedStageRole.CHEMICAL_INTERPRETATION)
        val graphRegion = ModelAssistedAnalysisContract.stage(ModelAssistedStageRole.GRAPH_REGION)

        assertEquals(ModelAssistedStageMode.DETERMINISTIC_ONLY, numeric.mode)
        assertTrue(numeric.canProduceNumericResults)
        assertEquals(setOf(ReportValueSource.DETERMINISTIC, ReportValueSource.USER), numeric.allowedSources)

        assertEquals(ModelAssistedStageMode.LOCAL_KNOWLEDGE_ONLY, interpretation.mode)
        assertFalse(interpretation.canProduceNumericResults)
        assertFalse(interpretation.canAssignFinalCompounds)

        assertEquals(ModelAssistedStageMode.OPTIONAL_VISION_HINT, graphRegion.mode)
        assertTrue(graphRegion.deterministicRunsFirst)
        assertFalse(graphRegion.canProduceNumericResults)
        assertFalse(graphRegion.canAssignFinalCompounds)
    }

    @Test
    fun requiredVisionFailuresAreFailedReportWarnings() {
        val warning = ModelAssistedAnalysisContract.failureWarning(
            stage = ModelAssistedAnalysisContract.stage(ModelAssistedStageRole.GRAPH_REGION),
            detail = "AI graph detection failed.",
            graphIndex = 2,
        )

        assertEquals("model.graph_region.required_vision_failed", warning.code)
        assertEquals(ReportSeverity.FAILED, warning.severity)
        assertEquals("model.graph_region", warning.stage)
        assertEquals(2, warning.graphIndex)
        assertTrue(warning.message.contains("cannot be replaced by deterministic-only output"))
    }

    @Test
    fun eligibilityRequiresChromatogramVlmFamilyAndGgufVisionPair() {
        val validQwen = model(
            id = "qwen-valid",
            family = "qwen3-vl",
            files = listOf(
                ModelFile("qwen.gguf", 100L, ModelFileType.GGUF_BASE, ""),
                ModelFile("mmproj-qwen.gguf", 10L, ModelFileType.GGUF_MMPROJ, ""),
            ),
        )
        val baseOnlyQwen = model(
            id = "qwen-base-only",
            family = "qwen3-vl",
            files = listOf(ModelFile("qwen.gguf", 100L, ModelFileType.GGUF_BASE, "")),
        )
        val ocrOnly = model(
            id = "paddle",
            family = "paddleocr-vl",
            files = listOf(
                ModelFile("paddle.gguf", 100L, ModelFileType.GGUF_BASE, ""),
                ModelFile("mmproj-paddle.gguf", 10L, ModelFileType.GGUF_MMPROJ, ""),
            ),
        )

        assertTrue(ModelAssistedAnalysisContract.evaluateChromatogramVisionEligibility(validQwen).eligible)

        val baseOnly = ModelAssistedAnalysisContract.evaluateChromatogramVisionEligibility(baseOnlyQwen)
        assertFalse(baseOnly.eligible)
        assertTrue(baseOnly.reasons.any { it.contains("mmproj") })

        val ocr = ModelAssistedAnalysisContract.evaluateChromatogramVisionEligibility(ocrOnly)
        assertFalse(ocr.eligible)
        assertTrue(ocr.reasons.any { it.contains("OCR/document-only") })
    }

    private fun model(
        id: String,
        family: String,
        files: List<ModelFile>,
        supportsVision: Boolean = true,
    ): ModelInfo =
        ModelInfo(
            id = id,
            displayName = id,
            family = family,
            runtime = ModelRuntime.LLAMA_CPP,
            files = files,
            minRamMb = 4096,
            isBuiltin = false,
            supportsVision = supportsVision,
        )
}
