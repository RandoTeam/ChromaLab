package com.chromalab.feature.processing.model

import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.reports.ReportSeverity
import com.chromalab.feature.reports.ReportValueSource
import com.chromalab.feature.reports.ReportWarning

enum class ModelAssistedStageRole {
    GRAPH_REGION,
    TITLE_ION_AXIS,
    AXIS_STRUCTURE,
    CHEMICAL_INTERPRETATION,
    NUMERIC_CALCULATION,
}

enum class ModelAssistedStageMode {
    DETERMINISTIC_ONLY,
    REQUIRED_VISION_CONTRACT,
    OPTIONAL_VISION_HINT,
    LOCAL_KNOWLEDGE_ONLY,
}

data class ModelAssistedStageSpec(
    val stageId: String,
    val role: ModelAssistedStageRole,
    val mode: ModelAssistedStageMode,
    val deterministicRunsFirst: Boolean,
    val allowedSources: Set<ReportValueSource>,
    val canProduceNumericResults: Boolean,
    val canAssignFinalCompounds: Boolean,
    val blockingFailureMarker: String,
)

data class ModelEligibility(
    val eligible: Boolean,
    val reasons: List<String> = emptyList(),
)

object ModelAssistedAnalysisContract {

    const val MISSING_CHROMATOGRAM_VLM_MESSAGE =
        "AI vision model is required for photo chromatogram analysis. Download or activate a chromatography VLM first."

    val strictPhotoStages: List<ModelAssistedStageSpec> = listOf(
        ModelAssistedStageSpec(
            stageId = "model.graph_region",
            role = ModelAssistedStageRole.GRAPH_REGION,
            mode = ModelAssistedStageMode.REQUIRED_VISION_CONTRACT,
            deterministicRunsFirst = true,
            allowedSources = setOf(ReportValueSource.VISION_MODEL, ReportValueSource.DETERMINISTIC),
            canProduceNumericResults = false,
            canAssignFinalCompounds = false,
            blockingFailureMarker = "ai graph detection",
        ),
        ModelAssistedStageSpec(
            stageId = "model.title_ion_axis",
            role = ModelAssistedStageRole.TITLE_ION_AXIS,
            mode = ModelAssistedStageMode.REQUIRED_VISION_CONTRACT,
            deterministicRunsFirst = true,
            allowedSources = setOf(ReportValueSource.VISION_MODEL, ReportValueSource.OCR),
            canProduceNumericResults = false,
            canAssignFinalCompounds = false,
            blockingFailureMarker = "ai axis extraction",
        ),
        ModelAssistedStageSpec(
            stageId = "model.axis_structure",
            role = ModelAssistedStageRole.AXIS_STRUCTURE,
            mode = ModelAssistedStageMode.OPTIONAL_VISION_HINT,
            deterministicRunsFirst = true,
            allowedSources = setOf(ReportValueSource.VISION_MODEL, ReportValueSource.DETERMINISTIC),
            canProduceNumericResults = false,
            canAssignFinalCompounds = false,
            blockingFailureMarker = "ai axis structure",
        ),
        ModelAssistedStageSpec(
            stageId = "model.chemical_interpretation",
            role = ModelAssistedStageRole.CHEMICAL_INTERPRETATION,
            mode = ModelAssistedStageMode.LOCAL_KNOWLEDGE_ONLY,
            deterministicRunsFirst = true,
            allowedSources = setOf(
                ReportValueSource.LOCAL_KNOWLEDGE,
                ReportValueSource.MODEL_SUGGESTED,
                ReportValueSource.USER,
            ),
            canProduceNumericResults = false,
            canAssignFinalCompounds = false,
            blockingFailureMarker = "chemical interpretation",
        ),
        ModelAssistedStageSpec(
            stageId = "calculation.numeric_results",
            role = ModelAssistedStageRole.NUMERIC_CALCULATION,
            mode = ModelAssistedStageMode.DETERMINISTIC_ONLY,
            deterministicRunsFirst = true,
            allowedSources = setOf(ReportValueSource.DETERMINISTIC, ReportValueSource.USER),
            canProduceNumericResults = true,
            canAssignFinalCompounds = false,
            blockingFailureMarker = "numeric calculation",
        ),
    )

    fun stage(role: ModelAssistedStageRole): ModelAssistedStageSpec =
        strictPhotoStages.first { it.role == role }

    fun requiredVisionStages(): List<ModelAssistedStageSpec> =
        strictPhotoStages.filter { it.mode == ModelAssistedStageMode.REQUIRED_VISION_CONTRACT }

    fun blockingErrorMarkers(): List<String> =
        listOf(
            "ai vision model is required",
            "ai vision model is not loaded",
            "chromatography vlm",
            "vision projector is missing",
            "vision support",
            "image input",
            "ai vision analysis did not produce",
        ) + requiredVisionStages().map { it.blockingFailureMarker }

    fun failureWarning(
        stage: ModelAssistedStageSpec,
        detail: String,
        graphIndex: Int? = null,
    ): ReportWarning =
        ReportWarning(
            code = "${stage.stageId}.required_vision_failed",
            message = "$detail This strict analysis stage cannot be replaced by deterministic-only output.",
            severity = ReportSeverity.FAILED,
            stage = stage.stageId,
            graphIndex = graphIndex,
        )

    fun evaluateChromatogramVisionEligibility(model: ModelInfo): ModelEligibility {
        val reasons = buildList {
            if (!model.supportsVision) {
                add("Model metadata does not declare image input support.")
            }
            if (model.family.lowercase() in ocrOnlyFamilies) {
                add("OCR/document-only model family is not accepted for strict chromatogram VLM analysis.")
            }
            if (!isKnownChromatogramVisionFamily(model.family)) {
                add("Model family is not in the chromatogram VLM allow-list.")
            }
            if (model.runtime == ModelRuntime.LLAMA_CPP) {
                val hasBase = model.files.any { it.type == ModelFileType.GGUF_BASE }
                val hasMmproj = model.files.any { it.type == ModelFileType.GGUF_MMPROJ }
                if (!hasBase) add("GGUF image analysis requires a base GGUF model file.")
                if (!hasMmproj) add("GGUF image analysis requires a matching mmproj vision projector.")
            }
        }
        return ModelEligibility(
            eligible = reasons.isEmpty(),
            reasons = reasons,
        )
    }

    private fun isKnownChromatogramVisionFamily(family: String): Boolean {
        val normalized = family.lowercase()
        return normalized.contains("gemma") ||
            normalized.contains("fastvlm") ||
            normalized.contains("smolvlm") ||
            normalized.contains("moondream") ||
            normalized.contains("qwen")
    }

    private val ocrOnlyFamilies = setOf(
        "paddleocr-vl",
        "dots-mocr",
        "deepseek-ocr",
    )
}
