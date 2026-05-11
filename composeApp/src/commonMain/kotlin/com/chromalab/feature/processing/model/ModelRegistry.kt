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
    /** Parent group ID for expandable UI (null = standalone). */
    val groupId: String? = null,
    /** Quantization label for display (e.g. "Q4_K_M", "Q8_0"). */
    val quantLabel: String? = null,
) {
    val totalSizeBytes: Long get() = files.sumOf { it.sizeBytes }
    val primaryFileName: String get() = files.first().fileName
}

/**
 * A group of quantization variants for the same base model.
 * Used for the expandable UI in ModelManagerScreen.
 */
data class ModelGroup(
    val groupId: String,
    val displayName: String,
    val family: String,
    val runtime: ModelRuntime,
    val description: String,
    val supportsVision: Boolean,
    val variants: List<ModelInfo>,
)

/**
 * Registry of all known built-in models.
 *
 * URLs verified against HuggingFace API on 2026-05-11.
 *
 * Available models:
 *   LiteRT-LM:  Gemma 4 E2B (2.59 GB), Gemma 4 E4B (12.9 GB)
 *   llama.cpp:  Qwen2.5-VL-3B (6 quants), Qwen2.5-VL-7B (6 quants),
 *               Qwen3.5-VL-9B (Q4_K_M)
 */
object ModelRegistry {

    private const val HF_BASE = "https://huggingface.co"
    private const val UNSLOTH = "$HF_BASE/unsloth"
    private const val GGML_ORG = "$HF_BASE/ggml-org"

    // ===== Shared mmproj files (one per model family, Q8_0 quality) =====

    private val mmproj3B = ModelFile(
        fileName = "mmproj-Qwen2.5-VL-3B-Instruct-Q8_0.gguf",
        sizeBytes = 844_757_728L,
        type = ModelFileType.GGUF_MMPROJ,
        downloadUrl = "$GGML_ORG/Qwen2.5-VL-3B-Instruct-GGUF/resolve/main/mmproj-Qwen2.5-VL-3B-Instruct-Q8_0.gguf",
    )

    private val mmproj7B = ModelFile(
        fileName = "mmproj-Qwen2.5-VL-7B-Instruct-Q8_0.gguf",
        sizeBytes = 853_119_712L,
        type = ModelFileType.GGUF_MMPROJ,
        downloadUrl = "$GGML_ORG/Qwen2.5-VL-7B-Instruct-GGUF/resolve/main/mmproj-Qwen2.5-VL-7B-Instruct-Q8_0.gguf",
    )

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

    private val gemma4E4B = ModelInfo(
        id = "gemma4-e4b",
        displayName = "Gemma 4 E4B",
        family = "gemma-4",
        runtime = ModelRuntime.LITERT_LM,
        files = listOf(
            ModelFile(
                fileName = "gemma-4-E4B-it.litertlm",
                sizeBytes = 13_887_717_376L,
                type = ModelFileType.LITERT_BUNDLE,
                downloadUrl = "$HF_BASE/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            ),
        ),
        minRamMb = 12288,
        isBuiltin = true,
        supportsVision = true,
        description = "Высокая точность с NPU/GPU. 12+ GB RAM. ~12.9 GB.",
    )

    // ===== Qwen2.5-VL-3B quantization variants =====
    // Source: unsloth/Qwen2.5-VL-3B-Instruct-GGUF

    private fun qwen3BVariant(quant: String, sizeBytes: Long, minRam: Int, desc: String) = ModelInfo(
        id = "qwen25vl-3b-${quant.lowercase().replace("_", "")}",
        displayName = "3B · $quant",
        family = "qwen2.5-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "Qwen2.5-VL-3B-Instruct-$quant.gguf",
                sizeBytes = sizeBytes,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$UNSLOTH/Qwen2.5-VL-3B-Instruct-GGUF/resolve/main/Qwen2.5-VL-3B-Instruct-$quant.gguf",
            ),
            mmproj3B,
        ),
        minRamMb = minRam,
        isBuiltin = true,
        supportsVision = true,
        description = desc,
        groupId = "qwen25vl-3b",
        quantLabel = quant,
    )

    private val qwen3B_Q2_K   = qwen3BVariant("Q2_K",   1_274_754_400L, 2048,  "Минимальный. ~1.2 GB. Низкое качество.")
    private val qwen3B_Q3_K_M = qwen3BVariant("Q3_K_M", 1_590_474_080L, 3072,  "Лёгкий. ~1.5 GB. Приемлемое качество.")
    private val qwen3B_Q4_K_M = qwen3BVariant("Q4_K_M", 1_929_901_408L, 4096,  "Оптимальный баланс. ~1.8 GB.")
    private val qwen3B_Q5_K_M = qwen3BVariant("Q5_K_M", 2_224_813_408L, 4096,  "Высокое качество. ~2.1 GB.")
    private val qwen3B_Q6_K   = qwen3BVariant("Q6_K",   2_538_157_408L, 6144,  "Очень высокое качество. ~2.4 GB.")
    private val qwen3B_Q8_0   = qwen3BVariant("Q8_0",   3_285_474_656L, 6144,  "Максимальное качество. ~3.1 GB.")

    // ===== Qwen2.5-VL-7B quantization variants =====
    // Source: unsloth/Qwen2.5-VL-7B-Instruct-GGUF

    private fun qwen7BVariant(quant: String, sizeBytes: Long, minRam: Int, desc: String) = ModelInfo(
        id = "qwen25vl-7b-${quant.lowercase().replace("_", "")}",
        displayName = "7B · $quant",
        family = "qwen2.5-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "Qwen2.5-VL-7B-Instruct-$quant.gguf",
                sizeBytes = sizeBytes,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$UNSLOTH/Qwen2.5-VL-7B-Instruct-GGUF/resolve/main/Qwen2.5-VL-7B-Instruct-$quant.gguf",
            ),
            mmproj7B,
        ),
        minRamMb = minRam,
        isBuiltin = true,
        supportsVision = true,
        description = desc,
        groupId = "qwen25vl-7b",
        quantLabel = quant,
    )

    private val qwen7B_Q2_K   = qwen7BVariant("Q2_K",   3_015_938_944L, 6144,  "Минимальный. ~2.8 GB. Низкое качество.")
    private val qwen7B_Q3_K_M = qwen7BVariant("Q3_K_M", 3_808_390_016L, 6144,  "Лёгкий. ~3.5 GB. Приемлемое качество.")
    private val qwen7B_Q4_K_M = qwen7BVariant("Q4_K_M", 4_683_072_384L, 8192,  "Оптимальный баланс. ~4.4 GB.")
    private val qwen7B_Q5_K_M = qwen7BVariant("Q5_K_M", 5_444_830_080L, 8192,  "Высокое качество. ~5.1 GB.")
    private val qwen7B_Q6_K   = qwen7BVariant("Q6_K",   6_254_197_632L, 10240, "Очень высокое качество. ~5.8 GB.")
    private val qwen7B_Q8_0   = qwen7BVariant("Q8_0",   8_098_524_032L, 12288, "Максимальное качество. ~7.5 GB.")

    // ===== Qwen3.5-VL-9B (single quant from jc-builds) =====

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
        quantLabel = "Q4_K_M",
    )

    // ===== Public API =====

    /** All built-in models. LiteRT first, then GGUF variants. */
    val builtinModels: List<ModelInfo> = listOf(
        gemma4E2B,
        gemma4E4B,
        // 3B variants
        qwen3B_Q2_K, qwen3B_Q3_K_M, qwen3B_Q4_K_M, qwen3B_Q5_K_M, qwen3B_Q6_K, qwen3B_Q8_0,
        // 7B variants
        qwen7B_Q2_K, qwen7B_Q3_K_M, qwen7B_Q4_K_M, qwen7B_Q5_K_M, qwen7B_Q6_K, qwen7B_Q8_0,
        // 9B
        qwen35vl9b,
    )

    /** Model groups for expandable UI. */
    val modelGroups: List<ModelGroup> = listOf(
        ModelGroup(
            groupId = "qwen25vl-3b",
            displayName = "Qwen2.5-VL 3B",
            family = "qwen2.5-vl",
            runtime = ModelRuntime.LLAMA_CPP,
            description = "Лёгкая VLM модель. 6 вариантов квантизации.",
            supportsVision = true,
            variants = listOf(qwen3B_Q2_K, qwen3B_Q3_K_M, qwen3B_Q4_K_M, qwen3B_Q5_K_M, qwen3B_Q6_K, qwen3B_Q8_0),
        ),
        ModelGroup(
            groupId = "qwen25vl-7b",
            displayName = "Qwen2.5-VL 7B",
            family = "qwen2.5-vl",
            runtime = ModelRuntime.LLAMA_CPP,
            description = "Высокое качество. 6 вариантов квантизации.",
            supportsVision = true,
            variants = listOf(qwen7B_Q2_K, qwen7B_Q3_K_M, qwen7B_Q4_K_M, qwen7B_Q5_K_M, qwen7B_Q6_K, qwen7B_Q8_0),
        ),
    )

    /** Find a builtin model by ID. */
    fun findById(id: String): ModelInfo? = builtinModels.find { it.id == id }

    /** Group models by runtime for UI display. */
    fun groupedByRuntime(): Map<ModelRuntime, List<ModelInfo>> =
        builtinModels.groupBy { it.runtime }

    /** Get standalone models (not part of a group) for a given runtime. */
    fun standaloneModels(runtime: ModelRuntime): List<ModelInfo> =
        builtinModels.filter { it.runtime == runtime && it.groupId == null }

    /** Get groups for a given runtime. */
    fun groupsForRuntime(runtime: ModelRuntime): List<ModelGroup> =
        modelGroups.filter { it.runtime == runtime }

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

    /**
     * Match an imported file to its builtin model by filename.
     * Returns the ModelInfo if any builtin model contains a file with this exact name.
     * This enables re-importing exported models back to their correct registry slot.
     */
    fun findByFileName(fileName: String): ModelInfo? =
        builtinModels.find { model ->
            model.files.any { it.fileName == fileName }
        }
}
