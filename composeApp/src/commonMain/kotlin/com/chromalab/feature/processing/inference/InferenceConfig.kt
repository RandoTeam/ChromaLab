package com.chromalab.feature.processing.inference

/**
 * Inference configuration — per-model sampling and context parameters.
 *
 * All models use GREEDY sampling (temperature=0) for deterministic, factual output.
 * This is critical for chromatogram analysis where accuracy is paramount.
 *
 * Parameters tuned per model family:
 * - Qwen3.5-VL:  repeat_penalty=1.1, n_predict=512
 * - Gemma 4:      managed by LiteRT SDK (greedy by default)
 * - User-imported: conservative defaults
 */
data class InferenceConfig(
    /** Maximum tokens to generate. OCR/JSON extraction rarely exceeds 256. */
    val maxTokens: Int = 512,
    /**
     * Repetition penalty to prevent loops. 1.0 = disabled.
     * For structured JSON output, mild penalty (1.1) prevents degenerate repetitions
     * without distorting the numeric values.
     */
    val repeatPenalty: Float = 1.1f,
    /**
     * Number of last tokens to consider for repeat penalty.
     * For JSON output keep this smaller to avoid penalizing legitimate repeated digits.
     */
    val repeatLastN: Int = 64,
    /**
     * Context window size. Must accommodate image tokens + prompt + response.
     * Qwen3.5-VL: ~1800 image tokens + prompt ≈ 2000, response ≈ 256.
     * 4096 is safe for all supported models.
     */
    val contextSize: Int = 4096,
    /** Batch size for prompt processing. */
    val batchSize: Int = 512,
) {
    companion object {
        /**
         * Preset for Qwen3.5-VL models (all sizes: 0.8B, 2B, 4B, 9B).
         *
         * Greedy sampling (handled in C++ via llama_sampler_init_greedy).
         * Repeat penalty 1.1 — prevents JSON value duplication.
         * Max tokens 512 — sufficient for axis extraction JSON.
         */
        val QWEN_VL = InferenceConfig(
            maxTokens = 512,
            repeatPenalty = 1.1f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 512,
        )

        /**
         * Preset for Gemma 4 models (E2B, E4B).
         *
         * LiteRT SDK manages sampling internally (greedy by default).
         * These params are stored for reference but not passed to native.
         */
        val GEMMA = InferenceConfig(
            maxTokens = 512,
            repeatPenalty = 1.0f, // LiteRT handles this
            repeatLastN = 0,
            contextSize = 4096,
            batchSize = 512,
        )

        /**
         * Conservative default for user-imported models.
         * Slightly stricter repeat penalty to handle unknown model behaviors.
         */
        val DEFAULT = InferenceConfig(
            maxTokens = 384,
            repeatPenalty = 1.15f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 256,
        )

        /**
         * Select the optimal config for a model by its family.
         */
        fun forModelFamily(family: String): InferenceConfig = when {
            family.contains("qwen", ignoreCase = true) -> QWEN_VL
            family.contains("gemma", ignoreCase = true) -> GEMMA
            else -> DEFAULT
        }
    }
}
