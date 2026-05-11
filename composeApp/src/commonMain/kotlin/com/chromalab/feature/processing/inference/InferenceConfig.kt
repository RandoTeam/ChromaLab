package com.chromalab.feature.processing.inference

/**
 * Inference configuration — per-model sampling and context parameters.
 *
 * ## Sampling Strategy
 *
 * All models use **GREEDY sampling (temperature=0)** for deterministic, factual output.
 * This is enforced at the C++ sampler level (llama_sampler_init_greedy).
 *
 * For chromatogram OCR, ANY temperature > 0 introduces randomness into numeric values,
 * making the output non-reproducible. Greedy is the only correct choice.
 *
 * ## Repeat Penalty
 *
 * Mild repeat penalty (1.1) prevents degenerate loops in JSON output
 * (e.g. model repeating the same number array indefinitely).
 * Values > 1.2 risk distorting legitimate repeated numbers (e.g. [0, 0, 100, 200]).
 *
 * ## Token Budget
 *
 * maxTokens is sized for the expected output format:
 * - Axis extraction JSON: ~100-300 tokens (depending on number of tick labels)
 * - Graph region JSON: ~30 tokens
 * - Axis structure JSON: ~30 tokens
 *
 * We set generous limits (768) to accommodate charts with many labels
 * and models that emit thinking tokens or preamble before JSON.
 *
 * ## Parameters tuned per model family
 *
 * | Model               | maxTokens | repeatPenalty | repeatLastN | contextSize |
 * |---------------------|-----------|---------------|-------------|-------------|
 * | Qwen3-VL (2B/4B/8B) | 768       | 1.1           | 64          | 4096        |
 * | Qwen3.5-VL (9B)     | 768       | 1.1           | 64          | 4096        |
 * | Gemma 4 (LiteRT)    | 768       | 1.0 (SDK)     | 0           | 4096        |
 * | User-imported        | 512       | 1.15          | 64          | 4096        |
 */
data class InferenceConfig(
    /** Maximum tokens to generate. */
    val maxTokens: Int = 768,
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
     * Qwen2.5-VL: ~1800 image tokens + ~200 prompt tokens + ~300 response ≈ 2300.
     * 4096 is safe for all supported models.
     */
    val contextSize: Int = 4096,
    /** Batch size for prompt processing. */
    val batchSize: Int = 512,
    /**
     * Whether the model expects ChatML template wrapping.
     * True for Qwen VL Instruct models, false for Gemma (LiteRT handles internally).
     */
    val useChatML: Boolean = true,
) {
    companion object {
        /**
         * Preset for Qwen2.5-VL models (3B, 7B).
         *
         * Greedy sampling (handled in C++ via llama_sampler_init_greedy).
         * Repeat penalty 1.1 — prevents JSON value duplication.
         * Max tokens 768 — sufficient for complex charts with many labels.
         * ChatML template required for Instruct-tuned models.
         */
        val QWEN25_VL = InferenceConfig(
            maxTokens = 768,
            repeatPenalty = 1.1f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 512,
            useChatML = true,
        )

        /**
         * Preset for Qwen3-VL models (2B, 4B, 8B).
         *
         * Same ChatML template as Qwen2.5-VL.
         * Qwen3 may emit <think>...</think> blocks — parser strips them.
         */
        val QWEN3_VL = InferenceConfig(
            maxTokens = 768,
            repeatPenalty = 1.1f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 512,
            useChatML = true,
        )

        /**
         * Preset for Qwen3.5-VL models (9B).
         *
         * Same as Qwen3-VL — both use ChatML template.
         * Qwen3.5 may emit <think>...</think> blocks — parser strips them.
         */
        val QWEN35_VL = InferenceConfig(
            maxTokens = 768,
            repeatPenalty = 1.1f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 512,
            useChatML = true,
        )

        /**
         * Preset for Gemma 4 models (E2B, E4B).
         *
         * LiteRT SDK manages sampling and chat template internally (greedy by default).
         * These params are stored for reference but not passed to native.
         * useChatML = false — Gemma uses its own format via LiteRT SDK.
         */
        val GEMMA = InferenceConfig(
            maxTokens = 768,
            repeatPenalty = 1.0f, // LiteRT handles this
            repeatLastN = 0,
            contextSize = 4096,
            batchSize = 512,
            useChatML = false,
        )

        /**
         * Conservative default for user-imported models.
         * Slightly stricter repeat penalty to handle unknown model behaviors.
         * No ChatML — unknown template format.
         */
        val DEFAULT = InferenceConfig(
            maxTokens = 512,
            repeatPenalty = 1.15f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 256,
            useChatML = false,
        )

        /**
         * Select the optimal config for a model by its family.
         */
        fun forModelFamily(family: String): InferenceConfig = when {
            family.contains("qwen3.5", ignoreCase = true) -> QWEN35_VL
            family.contains("qwen3", ignoreCase = true) -> QWEN3_VL
            family.contains("qwen2", ignoreCase = true) -> QWEN25_VL
            family.contains("qwen", ignoreCase = true) -> QWEN3_VL
            family.contains("gemma", ignoreCase = true) -> GEMMA
            family.contains("llava", ignoreCase = true) -> DEFAULT
            else -> DEFAULT
        }
    }
}
