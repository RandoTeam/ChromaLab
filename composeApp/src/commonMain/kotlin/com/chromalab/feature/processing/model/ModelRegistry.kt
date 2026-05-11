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
 *
 * URLs verified against HuggingFace API on 2026-05-11.
 * All repos confirmed to exist with correct file names and sizes.
 *
 * Available models:
 *   LiteRT-LM:  Gemma 4 E2B (2.59 GB)
 *   llama.cpp:  Qwen2.5-VL-3B (1.93+0.84 GB), Qwen2.5-VL-7B (4.68+0.85 GB),
 *               Qwen3.5-VL-9B (5.68+0.92 GB)
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
                sizeBytes = 2_588_147_712L,
                type = ModelFileType.LITERT_BUNDLE,
                downloadUrl = "$HF_BASE/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            ),
        ),
        minRamMb = 4096,
        isBuiltin = true,
        supportsVision = true,
        description = "Быстрая модель с NPU/GPU ускорением. Рекомендуется для большинства устройств.",
    )

    // ===== llama.cpp GGUF models =====
    // Source: ggml-org (official llama.cpp GGUF repos), jc-builds (Qwen3.5)
    // All URLs verified via HuggingFace API 2026-05-11

    private val qwen25vl3b = ModelInfo(
        id = "qwen25vl-3b",
        displayName = "Qwen2.5-VL 3B",
        family = "qwen2.5-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "Qwen2.5-VL-3B-Instruct-Q4_K_M.gguf",
                sizeBytes = 1_929_901_056L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$HF_BASE/ggml-org/Qwen2.5-VL-3B-Instruct-GGUF/resolve/main/Qwen2.5-VL-3B-Instruct-Q4_K_M.gguf",
            ),
            ModelFile(
                fileName = "mmproj-Qwen2.5-VL-3B-Instruct-Q8_0.gguf",
                sizeBytes = 844_757_728L,
                type = ModelFileType.GGUF_MMPROJ,
                downloadUrl = "$HF_BASE/ggml-org/Qwen2.5-VL-3B-Instruct-GGUF/resolve/main/mmproj-Qwen2.5-VL-3B-Instruct-Q8_0.gguf",
            ),
        ),
        minRamMb = 4096,
        isBuiltin = true,
        supportsVision = true,
        description = "Лёгкая VLM модель. Q4_K_M квантизация. 4+ GB RAM.",
    )

    private val qwen25vl7b = ModelInfo(
        id = "qwen25vl-7b",
        displayName = "Qwen2.5-VL 7B",
        family = "qwen2.5-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "Qwen2.5-VL-7B-Instruct-Q4_K_M.gguf",
                sizeBytes = 4_683_072_032L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$HF_BASE/ggml-org/Qwen2.5-VL-7B-Instruct-GGUF/resolve/main/Qwen2.5-VL-7B-Instruct-Q4_K_M.gguf",
            ),
            ModelFile(
                fileName = "mmproj-Qwen2.5-VL-7B-Instruct-Q8_0.gguf",
                sizeBytes = 853_119_712L,
                type = ModelFileType.GGUF_MMPROJ,
                downloadUrl = "$HF_BASE/ggml-org/Qwen2.5-VL-7B-Instruct-GGUF/resolve/main/mmproj-Qwen2.5-VL-7B-Instruct-Q8_0.gguf",
            ),
        ),
        minRamMb = 8192,
        isBuiltin = true,
        supportsVision = true,
        description = "Высокое качество распознавания. Q4_K_M. 8+ GB RAM.",
    )

    private val qwen35vl9b = ModelInfo(
        id = "qwen35vl-9b",
        displayName = "Qwen3.5-VL 9B",
        family = "qwen3.5-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "Qwen3.5-9B-Q4_K_M.gguf",
                sizeBytes = 5_680_522_464L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$HF_BASE/jc-builds/Qwen3.5-9B-VLM-Q4_K_M-GGUF/resolve/main/Qwen3.5-9B-Q4_K_M.gguf",
            ),
            ModelFile(
                fileName = "mmproj-F16.gguf",
                sizeBytes = 918_166_080L,
                type = ModelFileType.GGUF_MMPROJ,
                downloadUrl = "$HF_BASE/jc-builds/Qwen3.5-9B-VLM-Q4_K_M-GGUF/resolve/main/mmproj-F16.gguf",
            ),
        ),
        minRamMb = 12288,
        isBuiltin = true,
        supportsVision = true,
        description = "Максимальное качество. Только для устройств с 12+ GB RAM.",
    )

    // ===== Public API =====

    /** All built-in models. LiteRT first (recommended), then GGUF by size. */
    val builtinModels: List<ModelInfo> = listOf(
        gemma4E2B,
        qwen25vl3b,
        qwen25vl7b,
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
