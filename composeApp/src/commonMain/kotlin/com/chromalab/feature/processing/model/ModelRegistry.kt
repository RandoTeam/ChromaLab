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
    val chatContextLimit: Int
        get() = when {
            runtime == ModelRuntime.LITERT_LM -> 8192
            family.equals("qwen3.5-mtp", ignoreCase = true) -> 32768
            family.contains("gpt-oss", ignoreCase = true) -> 32768
            runtime == ModelRuntime.LLAMA_CPP -> 16384
            else -> 4096
        }
    val defaultChatContextSize: Int
        get() = when {
            family.equals("qwen3.5-mtp", ignoreCase = true) -> 4096
            runtime == ModelRuntime.LITERT_LM -> 4096
            else -> minOf(4096, chatContextLimit)
        }
    val kvCacheBytesPerToken: Long
        get() = when {
            id.contains("20b", ignoreCase = true) -> 1_024L * 1_024L
            id.contains("9b", ignoreCase = true) -> 512L * 1024L
            id.contains("8b", ignoreCase = true) -> 512L * 1024L
            id.contains("4b", ignoreCase = true) -> 256L * 1024L
            id.contains("2b", ignoreCase = true) -> 192L * 1024L
            runtime == ModelRuntime.LITERT_LM -> 192L * 1024L
            else -> 256L * 1024L
        }
    val runtimeOverheadBytes: Long
        get() = when (runtime) {
            ModelRuntime.LITERT_LM -> 512L * 1024L * 1024L
            ModelRuntime.LLAMA_CPP -> if (supportsMtp) 768L * 1024L * 1024L else 512L * 1024L * 1024L
        }
    val maxMtpDraftTokens: Int get() = if (supportsMtp) 6 else 0
    val defaultMtpDraftTokens: Int
        get() = when {
            !supportsMtp -> 0
            id.contains("27b", ignoreCase = true) -> 2
            else -> 3
        }
    val supportsMtp: Boolean
        get() {
            if (runtime != ModelRuntime.LLAMA_CPP) return false
            if (files.any { it.type == ModelFileType.GGUF_MMPROJ }) return false

            val supportSignal = listOf(
                id,
                displayName,
                family,
                files.firstOrNull()?.fileName.orEmpty(),
                quantLabel.orEmpty(),
                description,
            ).joinToString(" ").lowercase()

            return supportSignal.contains("mtp") ||
                supportSignal.contains("nextn") ||
                supportSignal.contains("next-n")
        }
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
 * URLs verified against HuggingFace API/range probes on 2026-05-18.
 *
 * Available models:
 *   LiteRT-LM:  Gemma 4 E2B (2.59 GB), Gemma 4 E4B (3.66 GB),
 *               FastVLM 0.5B (1.08 GB), Qwen3.5 0.8B VLM (1.08 GB)
 *   llama.cpp:  Qwen3-VL-2B (6 quants), Qwen3-VL-4B (6 quants),
 *               Qwen3-VL-8B (6 quants), Qwen3.5-VL-9B (Q4_K_M),
 *               Qwen3.5 MTP 4B/9B chat (all Unsloth GGUF quants),
 *               GPT-OSS 20B chat test (Q4_K_M),
 *               PaddleOCR-VL-1.5 (Q8_0, BF16),
 *               dots.mocr (Q5_K_M, Q8_0, BF16),
 *               DeepSeek-OCR (Q8_0),
 *               SmolVLM2-2.2B (Q4_K_M, Q8_0),
 *               Moondream2 (F16)
 */
object ModelRegistry {

    private const val HF_BASE = "https://huggingface.co"
    private const val LITERT_COMMUNITY = "$HF_BASE/litert-community"
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
        description = "Быстрая модель с GPU/CPU ускорением. Рекомендуется для большинства устройств.",
    )

    private val gemma4E4B = ModelInfo(
        id = "gemma4-e4b",
        displayName = "Gemma 4 E4B",
        family = "gemma-4",
        runtime = ModelRuntime.LITERT_LM,
        files = listOf(
            ModelFile(
                fileName = "gemma-4-E4B-it.litertlm",
                sizeBytes = 3_659_530_240L,
                type = ModelFileType.LITERT_BUNDLE,
                downloadUrl = "$HF_BASE/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            ),
        ),
        minRamMb = 8192,
        isBuiltin = true,
        supportsVision = true,
        description = "High-accuracy LiteRT-LM VLM. GPU/CPU capable. ~3.66 GB download; 8+ GB RAM recommended.",
    )

    private val fastVlm05B = ModelInfo(
        id = "fastvlm-05b-litert",
        displayName = "FastVLM 0.5B",
        family = "fastvlm-litert",
        runtime = ModelRuntime.LITERT_LM,
        files = listOf(
            ModelFile(
                fileName = "FastVLM-0.5B.litertlm",
                sizeBytes = 1_156_349_952L,
                type = ModelFileType.LITERT_BUNDLE,
                downloadUrl = "$LITERT_COMMUNITY/FastVLM-0.5B/resolve/main/FastVLM-0.5B.litertlm",
            ),
        ),
        minRamMb = 4096,
        isBuiltin = true,
        supportsVision = true,
        description = "Experimental non-Google LiteRT-LM VLM. Image input, GPU/CPU candidates. License: Apple AMLR.",
    )

    private val qwen35LiteRt08B = ModelInfo(
        id = "qwen35-08b-litert-vlm",
        displayName = "Qwen3.5 0.8B LiteRT VLM",
        family = "litert-qwen3.5-vlm",
        runtime = ModelRuntime.LITERT_LM,
        files = listOf(
            ModelFile(
                fileName = "qwen35_mm_q8_ekv2048.litertlm",
                sizeBytes = 1_159_757_824L,
                type = ModelFileType.LITERT_BUNDLE,
                downloadUrl = "$HF_BASE/GabrieleConte/Qwen3.5-0.8B-LiteRT/resolve/main/qwen35_mm_q8_ekv2048.litertlm",
            ),
        ),
        minRamMb = 4096,
        isBuiltin = true,
        supportsVision = true,
        description = "Experimental Apache-2.0 LiteRT-LM multimodal Qwen package with bundled vision encoder and adapter.",
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

    // ===== Qwen3.5 MTP chat models (Unsloth GGUF) =====
    // Source: unsloth/Qwen3.5-4B-MTP-GGUF and unsloth/Qwen3.5-9B-MTP-GGUF.
    // These upstream packages include vision files, but ChromaLab exposes them as
    // text-only chat models because in-app MTP is a GGUF text generation option.

    private fun qwen35MtpVariant(
        sizeLabel: String,
        quant: String,
        sizeBytes: Long,
        minRam: Int,
        desc: String,
    ) = ModelInfo(
        id = "qwen35-mtp-${sizeLabel.lowercase()}-${quant.lowercase().replace("_", "")}",
        displayName = "$sizeLabel MTP · $quant",
        family = "qwen3.5-mtp",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "Qwen3.5-MTP-$sizeLabel-$quant.gguf",
                sizeBytes = sizeBytes,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$UNSLOTH/Qwen3.5-$sizeLabel-MTP-GGUF/resolve/main/Qwen3.5-$sizeLabel-$quant.gguf",
            ),
        ),
        minRamMb = minRam,
        isBuiltin = true,
        supportsVision = false,
        description = desc,
        groupId = "qwen35-mtp-${sizeLabel.lowercase()}",
        quantLabel = quant,
    )

    private data class Qwen35MtpQuant(
        val quant: String,
        val sizeBytes: Long,
        val minRamMb: Int,
    )

    private fun qwen35MtpVariants(
        sizeLabel: String,
        specs: List<Qwen35MtpQuant>,
    ): List<ModelInfo> =
        specs.map { spec ->
            qwen35MtpVariant(
                sizeLabel = sizeLabel,
                quant = spec.quant,
                sizeBytes = spec.sizeBytes,
                minRam = spec.minRamMb,
                desc = "Unsloth Qwen3.5 $sizeLabel MTP GGUF chat model. Quant ${spec.quant}; text-only in ChromaLab.",
            )
        }

    private val qwen35Mtp4BVariants = qwen35MtpVariants(
        sizeLabel = "4B",
        specs = listOf(
            Qwen35MtpQuant("UD-IQ2_M", 1_942_548_800L, 4096),
            Qwen35MtpQuant("UD-IQ3_XXS", 2_103_521_600L, 4096),
            Qwen35MtpQuant("UD-Q2_K_XL", 2_121_677_120L, 4096),
            Qwen35MtpQuant("Q3_K_S", 2_177_505_600L, 4096),
            Qwen35MtpQuant("Q3_K_M", 2_374_564_160L, 4096),
            Qwen35MtpQuant("UD-Q3_K_XL", 2_528_860_480L, 4096),
            Qwen35MtpQuant("IQ4_XS", 2_642_012_480L, 6144),
            Qwen35MtpQuant("Q4_0", 2_669_209_920L, 6144),
            Qwen35MtpQuant("Q4_K_S", 2_683_300_160L, 6144),
            Qwen35MtpQuant("IQ4_NL", 2_729_175_360L, 6144),
            Qwen35MtpQuant("Q4_K_M", 2_834_975_040L, 6144),
            Qwen35MtpQuant("Q4_1", 2_877_122_880L, 6144),
            Qwen35MtpQuant("UD-Q4_K_XL", 2_990_664_000L, 6144),
            Qwen35MtpQuant("Q5_K_S", 3_124_357_440L, 6144),
            Qwen35MtpQuant("Q5_K_M", 3_212_790_080L, 6144),
            Qwen35MtpQuant("UD-Q5_K_XL", 3_304_827_200L, 8192),
            Qwen35MtpQuant("Q6_K", 3_639_654_720L, 8192),
            Qwen35MtpQuant("UD-Q6_K_XL", 4_261_908_800L, 8192),
            Qwen35MtpQuant("Q8_0", 4_610_580_800L, 10240),
            Qwen35MtpQuant("UD-Q8_K_XL", 6_065_971_520L, 12288),
            Qwen35MtpQuant("BF16", 8_665_620_544L, 16384),
        ),
    )

    private val qwen35Mtp9BVariants = qwen35MtpVariants(
        sizeLabel = "9B",
        specs = listOf(
            Qwen35MtpQuant("UD-IQ2_M", 3_969_560_928L, 8192),
            Qwen35MtpQuant("UD-IQ3_XXS", 4_295_536_992L, 8192),
            Qwen35MtpQuant("UD-Q2_K_XL", 4_440_797_536L, 8192),
            Qwen35MtpQuant("Q3_K_S", 4_461_195_616L, 8192),
            Qwen35MtpQuant("Q3_K_M", 4_834_783_584L, 10240),
            Qwen35MtpQuant("UD-Q3_K_XL", 5_241_319_776L, 10240),
            Qwen35MtpQuant("IQ4_XS", 5_467_189_600L, 10240),
            Qwen35MtpQuant("Q4_0", 5_551_599_968L, 12288),
            Qwen35MtpQuant("Q4_K_S", 5_577_290_080L, 12288),
            Qwen35MtpQuant("IQ4_NL", 5_644_398_944L, 12288),
            Qwen35MtpQuant("Q4_K_M", 5_868_826_976L, 12288),
            Qwen35MtpQuant("Q4_1", 6_022_541_664L, 12288),
            Qwen35MtpQuant("UD-Q4_K_XL", 6_135_034_208L, 12288),
            Qwen35MtpQuant("Q5_K_S", 6_559_543_648L, 12288),
            Qwen35MtpQuant("Q5_K_M", 6_729_445_728L, 12288),
            Qwen35MtpQuant("UD-Q5_K_XL", 6_874_345_824L, 12288),
            Qwen35MtpQuant("Q6_K", 7_684_551_008L, 14336),
            Qwen35MtpQuant("UD-Q6_K_XL", 8_987_439_456L, 16384),
            Qwen35MtpQuant("Q8_0", 9_786_061_152L, 16384),
            Qwen35MtpQuant("UD-Q8_K_XL", 13_245_182_304L, 24576),
            Qwen35MtpQuant("BF16", 18_407_321_728L, 32768),
        ),
    )

    // ===== GPT-OSS 20B (Unsloth GGUF, chat test) =====

    private val gptOss20B_Q4_K_M = ModelInfo(
        id = "gpt-oss-20b-q4km",
        displayName = "GPT-OSS 20B · Q4_K_M",
        family = "gpt-oss",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "gpt-oss-20b-Q4_K_M.gguf",
                sizeBytes = 11_624_759_488L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$UNSLOTH/gpt-oss-20b-GGUF/resolve/main/gpt-oss-20b-Q4_K_M.gguf",
            ),
        ),
        minRamMb = 16384,
        isBuiltin = true,
        supportsVision = false,
        description = "Experimental Unsloth GPT-OSS 20B GGUF chat model. Requires the model-native chat template; 16+ GB RAM recommended.",
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

    // ===== dots.mocr (document/chart→structure parser) =====
    // Source: lodrick-the-lafted/dots.mocr-gguf (llama.cpp b8731+)

    private val mmprojDotsMocr = ModelFile(
        fileName = "mmproj-dotsmocr-q8_0.gguf",
        sizeBytes = 1_344_068_544L,
        type = ModelFileType.GGUF_MMPROJ,
        downloadUrl = "$HF_BASE/lodrick-the-lafted/dots.mocr-gguf/resolve/main/mmproj-dotsmocr-q8_0.gguf",
    )

    private fun dotsMocrVariant(quant: String, sizeBytes: Long, minRam: Int, desc: String) = ModelInfo(
        id = "dotsmocr-${quant.lowercase().replace("_", "")}",
        displayName = "1.8B · $quant",
        family = "dots-mocr",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "dotsmocr-1.8b-${quant.lowercase().replace("_", "_")}.gguf",
                sizeBytes = sizeBytes,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$HF_BASE/lodrick-the-lafted/dots.mocr-gguf/resolve/main/dotsmocr-1.8b-${quant.lowercase()}.gguf",
            ),
            mmprojDotsMocr,
        ),
        minRamMb = minRam,
        isBuiltin = true,
        supportsVision = true,
        description = desc,
        groupId = "dots-mocr",
        quantLabel = quant,
    )

    private val dotsMocr_Q5_K_M = dotsMocrVariant("q5_k_m", 1_285_492_512L, 3072, "Оптимальный баланс. ~1.2 GB.")
    private val dotsMocr_Q8_0   = dotsMocrVariant("q8_0",   1_894_530_336L, 4096, "Высокое качество. ~1.8 GB.")
    private val dotsMocr_BF16   = dotsMocrVariant("bf16",   3_560_414_496L, 6144, "Максимальное качество. ~3.4 GB.")

    // ===== DeepSeek-OCR (document OCR, 3B MoE) =====
    // Source: ggml-org/DeepSeek-OCR-GGUF (14K downloads)

    private val deepSeekOCR = ModelInfo(
        id = "deepseek-ocr-q80",
        displayName = "DeepSeek-OCR",
        family = "deepseek-ocr",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "DeepSeek-OCR-Q8_0.gguf",
                sizeBytes = 3_126_139_712L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$GGML_ORG/DeepSeek-OCR-GGUF/resolve/main/DeepSeek-OCR-Q8_0.gguf",
            ),
            ModelFile(
                fileName = "mmproj-DeepSeek-OCR-Q8_0.gguf",
                sizeBytes = 447_856_768L,
                type = ModelFileType.GGUF_MMPROJ,
                downloadUrl = "$GGML_ORG/DeepSeek-OCR-GGUF/resolve/main/mmproj-DeepSeek-OCR-Q8_0.gguf",
            ),
        ),
        minRamMb = 6144,
        isBuiltin = true,
        supportsVision = true,
        description = "OCR-модель для документов. ~3.0 GB + 427 MB mmproj. Q8_0.",
        quantLabel = "Q8_0",
    )

    // ===== SmolVLM2-2.2B (lightweight multimodal) =====
    // Source: ggml-org/SmolVLM2-2.2B-Instruct-GGUF (official)

    private val mmprojSmolVLM2 = ModelFile(
        fileName = "mmproj-SmolVLM2-2.2B-Instruct-Q8_0.gguf",
        sizeBytes = 592_523_200L,
        type = ModelFileType.GGUF_MMPROJ,
        downloadUrl = "$GGML_ORG/SmolVLM2-2.2B-Instruct-GGUF/resolve/main/mmproj-SmolVLM2-2.2B-Instruct-Q8_0.gguf",
    )

    private fun smolVLM2Variant(quant: String, sizeBytes: Long, minRam: Int, desc: String) = ModelInfo(
        id = "smolvlm2-${quant.lowercase().replace("_", "")}",
        displayName = "2.2B · $quant",
        family = "smolvlm2",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "SmolVLM2-2.2B-Instruct-$quant.gguf",
                sizeBytes = sizeBytes,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$GGML_ORG/SmolVLM2-2.2B-Instruct-GGUF/resolve/main/SmolVLM2-2.2B-Instruct-$quant.gguf",
            ),
            mmprojSmolVLM2,
        ),
        minRamMb = minRam,
        isBuiltin = true,
        supportsVision = true,
        description = desc,
        groupId = "smolvlm2",
        quantLabel = quant,
    )

    private val smolVLM2_Q4_K_M = smolVLM2Variant("Q4_K_M", 1_112_602_656L, 3072, "Оптимальный баланс. ~1.1 GB.")
    private val smolVLM2_Q8_0   = smolVLM2Variant("Q8_0",   1_927_933_984L, 4096, "Высокое качество. ~1.8 GB.")

    // ===== Moondream2 (fast tiny VLM) =====
    // Source: ggml-org/moondream2-20250414-GGUF (30K downloads)

    private val moondream2 = ModelInfo(
        id = "moondream2-f16",
        displayName = "Moondream2",
        family = "moondream",
        runtime = ModelRuntime.LLAMA_CPP,
        files = listOf(
            ModelFile(
                fileName = "moondream2-text-model-f16_ct-vicuna.gguf",
                sizeBytes = 2_839_535_072L,
                type = ModelFileType.GGUF_BASE,
                downloadUrl = "$GGML_ORG/moondream2-20250414-GGUF/resolve/main/moondream2-text-model-f16_ct-vicuna.gguf",
            ),
            ModelFile(
                fileName = "moondream2-mmproj-f16.gguf",
                sizeBytes = 909_777_984L,
                type = ModelFileType.GGUF_MMPROJ,
                downloadUrl = "$GGML_ORG/moondream2-20250414-GGUF/resolve/main/moondream2-mmproj-f16-20250414.gguf",
            ),
        ),
        minRamMb = 4096,
        isBuiltin = true,
        supportsVision = true,
        description = "Быстрая лёгкая VLM. ~2.7 GB + 868 MB mmproj. F16.",
        quantLabel = "F16",
    )

    // ===== Public API =====

    /** All built-in models. LiteRT first, then GGUF variants. */
    val builtinModels: List<ModelInfo> = listOf(
        gemma4E2B,
        gemma4E4B,
        fastVlm05B,
        qwen35LiteRt08B,
        // PaddleOCR-VL (specialized OCR)
        paddleOCR_Q8_0, paddleOCR_BF16,
        // dots.mocr (chart→structure)
        dotsMocr_Q5_K_M, dotsMocr_Q8_0, dotsMocr_BF16,
        // DeepSeek-OCR
        deepSeekOCR,
        // SmolVLM2
        smolVLM2_Q4_K_M, smolVLM2_Q8_0,
        // Moondream2
        moondream2,
        // Qwen3-VL 2B variants
        qwen3vl2B_Q2_K, qwen3vl2B_Q3_K_M, qwen3vl2B_Q4_K_M, qwen3vl2B_Q5_K_M, qwen3vl2B_Q6_K, qwen3vl2B_Q8_0,
        // Qwen3-VL 4B variants
        qwen3vl4B_Q2_K, qwen3vl4B_Q3_K_M, qwen3vl4B_Q4_K_M, qwen3vl4B_Q5_K_M, qwen3vl4B_Q6_K, qwen3vl4B_Q8_0,
        // Qwen3-VL 8B variants
        qwen3vl8B_Q2_K, qwen3vl8B_Q3_K_M, qwen3vl8B_Q4_K_M, qwen3vl8B_Q5_K_M, qwen3vl8B_Q6_K, qwen3vl8B_Q8_0,
        // Qwen3.5-VL 9B
        qwen35vl9b,
    ) +
        qwen35Mtp4BVariants +
        qwen35Mtp9BVariants +
        listOf(
            // GPT-OSS chat test
            gptOss20B_Q4_K_M,
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
            groupId = "qwen35-mtp-4b",
            displayName = "Qwen3.5 MTP 4B",
            family = "qwen3.5-mtp",
            runtime = ModelRuntime.LLAMA_CPP,
            description = "Unsloth GGUF MTP chat model. Text-only in ChromaLab.",
            supportsVision = false,
            variants = qwen35Mtp4BVariants,
        ),
        ModelGroup(
            groupId = "qwen35-mtp-9b",
            displayName = "Qwen3.5 MTP 9B",
            family = "qwen3.5-mtp",
            runtime = ModelRuntime.LLAMA_CPP,
            description = "Higher-quality Unsloth GGUF MTP chat model for high-end devices.",
            supportsVision = false,
            variants = qwen35Mtp9BVariants,
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
        ModelGroup(
            groupId = "dots-mocr",
            displayName = "dots.mocr",
            family = "dots-mocr",
            runtime = ModelRuntime.LLAMA_CPP,
            description = "Chart→Markdown/SVG парсер. 1.8B.",
            supportsVision = true,
            variants = listOf(dotsMocr_Q5_K_M, dotsMocr_Q8_0, dotsMocr_BF16),
        ),
        ModelGroup(
            groupId = "smolvlm2",
            displayName = "SmolVLM2 2.2B",
            family = "smolvlm2",
            runtime = ModelRuntime.LLAMA_CPP,
            description = "Лёгкая мультимодальная модель. 2 кванта.",
            supportsVision = true,
            variants = listOf(smolVLM2_Q4_K_M, smolVLM2_Q8_0),
        ),
    )

    /** Find a builtin model by ID. */
    fun findById(id: String): ModelInfo? = builtinModels.find { it.id == id }

    /** Models that are intended for chromatogram/photo analysis, ordered by analysis priority. */
    fun chromatogramVisionModels(): List<ModelInfo> =
        builtinModels
            .filter(::isChromatogramVisionModel)
            .sortedWith(compareBy<ModelInfo> { chromatogramVisionPriority(it) }.thenBy { it.totalSizeBytes })

    /** General chat/download list. Vision-only OCR models are excluded from default chat choices. */
    fun chatModels(): List<ModelInfo> =
        builtinModels.filter(::isChatModel)

    fun isChatModelId(modelId: String?): Boolean {
        if (modelId == null) return false
        val model = findById(modelId) ?: return true
        return isChatModel(model)
    }

    fun isChatModel(model: ModelInfo): Boolean {
        val family = model.family.lowercase()
        if (family in nonChatFamilies) return false
        return model.runtime == ModelRuntime.LITERT_LM || model.runtime == ModelRuntime.LLAMA_CPP
    }

    fun isChromatogramVisionModel(model: ModelInfo): Boolean {
        return ModelAssistedAnalysisContract.evaluateChromatogramVisionEligibility(model).eligible
    }

    fun hasGgufVisionFilePair(model: ModelInfo): Boolean =
        model.runtime == ModelRuntime.LLAMA_CPP &&
            model.files.any { it.type == ModelFileType.GGUF_BASE } &&
            model.files.any { it.type == ModelFileType.GGUF_MMPROJ }

    fun chromatogramVisionPriority(model: ModelInfo): Int {
        val family = model.family.lowercase()
        val id = model.id.lowercase()
        return when {
            id == "gemma4-e2b" -> 0
            family.contains("fastvlm") -> 1
            id == "qwen35-08b-litert-vlm" -> 2
            family.contains("qwen3-vl") && id.contains("2b") && id.contains("q3") -> 3
            family.contains("qwen3-vl") && id.contains("2b") && id.contains("q4") -> 4
            family.contains("smolvlm") && id.contains("q4") -> 5
            family.contains("moondream") -> 6
            id == "gemma4-e4b" -> 7
            family.contains("qwen") -> 8
            else -> 100
        }
    }

    private val nonChatFamilies = setOf(
        "paddleocr-vl",
        "dots-mocr",
        "deepseek-ocr",
    )

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
