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
 *   llama.cpp:  Qwen3-VL-2B (6 quants), Qwen3-VL-4B (6 quants),
 *               Qwen3-VL-8B (6 quants), Qwen3.5-VL-9B (Q4_K_M)
 */
object ModelRegistry {

    private const val HF_BASE = "https://huggingface.co"
    private const val UNSLOTH = "$HF_BASE/unsloth"
    private const val GGML_ORG = "$HF_BASE/ggml-org"

    // ===== Shared mmproj files per Qwen3-VL model size =====
    // Source: unsloth/Qwen3-VL-*-Instruct-GGUF (BF16 quality)

    private val mmproj2B = ModelFile(
        fileName = "mmproj-Qwen3-VL-2B-BF16.gguf",
        sizeBytes = 822_540_960L,
        type = ModelFileType.GGUF_MMPROJ,
        downloadUrl = "$UNSLOTH/Qwen3-VL-2B-Instruct-GGUF/resolve/main/mmproj-BF16.gguf",
    )

    private val mmproj4B = ModelFile(
        fileName = "mmproj-Qwen3-VL-4B-BF16.gguf",
        sizeBytes = 839_326_368L,
        type = ModelFileType.GGUF_MMPROJ,
        downloadUrl = "$UNSLOTH/Qwen3-VL-4B-Instruct-GGUF/resolve/main/mmproj-BF16.gguf",
    )

    private val mmproj8B = ModelFile(
        fileName = "mmproj-Qwen3-VL-8B-BF16.gguf",
        sizeBytes = 1_162_569_280L,
        type = ModelFileType.GGUF_MMPROJ,
        downloadUrl = "$UNSLOTH/Qwen3-VL-8B-Instruct-GGUF/resolve/main/mmproj-BF16.gguf",
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

    // ===== Qwen3-VL-2B quantization variants =====
    // Source: unsloth/Qwen3-VL-2B-Instruct-GGUF (32K downloads)

    private fun qwen3vl2BVariant(quant: String, sizeBytes: Long, minRam: Int, desc: String) = ModelInfo(
        id = "qwen3vl-2b-${quant.lowercase().replace("_", "")}",
        displayName = "2B · $quant",
        family = "qwen3-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "Qwen3-VL-2B-Instruct-$quant.gguf",
                sizeBytes = sizeBytes,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$UNSLOTH/Qwen3-VL-2B-Instruct-GGUF/resolve/main/Qwen3-VL-2B-Instruct-$quant.gguf",
            ),
            mmproj2B,
        ),
        minRamMb = minRam,
        isBuiltin = true,
        supportsVision = true,
        description = desc,
        groupId = "qwen3vl-2b",
        quantLabel = quant,
    )

    private val qwen3vl2B_Q2_K   = qwen3vl2BVariant("Q2_K",     777_797_312L, 2048, "Минимальный. ~742 MB. Низкое качество.")
    private val qwen3vl2B_Q3_K_M = qwen3vl2BVariant("Q3_K_M",   939_540_160L, 3072, "Лёгкий. ~896 MB. Приемлемое качество.")
    private val qwen3vl2B_Q4_K_M = qwen3vl2BVariant("Q4_K_M", 1_107_410_624L, 3072, "Оптимальный баланс. ~1.1 GB.")
    private val qwen3vl2B_Q5_K_M = qwen3vl2BVariant("Q5_K_M", 1_257_881_280L, 4096, "Высокое качество. ~1.2 GB.")
    private val qwen3vl2B_Q6_K   = qwen3vl2BVariant("Q6_K",   1_417_756_352L, 4096, "Очень высокое качество. ~1.4 GB.")
    private val qwen3vl2B_Q8_0   = qwen3vl2BVariant("Q8_0",   1_834_428_096L, 6144, "Максимальное качество. ~1.7 GB.")

    // ===== Qwen3-VL-4B quantization variants =====
    // Source: unsloth/Qwen3-VL-4B-Instruct-GGUF (107K downloads)

    private fun qwen3vl4BVariant(quant: String, sizeBytes: Long, minRam: Int, desc: String) = ModelInfo(
        id = "qwen3vl-4b-${quant.lowercase().replace("_", "")}",
        displayName = "4B · $quant",
        family = "qwen3-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "Qwen3-VL-4B-Instruct-$quant.gguf",
                sizeBytes = sizeBytes,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$UNSLOTH/Qwen3-VL-4B-Instruct-GGUF/resolve/main/Qwen3-VL-4B-Instruct-$quant.gguf",
            ),
            mmproj4B,
        ),
        minRamMb = minRam,
        isBuiltin = true,
        supportsVision = true,
        description = desc,
        groupId = "qwen3vl-4b",
        quantLabel = quant,
    )

    private val qwen3vl4B_Q2_K   = qwen3vl4BVariant("Q2_K",   1_669_501_216L, 4096,  "Минимальный. ~1.6 GB. Низкое качество.")
    private val qwen3vl4B_Q3_K_M = qwen3vl4BVariant("Q3_K_M", 2_075_619_616L, 4096,  "Лёгкий. ~2.0 GB. Приемлемое качество.")
    private val qwen3vl4B_Q4_K_M = qwen3vl4BVariant("Q4_K_M", 2_497_282_336L, 6144,  "Оптимальный баланс. ~2.4 GB.")
    private val qwen3vl4B_Q5_K_M = qwen3vl4BVariant("Q5_K_M", 2_889_515_296L, 6144,  "Высокое качество. ~2.8 GB.")
    private val qwen3vl4B_Q6_K   = qwen3vl4BVariant("Q6_K",   3_306_262_816L, 8192,  "Очень высокое качество. ~3.2 GB.")
    private val qwen3vl4B_Q8_0   = qwen3vl4BVariant("Q8_0",   4_280_406_816L, 8192,  "Максимальное качество. ~4.1 GB.")

    // ===== Qwen3-VL-8B quantization variants =====
    // Source: unsloth/Qwen3-VL-8B-Instruct-GGUF (50K downloads)

    private fun qwen3vl8BVariant(quant: String, sizeBytes: Long, minRam: Int, desc: String) = ModelInfo(
        id = "qwen3vl-8b-${quant.lowercase().replace("_", "")}",
        displayName = "8B · $quant",
        family = "qwen3-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "Qwen3-VL-8B-Instruct-$quant.gguf",
                sizeBytes = sizeBytes,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$UNSLOTH/Qwen3-VL-8B-Instruct-GGUF/resolve/main/Qwen3-VL-8B-Instruct-$quant.gguf",
            ),
            mmproj8B,
        ),
        minRamMb = minRam,
        isBuiltin = true,
        supportsVision = true,
        description = desc,
        groupId = "qwen3vl-8b",
        quantLabel = quant,
    )

    private val qwen3vl8B_Q2_K   = qwen3vl8BVariant("Q2_K",   3_281_734_496L,  6144, "Минимальный. ~3.1 GB. Низкое качество.")
    private val qwen3vl8B_Q3_K_M = qwen3vl8BVariant("Q3_K_M", 4_124_162_912L,  8192, "Лёгкий. ~3.9 GB. Приемлемое качество.")
    private val qwen3vl8B_Q4_K_M = qwen3vl8BVariant("Q4_K_M", 5_027_785_568L,  8192, "Оптимальный баланс. ~4.8 GB.")
    private val qwen3vl8B_Q5_K_M = qwen3vl8BVariant("Q5_K_M", 5_851_114_336L, 10240, "Высокое качество. ~5.6 GB.")
    private val qwen3vl8B_Q6_K   = qwen3vl8BVariant("Q6_K",   6_725_901_152L, 10240, "Очень высокое качество. ~6.4 GB.")
    private val qwen3vl8B_Q8_0   = qwen3vl8BVariant("Q8_0",   8_709_520_224L, 12288, "Максимальное качество. ~8.3 GB.")

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
                fileName = "mmproj-Qwen35-9B-F16.gguf",
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

    // ===== PaddleOCR-VL-1.5 (specialized OCR model) =====
    // Official: PaddlePaddle/PaddleOCR-VL-1.5-GGUF (1M+ downloads)
    // Community quants: noctrex/PaddleOCR-VL-1.5-GGUF

    private val mmprojPaddleOCR = ModelFile(
        fileName = "mmproj-PaddleOCR-VL-BF16.gguf",
        sizeBytes = 881_770_496L,
        type = ModelFileType.GGUF_MMPROJ,
        downloadUrl = "$HF_BASE/PaddlePaddle/PaddleOCR-VL-1.5-GGUF/resolve/main/PaddleOCR-VL-1.5-mmproj.gguf",
    )

    private val paddleOCR_Q8_0 = ModelInfo(
        id = "paddleocr-vl-q80",
        displayName = "0.9B · Q8_0",
        family = "paddleocr-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "PaddleOCR-VL-1.5-Q8_0.gguf",
                sizeBytes = 498_316_224L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$HF_BASE/noctrex/PaddleOCR-VL-1.5-GGUF/resolve/main/PaddleOCR-VL-1.5-Q8_0.gguf",
            ),
            mmprojPaddleOCR,
        ),
        minRamMb = 2048,
        isBuiltin = true,
        supportsVision = true,
        description = "Высокое качество OCR. ~475 MB + 841 MB mmproj.",
        groupId = "paddleocr-vl",
        quantLabel = "Q8_0",
    )

    private val paddleOCR_BF16 = ModelInfo(
        id = "paddleocr-vl-bf16",
        displayName = "0.9B · BF16",
        family = "paddleocr-vl",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "PaddleOCR-VL-1.5-BF16.gguf",
                sizeBytes = 935_769_024L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$HF_BASE/noctrex/PaddleOCR-VL-1.5-GGUF/resolve/main/PaddleOCR-VL-1.5-BF16.gguf",
            ),
            mmprojPaddleOCR,
        ),
        minRamMb = 2048,
        isBuiltin = true,
        supportsVision = true,
        description = "Максимальное качество OCR. ~892 MB + 841 MB mmproj.",
        groupId = "paddleocr-vl",
        quantLabel = "BF16",
    )

    // ===== Public API =====

    /** All built-in models. LiteRT first, then GGUF variants. */
    val builtinModels: List<ModelInfo> = listOf(
        gemma4E2B,
        gemma4E4B,
        // PaddleOCR-VL (specialized OCR)
        paddleOCR_Q8_0, paddleOCR_BF16,
        // Qwen3-VL 2B variants
        qwen3vl2B_Q2_K, qwen3vl2B_Q3_K_M, qwen3vl2B_Q4_K_M, qwen3vl2B_Q5_K_M, qwen3vl2B_Q6_K, qwen3vl2B_Q8_0,
        // Qwen3-VL 4B variants
        qwen3vl4B_Q2_K, qwen3vl4B_Q3_K_M, qwen3vl4B_Q4_K_M, qwen3vl4B_Q5_K_M, qwen3vl4B_Q6_K, qwen3vl4B_Q8_0,
        // Qwen3-VL 8B variants
        qwen3vl8B_Q2_K, qwen3vl8B_Q3_K_M, qwen3vl8B_Q4_K_M, qwen3vl8B_Q5_K_M, qwen3vl8B_Q6_K, qwen3vl8B_Q8_0,
        // Qwen3.5-VL 9B
        qwen35vl9b,
    )

    /** Model groups for expandable UI. */
    val modelGroups: List<ModelGroup> = listOf(
        ModelGroup(
            groupId = "qwen3vl-2b",
            displayName = "Qwen3-VL 2B",
            family = "qwen3-vl",
            runtime = ModelRuntime.LLAMA_CPP,
            description = "Лёгкая VLM модель нового поколения. 6 квантов.",
            supportsVision = true,
            variants = listOf(qwen3vl2B_Q2_K, qwen3vl2B_Q3_K_M, qwen3vl2B_Q4_K_M, qwen3vl2B_Q5_K_M, qwen3vl2B_Q6_K, qwen3vl2B_Q8_0),
        ),
        ModelGroup(
            groupId = "qwen3vl-4b",
            displayName = "Qwen3-VL 4B",
            family = "qwen3-vl",
            runtime = ModelRuntime.LLAMA_CPP,
            description = "Оптимальный баланс точности и скорости. 6 квантов.",
            supportsVision = true,
            variants = listOf(qwen3vl4B_Q2_K, qwen3vl4B_Q3_K_M, qwen3vl4B_Q4_K_M, qwen3vl4B_Q5_K_M, qwen3vl4B_Q6_K, qwen3vl4B_Q8_0),
        ),
        ModelGroup(
            groupId = "qwen3vl-8b",
            displayName = "Qwen3-VL 8B",
            family = "qwen3-vl",
            runtime = ModelRuntime.LLAMA_CPP,
            description = "Высокое качество. 8+ GB RAM. 6 квантов.",
            supportsVision = true,
            variants = listOf(qwen3vl8B_Q2_K, qwen3vl8B_Q3_K_M, qwen3vl8B_Q4_K_M, qwen3vl8B_Q5_K_M, qwen3vl8B_Q6_K, qwen3vl8B_Q8_0),
        ),
        ModelGroup(
            groupId = "paddleocr-vl",
            displayName = "PaddleOCR-VL 1.5",
            family = "paddleocr-vl",
            runtime = ModelRuntime.LLAMA_CPP,
            description = "OCR-специалист для документов и графиков. 0.9B.",
            supportsVision = true,
            variants = listOf(paddleOCR_Q8_0, paddleOCR_BF16),
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
