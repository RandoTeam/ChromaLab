package com.chromalab.feature.processing.inference

/**
 * Per-model prompt format.
 *
 * Each VLM family expects a different prompt structure.
 * Using the wrong format causes degraded output or infinite generation.
 *
 * | Style           | Models                    | Format                                      |
 * |-----------------|---------------------------|---------------------------------------------|
 * | CHATML          | Qwen2.5/3/3.5-VL          | <|im_start|>system\n...<|im_end|>\n...       |
 * | TRIGGER         | PaddleOCR-VL              | "Chart Recognition:" (no roles)             |
 * | DEEPSEEK_OCR    | DeepSeek-OCR              | "<|grounding|>..." (special marker)          |
 * | DIRECT_QUESTION | Moondream2                | Plain question string, no roles             |
 * | RAW             | SmolVLM2, dots.mocr, etc.  | system\n\nuser (concatenated, no markers)   |
 * | LITERT          | Gemma 4                   | SDK manages internally                      |
 */
enum class PromptStyle {
    /** Qwen ChatML: <|im_start|>system/user/assistant markers. */
    CHATML,
    /** PaddleOCR-VL trigger phrases: "Chart Recognition:", "OCR:", etc. */
    TRIGGER,
    /** DeepSeek-OCR: <|grounding|> prefix marker. */
    DEEPSEEK_OCR,
    /** Moondream2: plain direct question, no system/user roles. */
    DIRECT_QUESTION,
    /** Generic: system + user concatenated with newlines. */
    RAW,
    /** LiteRT SDK manages template internally (Gemma 4). */
    LITERT,
}

/**
 * Inference configuration — per-model sampling, context, and prompt parameters.
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
 * ## Parameters tuned per model family
 *
 * | Model               | maxTokens | repeatPenalty | contextSize | promptStyle     |
 * |---------------------|-----------|---------------|-------------|-----------------|
 * | Qwen3-VL (2B/4B/8B) | 768       | 1.1           | 4096        | CHATML          |
 * | Qwen3.5-VL (9B)     | 768       | 1.1           | 4096        | CHATML          |
 * | PaddleOCR-VL 1.5    | 512       | 1.1           | 2048        | TRIGGER         |
 * | dots.mocr            | 1024      | 1.1           | 4096        | CHATML          |
 * | DeepSeek-OCR         | 2048      | 1.0           | 4096        | DEEPSEEK_OCR    |
 * | SmolVLM2             | 768       | 1.1           | 4096        | RAW             |
 * | Moondream2           | 768       | 1.1           | 4096        | DIRECT_QUESTION |
 * | LiteRT-LM VLM        | 768       | 1.0           | 4096        | LITERT          |
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
     * Prompt format this model expects.
     * Determines how system/user content is wrapped before sending to the engine.
     */
    val promptStyle: PromptStyle = PromptStyle.RAW,

    /**
     * Legacy convenience accessor. True when promptStyle == CHATML.
     */
    val useChatML: Boolean = promptStyle == PromptStyle.CHATML,
) {
    companion object {
        // ─── Qwen family ────────────────────────────────────────────

        /**
         * Preset for Qwen2.5-VL models (3B, 7B).
         *
         * Greedy sampling (handled in C++ via llama_sampler_init_greedy).
         * Repeat penalty 1.1 — prevents JSON value duplication.
         * ChatML template required for Instruct-tuned models.
         */
        val QWEN25_VL = InferenceConfig(
            maxTokens = 768,
            repeatPenalty = 1.1f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 512,
            promptStyle = PromptStyle.CHATML,
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
            promptStyle = PromptStyle.CHATML,
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
            promptStyle = PromptStyle.CHATML,
        )

        // ─── Gemma (LiteRT) ─────────────────────────────────────────

        /**
         * Preset for Gemma 4 models (E2B, E4B).
         *
         * LiteRT SDK manages sampling and chat template internally (greedy by default).
         * These params are stored for reference but not passed to native.
         */
        val GEMMA = InferenceConfig(
            maxTokens = 768,
            repeatPenalty = 1.0f, // LiteRT handles this
            repeatLastN = 0,
            contextSize = 4096,
            batchSize = 512,
            promptStyle = PromptStyle.LITERT,
        )

        /**
         * Preset for LiteRT-LM multimodal bundles that receive image/text via
         * the SDK Message/Content API. Do not wrap these prompts in ChatML text.
         */
        val LITERT_VLM = InferenceConfig(
            maxTokens = 768,
            repeatPenalty = 1.0f,
            repeatLastN = 0,
            contextSize = 4096,
            batchSize = 512,
            promptStyle = PromptStyle.LITERT,
        )

        // ─── Specialized OCR models ─────────────────────────────────

        /**
         * Preset for PaddleOCR-VL 1.5.
         *
         * CRITICAL: PaddleOCR-VL does NOT use ChatML.
         * It uses task-trigger prompts: "Chart Recognition:", "OCR:", "Table Recognition:"
         * The trigger phrase IS the entire prompt — no system/user roles needed.
         * Using ChatML causes degraded output or infinite generation.
         *
         * Reference: https://huggingface.co/PaddlePaddle/PaddleOCR-VL-1.5
         */
        val PADDLEOCR_VL = InferenceConfig(
            maxTokens = 512,
            repeatPenalty = 1.1f,
            repeatLastN = 64,
            contextSize = 2048,
            batchSize = 512,
            promptStyle = PromptStyle.TRIGGER,
        )

        /**
         * Preset for dots.mocr (1.8B).
         *
         * Qwen2 backbone — uses ChatML template.
         * Optimized for document→Markdown/SVG parsing.
         * Larger maxTokens for SVG code output.
         *
         * Reference: https://huggingface.co/reducto/dots.mocr
         */
        val DOTS_MOCR = InferenceConfig(
            maxTokens = 1024,
            repeatPenalty = 1.1f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 512,
            promptStyle = PromptStyle.CHATML,
        )

        /**
         * Preset for DeepSeek-OCR (3B MoE).
         *
         * CRITICAL: Uses special <|grounding|> prefix marker.
         * Does NOT use standard ChatML.
         * Recommended: temperature=0, maxTokens=8192.
         * We limit to 2048 for mobile — chromatogram OCR won't produce 8K tokens.
         *
         * Reference: https://huggingface.co/deepseek-ai/DeepSeek-OCR
         */
        val DEEPSEEK_OCR = InferenceConfig(
            maxTokens = 2048,
            repeatPenalty = 1.0f, // Official: no repeat penalty
            repeatLastN = 0,
            contextSize = 4096,
            batchSize = 512,
            promptStyle = PromptStyle.DEEPSEEK_OCR,
        )

        // ─── Lightweight VLMs ───────────────────────────────────────

        /**
         * Preset for SmolVLM2 (2.2B).
         *
         * Uses conversational format with system/user roles.
         * No ChatML markers — raw concatenation.
         * Good at general VQA, moderate OCR capability.
         *
         * Reference: https://huggingface.co/HuggingFaceTB/SmolVLM2-2.2B-Instruct
         */
        val SMOLVLM2 = InferenceConfig(
            maxTokens = 768,
            repeatPenalty = 1.1f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 512,
            promptStyle = PromptStyle.RAW,
        )

        /**
         * Preset for Moondream2.
         *
         * Uses direct question format — no system/user roles.
         * Just the question text, nothing else.
         * Supports "Transcribe the text" for OCR tasks.
         *
         * Reference: https://github.com/vikhyat/moondream
         */
        val MOONDREAM2 = InferenceConfig(
            maxTokens = 768,
            repeatPenalty = 1.1f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 512,
            promptStyle = PromptStyle.DIRECT_QUESTION,
        )

        /**
         * Conservative default for user-imported models.
         * Slightly stricter repeat penalty to handle unknown model behaviors.
         * Raw prompt format — safest for unknown architectures.
         */
        val DEFAULT = InferenceConfig(
            maxTokens = 512,
            repeatPenalty = 1.15f,
            repeatLastN = 64,
            contextSize = 4096,
            batchSize = 256,
            promptStyle = PromptStyle.RAW,
        )

        /**
         * Select the optimal config for a model by its family.
         */
        fun forModelFamily(family: String): InferenceConfig = when {
            family.contains("fastvlm-litert", ignoreCase = true) -> LITERT_VLM
            family.contains("litert-qwen", ignoreCase = true) -> LITERT_VLM
            family.contains("paddleocr", ignoreCase = true) -> PADDLEOCR_VL
            family.contains("dots", ignoreCase = true) -> DOTS_MOCR
            family.contains("deepseek-ocr", ignoreCase = true) -> DEEPSEEK_OCR
            family.contains("smolvlm", ignoreCase = true) -> SMOLVLM2
            family.contains("moondream", ignoreCase = true) -> MOONDREAM2
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
