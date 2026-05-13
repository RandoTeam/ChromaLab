package com.chromalab.feature.processing.inference

/**
 * Contract-only shape for the next GGUF runtime.
 *
 * This file intentionally does not replace [InferenceEngine]. It defines the
 * target API that a future llama.cpp implementation must satisfy before GGUF is
 * used for strict chromatogram reports.
 */
interface GgufRuntimeV2 {
    suspend fun loadTextModel(
        model: GgufModelSpec,
        backend: GgufBackendSpec = GgufBackendSpec(),
    ): LoadedGgufRuntime

    suspend fun loadVisionModel(
        model: GgufModelSpec,
        projector: GgufVisionProjectorSpec,
        backend: GgufBackendSpec = GgufBackendSpec(),
    ): LoadedGgufRuntime
}

interface LoadedGgufRuntime {
    val capabilities: RuntimeCapabilities

    suspend fun complete(
        request: ChatCompletionRequest,
        onToken: (TokenChunk) -> Unit = {},
    ): CompletionResult

    suspend fun analyzeImage(
        request: VisionCompletionRequest,
        onToken: (TokenChunk) -> Unit = {},
    ): CompletionResult

    fun stop()

    fun unload()
}

data class GgufModelSpec(
    val modelId: String,
    val displayName: String,
    val baseModelPath: String,
    val family: String? = null,
    val quantization: String? = null,
)

data class GgufVisionProjectorSpec(
    val projectorPath: String,
    val imageMinTokens: Int? = null,
    val imageMaxTokens: Int? = null,
)

data class GgufBackendSpec(
    val requestedBackend: GgufBackendPreference = GgufBackendPreference.CPU,
    val threads: Int? = null,
    val contextSize: Int? = null,
    val batchSize: Int? = null,
)

enum class GgufBackendPreference {
    CPU,
    ACCELERATED,
    AUTO,
}

data class ChatCompletionRequest(
    val taskId: String = "chat",
    val messages: List<GgufChatMessage>,
    val generation: GgufGenerationSettings = GgufGenerationSettings(),
    val stopWords: List<String> = defaultGgufStopWords,
    val requireNativeStreaming: Boolean = true,
)

data class VisionCompletionRequest(
    val taskId: String,
    val messages: List<GgufChatMessage>,
    val imagePaths: List<String>,
    val generation: GgufGenerationSettings = GgufGenerationSettings(temperature = 0f),
    val stopWords: List<String> = defaultGgufStopWords,
    val strictVisionRequired: Boolean = true,
)

data class GgufChatMessage(
    val role: GgufMessageRole,
    val content: List<GgufContentPart>,
) {
    constructor(role: GgufMessageRole, text: String) : this(
        role = role,
        content = listOf(GgufContentPart.Text(text)),
    )
}

enum class GgufMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
}

sealed class GgufContentPart {
    data class Text(val text: String) : GgufContentPart()
    data class ImagePath(val path: String) : GgufContentPart()
}

data class GgufGenerationSettings(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.05f,
    val repeatLastN: Int = 128,
)

data class CompletionResult(
    val finalText: String,
    val structuredJson: String? = null,
    val timing: RuntimeTiming,
    val stopReason: GgufStopReason,
    val modelId: String,
    val runtimeId: String,
    val backendLabel: String,
    val issues: List<RuntimeIssue> = emptyList(),
)

data class RuntimeTiming(
    val promptTokens: Int = 0,
    val generatedTokens: Int = 0,
    val cachedPromptTokens: Int = 0,
    val loadMs: Long? = null,
    val promptEvalMs: Long? = null,
    val timeToFirstVisibleTokenMs: Long? = null,
    val generationMs: Long? = null,
    val totalMs: Long? = null,
    val generatedTokensPerSecond: Double? = null,
)

data class RuntimeCapabilities(
    val runtimeId: String,
    val modelId: String,
    val backendLabel: String,
    val supportsText: Boolean,
    val supportsVision: Boolean,
    val supportsNativeStreaming: Boolean,
    val supportsNativeTimings: Boolean,
    val supportsMetadataChatTemplates: Boolean,
    val supportsCancellation: Boolean,
    val supportsImageTokenBudget: Boolean,
    val supportsAcceleratedBackend: Boolean,
    val loadedProjectorPath: String? = null,
    val issues: List<RuntimeIssue> = emptyList(),
)

data class RuntimeIssue(
    val severity: RuntimeIssueSeverity,
    val code: String,
    val message: String,
)

enum class RuntimeIssueSeverity {
    INFO,
    WARNING,
    ERROR,
}

enum class GgufStopReason {
    EOS,
    STOP_WORD,
    TOKEN_LIMIT,
    CONTEXT_FULL,
    CANCELLED,
    TIMEOUT,
    ERROR,
}

data class TokenChunk(
    val textDelta: String,
    val cumulativeText: String,
    val tokenId: Int? = null,
    val generatedTokenCount: Int,
    val isFirstVisibleToken: Boolean = false,
)

val defaultGgufStopWords: List<String> = listOf(
    "</s>",
    "<|end|>",
    "<|eot_id|>",
    "<|end_of_text|>",
    "<|im_end|>",
    "<|EOT|>",
    "<|END_OF_TURN_TOKEN|>",
    "<|end_of_turn|>",
    "<|endoftext|>",
)
