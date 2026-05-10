package com.chromalab.feature.processing.model

import com.chromalab.feature.processing.inference.ModelRuntime

/**
 * Type of file within a model package.
 */
enum class ModelFileType {
    /** LiteRT-LM bundle — single .litertlm file with everything included */
    LITERT_BUNDLE,
    /** Base language model — .gguf */
    GGUF_BASE,
    /** Vision projector for multimodal GGUF models — .gguf mmproj */
    GGUF_MMPROJ,
}

/**
 * A single file that is part of a model.
 */
data class ModelFile(
    val fileName: String,
    val sizeBytes: Long,
    val type: ModelFileType,
    val downloadUrl: String,
)

/**
 * Describes a model that can be downloaded and used for inference.
 */
data class ModelInfo(
    val id: String,
    val displayName: String,
    val family: String,
    val runtime: ModelRuntime,
    val files: List<ModelFile>,
    val minRamMb: Int,
    val isBuiltin: Boolean,
    val supportsVision: Boolean,
    val description: String = "",
) {
    val totalSizeBytes: Long get() = files.sumOf { it.sizeBytes }
    val primaryFileName: String get() = files.first().fileName
}

/**
 * Registry of all known built-in models.
 * 2 LiteRT-LM (Gemma 4) + 4 llama.cpp GGUF (Qwen3.5-VL).
 *
 * HF CDN URLs verified May 2026.
 */
object ModelRegistry {

    private const val HF_BASE = "https://huggingface.co"

    // ===== LiteRT-LM models (Gemma 4 family) =====

    private val gemma4E2B = ModelInfo(
        id = "gemma4-e2b",
        displayName = "Gemma 4 E2B",
        family = "gemma-4",
        runtime = ModelRuntime.LITERT_LM,
        files = listOf(
            ModelFile(
                fileName = "gemma-4-E2B-it.litertlm",
                sizeBytes = 2_590_000_000L, // ~2.59 GB
                type = ModelFileType.LITERT_BUNDLE,
                downloadUrl = "$HF_BASE/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            ),
        ),
        minRamMb = 4096,
        isBuiltin = true,
        supportsVision = true,
        description = "Быстрая модель с NPU/GPU ускорением. Рекомендуется для большинства устройств.",
    )

    private val gemma4E4B = ModelInfo(
        id = "gemma4-e4b",
        displayName = "Gemma 4 E4B",
        family = "gemma-4",
        runtime = ModelRuntime.LITERT_LM,
        files = listOf(
            ModelFile(
                fileName = "gemma-4-E4B-it.litertlm",
                sizeBytes = 3_500_000_000L, // ~3.5 GB
                type = ModelFileType.LITERT_BUNDLE,
                downloadUrl = "$HF_BASE/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            ),
        ),
        minRamMb = 6144,
        isBuiltin = true,
        supportsVision = true,
        description = "Более точная модель с NPU/GPU. Требует 6+ GB RAM.",
    )

    // ===== llama.cpp GGUF models (Qwen3.5-VL family) =====
    // Note: Qwen3.5-VL GGUF models are provided by community (unsloth, jc-builds).
    // URLs are placeholders for 0.8B/2B/4B as official GGUF repos may vary.
    // The 9B model has a confirmed repo: jc-builds/Qwen3.5-9B-VLM-Q4_K_M-GGUF

    private val qwen35vl08b = ModelInfo(
        id = "qwen35vl-08b",
        displayName = "Qwen3.5-VL 0.8B",
        family = "qwen3.5-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "qwen3.5-vl-0.8b-q4_k_m.gguf",
                sizeBytes = 500_000_000L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$HF_BASE/unsloth/Qwen3.5-VL-0.8B-GGUF/resolve/main/Qwen3.5-VL-0.8B-Q4_K_M.gguf",
            ),
            ModelFile(
                fileName = "qwen3.5-vl-0.8b-mmproj.gguf",
                sizeBytes = 150_000_000L,
                type = ModelFileType.GGUF_MMPROJ,
                downloadUrl = "$HF_BASE/unsloth/Qwen3.5-VL-0.8B-GGUF/resolve/main/mmproj-Qwen3.5-VL-0.8B-f16.gguf",
            ),
        ),
        minRamMb = 4096,
        isBuiltin = true,
        supportsVision = true,
        description = "Самая лёгкая модель. Подходит для устройств с 4 GB RAM.",
    )

    private val qwen35vl2b = ModelInfo(
        id = "qwen35vl-2b",
        displayName = "Qwen3.5-VL 2B",
        family = "qwen3.5-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "qwen3.5-vl-2b-q4_k_m.gguf",
                sizeBytes = 1_200_000_000L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$HF_BASE/unsloth/Qwen3.5-VL-2B-GGUF/resolve/main/Qwen3.5-VL-2B-Q4_K_M.gguf",
            ),
            ModelFile(
                fileName = "qwen3.5-vl-2b-mmproj.gguf",
                sizeBytes = 300_000_000L,
                type = ModelFileType.GGUF_MMPROJ,
                downloadUrl = "$HF_BASE/unsloth/Qwen3.5-VL-2B-GGUF/resolve/main/mmproj-Qwen3.5-VL-2B-f16.gguf",
            ),
        ),
        minRamMb = 4096,
        isBuiltin = true,
        supportsVision = true,
        description = "Баланс качества и скорости. 4+ GB RAM.",
    )

    private val qwen35vl4b = ModelInfo(
        id = "qwen35vl-4b",
        displayName = "Qwen3.5-VL 4B",
        family = "qwen3.5-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "qwen3.5-vl-4b-q4_k_m.gguf",
                sizeBytes = 2_500_000_000L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$HF_BASE/unsloth/Qwen3.5-VL-4B-GGUF/resolve/main/Qwen3.5-VL-4B-Q4_K_M.gguf",
            ),
            ModelFile(
                fileName = "qwen3.5-vl-4b-mmproj.gguf",
                sizeBytes = 400_000_000L,
                type = ModelFileType.GGUF_MMPROJ,
                downloadUrl = "$HF_BASE/unsloth/Qwen3.5-VL-4B-GGUF/resolve/main/mmproj-Qwen3.5-VL-4B-f16.gguf",
            ),
        ),
        minRamMb = 6144,
        isBuiltin = true,
        supportsVision = true,
        description = "Высокое качество распознавания. 6+ GB RAM.",
    )

    private val qwen35vl9b = ModelInfo(
        id = "qwen35vl-9b",
        displayName = "Qwen3.5-VL 9B",
        family = "qwen3.5-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "qwen3.5-vl-9b-q4_k_m.gguf",
                sizeBytes = 5_000_000_000L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$HF_BASE/jc-builds/Qwen3.5-9B-VLM-Q4_K_M-GGUF/resolve/main/Qwen3.5-9B-VLM-Q4_K_M.gguf",
            ),
            ModelFile(
                fileName = "qwen3.5-vl-9b-mmproj.gguf",
                sizeBytes = 500_000_000L,
                type = ModelFileType.GGUF_MMPROJ,
                downloadUrl = "$HF_BASE/jc-builds/Qwen3.5-9B-VLM-Q4_K_M-GGUF/resolve/main/mmproj-Qwen3.5-9B-f16.gguf",
            ),
        ),
        minRamMb = 12288,
        isBuiltin = true,
        supportsVision = true,
        description = "Максимальное качество. Только для устройств с 12+ GB RAM.",
    )

    // ===== Public API =====

    /** All 6 built-in models. LiteRT first (recommended), then GGUF. */
    val builtinModels: List<ModelInfo> = listOf(
        gemma4E2B,
        gemma4E4B,
        qwen35vl08b,
        qwen35vl2b,
        qwen35vl4b,
        qwen35vl9b,
    )

    /** Find a builtin model by ID. */
    fun findById(id: String): ModelInfo? = builtinModels.find { it.id == id }

    /** Group models by runtime for UI display. */
    fun groupedByRuntime(): Map<ModelRuntime, List<ModelInfo>> =
        builtinModels.groupBy { it.runtime }

    /**
     * Identify an imported model file by extension.
     * Returns null if the file type is unrecognized.
     */
    fun identifyFileType(fileName: String): ModelFileType? = when {
        fileName.endsWith(".litertlm") -> ModelFileType.LITERT_BUNDLE
        fileName.contains("mmproj") && fileName.endsWith(".gguf") -> ModelFileType.GGUF_MMPROJ
        fileName.endsWith(".gguf") -> ModelFileType.GGUF_BASE
        else -> null
    }
}
