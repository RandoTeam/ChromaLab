package com.chromalab.feature.processing.inference

import android.util.Log
import com.chromalab.feature.processing.inference.InferenceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "ChromaLabLlama"

private fun log(message: String) {
    Log.i(TAG, message)
}

private fun logError(message: String, throwable: Throwable? = null) {
    Log.e(TAG, message, throwable)
}

/**
 * llama.cpp inference engine for .gguf models.
 * Uses JNI bridge to native llama.cpp library (b9219, May 2026).
 *
 * Supports multimodal inference via mtmd (Qwen3.5-VL, Gemma 4 VL, etc.).
 * Vision models require base .gguf + mmproj .gguf pair.
 */
class LlamaEngine : InferenceEngine {

    private var modelHandle: Long = 0L
    private var backendName: String = "llama.cpp CPU"
    private var loaded: Boolean = false
    private var hasVisionProjector: Boolean = false
    private var config: InferenceConfig = InferenceConfig.DEFAULT
    private val nativeLock = ReentrantLock()

    @Volatile
    private var unloadRequested: Boolean = false

    companion object {
        private var nativeLoaded = false

        /**
         * Load the native llama_bridge library.
         * Must be called before any engine operations.
         */
        fun loadNativeLibrary() {
            if (!nativeLoaded) {
                try {
                    System.loadLibrary("llama_bridge")
                    nativeLoaded = true
                    log("Native library loaded successfully")
                } catch (e: UnsatisfiedLinkError) {
                    logError("Failed to load native library: ${e.message}", e)
                }
            }
        }

        fun availableBackendCodesForDiagnostics(): IntArray {
            if (!nativeLoaded) {
                loadNativeLibrary()
            }
            return if (nativeLoaded) {
                runCatching { nativeGetAvailableBackends() }.getOrDefault(intArrayOf(0))
            } else {
                intArrayOf(0)
            }
        }

        // JNI native methods
        @JvmStatic private external fun nativeGetAvailableBackends(): IntArray
        @JvmStatic private external fun nativeLoadModel(
            basePath: String,
            mmprojPath: String,
            threads: Int,
            backendCode: Int,
            contextSize: Int,
            batchSize: Int,
            mtpDraftTokens: Int,
        ): Long
        @JvmStatic private external fun nativeUnloadModel(handle: Long)
        @JvmStatic private external fun nativeGetLoadedBackendName(handle: Long): String
        @JvmStatic private external fun nativeInferWithImage(
            handle: Long,
            imagePath: String,
            prompt: String,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float,
            repeatLastN: Int,
        ): String
        @JvmStatic private external fun nativeInferText(
            handle: Long,
            prompt: String,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float,
            repeatLastN: Int,
        ): String
        @JvmStatic private external fun nativeInferChat(
            handle: Long,
            roles: Array<String>,
            contents: Array<String>,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float,
            repeatLastN: Int,
        ): String
        @JvmStatic private external fun nativeInferChatStreaming(
            handle: Long,
            roles: Array<String>,
            contents: Array<String>,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float,
            repeatLastN: Int,
            callback: NativeTokenCallback,
        ): String
    }

    /**
     * Load a GGUF model + optional vision projector.
     *
     * @param basePath path to base model .gguf
     * @param mmprojPath path to vision projector .gguf (empty string if none)
     * @param threads number of CPU threads (1..N)
     * @param modelFamily model family for auto-selecting InferenceConfig
     */
    suspend fun loadModel(
        basePath: String,
        mmprojPath: String,
        threads: Int = 4,
        modelFamily: String = "",
        contextSize: Int? = null,
        batchSize: Int? = null,
        preferAccelerated: Boolean = false,
        mtpDraftTokens: Int = 0,
    ) = withContext(Dispatchers.IO) {
        nativeLock.withLock {
            if (!nativeLoaded) {
                loadNativeLibrary()
            }

            require(nativeLoaded) { "Native library not available" }

            if (modelHandle != 0L) {
                unloadLocked()
            }
            unloadRequested = false

            // Auto-select inference config for this model family
            config = InferenceConfig.forModelFamily(modelFamily)
            val ctx = contextSize ?: config.contextSize
            val batch = batchSize ?: config.batchSize
            val requestedBackendCode = if (preferAccelerated) 1 else 0
            log("Loading model base=$basePath mmproj=$mmprojPath threads=$threads backendCode=$requestedBackendCode")
            log("Config maxTokens=${config.maxTokens} repeatPenalty=${config.repeatPenalty} repeatLastN=${config.repeatLastN} ctx=$ctx batch=$batch mtpDraftTokens=$mtpDraftTokens")

            modelHandle = nativeLoadModel(
                basePath,
                mmprojPath,
                threads,
                requestedBackendCode,
                ctx,
                batch,
                mtpDraftTokens.coerceIn(0, 16),
            )
            if (modelHandle == 0L) {
                loaded = false
                hasVisionProjector = false
                throw RuntimeException("Failed to load model: $basePath")
            }

            backendName = nativeGetLoadedBackendName(modelHandle).ifBlank { "llama.cpp CPU" }
            loaded = true
            hasVisionProjector = mmprojPath.isNotBlank()
            log("Model loaded handle=$modelHandle vision=$hasVisionProjector backend=$backendName")
        }
    }

    override suspend fun analyzeChart(imagePath: String, prompt: String): ChartAnalysis {
        return withContext(Dispatchers.IO) {
            nativeLock.withLock {
                check(loaded && nativeLoaded) { "Model not loaded" }
                check(hasVisionProjector) { "GGUF image analysis requires an mmproj vision projector" }

                log("Analyze chart image=$imagePath maxTokens=${config.maxTokens} repeatPenalty=${config.repeatPenalty}")

                try {
                    val responseText = nativeInferWithImage(
                        modelHandle, imagePath, prompt,
                        config.maxTokens,
                        0f,
                        1f,
                        0,
                        config.repeatPenalty,
                        config.repeatLastN,
                    )
                    log("Analyze chart response chars=${responseText.length}")
                    ChartPrompts.parseResponse(responseText)
                } finally {
                    unloadIfRequestedLocked()
                }
            }
        }
    }

    override suspend fun inferRaw(
        imagePath: String,
        prompt: String,
        options: GenerationOptions,
    ): String {
        return withContext(Dispatchers.IO) {
            nativeLock.withLock {
                check(loaded && nativeLoaded) { "Model not loaded" }

                log("Raw inference image=$imagePath")
                val maxTokens = options.maxTokens ?: config.maxTokens
                val temperature = options.temperature ?: 0f
                val topP = options.topP ?: 1f
                val topK = options.topK ?: 0
                val repeatPenalty = options.repeatPenalty ?: config.repeatPenalty
                val repeatLastN = options.repeatLastN ?: config.repeatLastN
                val hasImage = imagePath.isNotBlank() && File(imagePath).isFile
                try {
                    val responseText = if (hasImage && hasVisionProjector) {
                        nativeInferWithImage(
                            modelHandle, imagePath, prompt,
                            maxTokens,
                            temperature,
                            topP,
                            topK,
                            repeatPenalty,
                            repeatLastN,
                        )
                    } else if (hasImage) {
                        log("Image inference requested, but no mmproj is loaded")
                        ""
                    } else {
                        inferTextLocked(
                            prompt = prompt,
                            maxTokens = maxTokens,
                            temperature = temperature,
                            topP = topP,
                            topK = topK,
                            repeatPenalty = repeatPenalty,
                            repeatLastN = repeatLastN,
                            onPartial = null,
                        )
                    }
                    log("Raw response chars=${responseText.length}")
                    if (responseText.isBlank()) {
                        val mode = if (hasImage) "image" else "text"
                        throw IllegalStateException(
                            "GGUF $mode inference returned an empty response " +
                                "(promptStyle=${config.promptStyle}, backend=$backendName)",
                        )
                    }
                    responseText
                } finally {
                    unloadIfRequestedLocked()
                }
            }
        }
    }

    override suspend fun inferRawStreaming(
        imagePath: String,
        prompt: String,
        options: GenerationOptions,
        onPartial: (String) -> Unit,
    ): String {
        return withContext(Dispatchers.IO) {
            val hasImage = imagePath.isNotBlank() && File(imagePath).isFile
            if (hasImage) {
                return@withContext super<InferenceEngine>.inferRawStreaming(imagePath, prompt, options, onPartial)
            }

            nativeLock.withLock {
                check(loaded && nativeLoaded) { "Model not loaded" }

                val maxTokens = options.maxTokens ?: config.maxTokens
                val temperature = options.temperature ?: 0f
                val topP = options.topP ?: 1f
                val topK = options.topK ?: 0
                val repeatPenalty = options.repeatPenalty ?: config.repeatPenalty
                val repeatLastN = options.repeatLastN ?: config.repeatLastN

                try {
                    inferTextLocked(
                        prompt = prompt,
                        maxTokens = maxTokens,
                        temperature = temperature,
                        topP = topP,
                        topK = topK,
                        repeatPenalty = repeatPenalty,
                        repeatLastN = repeatLastN,
                        onPartial = onPartial,
                        onNativeToken = null,
                    )
                } finally {
                    unloadIfRequestedLocked()
                }
            }
        }
    }

    suspend fun inferTextOnlyForDiagnostics(
        prompt: String,
        options: GenerationOptions,
        onNativeToken: (text: String, generatedTokens: Int, elapsedMs: Long) -> Unit,
    ): String {
        return withContext(Dispatchers.IO) {
            nativeLock.withLock {
                check(loaded && nativeLoaded) { "Model not loaded" }
                val maxTokens = options.maxTokens ?: config.maxTokens
                val temperature = options.temperature ?: 0f
                val topP = options.topP ?: 1f
                val topK = options.topK ?: 0
                val repeatPenalty = options.repeatPenalty ?: config.repeatPenalty
                val repeatLastN = options.repeatLastN ?: config.repeatLastN

                try {
                    inferTextLocked(
                        prompt = prompt,
                        maxTokens = maxTokens,
                        temperature = temperature,
                        topP = topP,
                        topK = topK,
                        repeatPenalty = repeatPenalty,
                        repeatLastN = repeatLastN,
                        onPartial = {},
                        onNativeToken = onNativeToken,
                    )
                } finally {
                    unloadIfRequestedLocked()
                }
            }
        }
    }

    override fun isLoaded(): Boolean = loaded && nativeLoaded

    override fun supportsImageInput(): Boolean = loaded && hasVisionProjector

    override fun unload() {
        if (!nativeLoaded || modelHandle == 0L) {
            loaded = false
            hasVisionProjector = false
            return
        }

        if (!nativeLock.tryLock()) {
            unloadRequested = true
            loaded = false
            hasVisionProjector = false
            log("Unload deferred until active inference finishes")
            return
        }

        try {
            unloadLocked()
        } finally {
            nativeLock.unlock()
        }
    }

    private fun unloadIfRequestedLocked() {
        if (unloadRequested && modelHandle != 0L) {
            unloadLocked()
        }
    }

    private fun unloadLocked() {
        if (!nativeLoaded || modelHandle == 0L) return
        try {
            nativeUnloadModel(modelHandle)
        } catch (e: Exception) {
            logError("Unload error: ${e.message}", e)
        }
        modelHandle = 0L
        loaded = false
        hasVisionProjector = false
        unloadRequested = false
        log("Model unloaded")
    }

    override fun getBackendName(): String = backendName

    private fun inferTextLocked(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float,
        repeatLastN: Int,
        onPartial: ((String) -> Unit)?,
        onNativeToken: ((text: String, generatedTokens: Int, elapsedMs: Long) -> Unit)? = null,
    ): String {
        val startedAt = System.currentTimeMillis()
        log(
            "Text inference start streaming=${onPartial != null} promptChars=${prompt.length} " +
                "maxTokens=$maxTokens temperature=$temperature topP=$topP topK=$topK " +
                "repeatPenalty=$repeatPenalty repeatLastN=$repeatLastN backend=$backendName",
        )
        val chatPrompt = prompt.toNativeChatPromptOrNull(config)
        if (chatPrompt != null) {
            log(
                "Using llama.cpp chat template for text inference " +
                    "messages=${chatPrompt.roles.size} promptStyle=${config.promptStyle}",
            )
            return if (onPartial == null) {
                nativeInferChat(
                    modelHandle,
                    chatPrompt.roles,
                    chatPrompt.contents,
                    maxTokens,
                    temperature,
                    topP,
                    topK,
                    repeatPenalty,
                    repeatLastN,
                ).also { result ->
                    log("Text inference complete mode=chat chars=${result.length} elapsedMs=${System.currentTimeMillis() - startedAt}")
                }
            } else {
                var callbackCount = 0
                nativeInferChatStreaming(
                    modelHandle,
                    chatPrompt.roles,
                    chatPrompt.contents,
                    maxTokens,
                    temperature,
                    topP,
                    topK,
                    repeatPenalty,
                    repeatLastN,
                    object : NativeTokenCallback {
                        override fun onToken(text: String, generatedTokens: Int, elapsedMs: Long) {
                            callbackCount += 1
                            onNativeToken?.invoke(text, generatedTokens, elapsedMs)
                            if (callbackCount == 1) {
                                log("Text first token generatedTokens=$generatedTokens nativeElapsedMs=$elapsedMs wallMs=${System.currentTimeMillis() - startedAt}")
                            } else if (callbackCount % 32 == 0) {
                                log("Text streaming progress callbacks=$callbackCount generatedTokens=$generatedTokens nativeElapsedMs=$elapsedMs")
                            }
                            onPartial(text)
                        }
                    },
                ).also { result ->
                    log(
                        "Text inference complete mode=chat_stream chars=${result.length} " +
                            "callbacks=$callbackCount elapsedMs=${System.currentTimeMillis() - startedAt}",
                    )
                }
            }
        }

        val textPrompt = formatGgufTextPrompt(prompt, config.promptStyle)
        if (textPrompt != prompt) {
            log("Applied ${config.promptStyle} prompt formatting for text inference")
        }
        return nativeInferText(
            modelHandle,
            textPrompt,
            maxTokens,
            temperature,
            topP,
            topK,
            repeatPenalty,
            repeatLastN,
        ).also { result ->
            log("Text inference complete mode=text chars=${result.length} elapsedMs=${System.currentTimeMillis() - startedAt}")
        }
    }
}

private interface NativeTokenCallback {
    fun onToken(text: String, generatedTokens: Int, elapsedMs: Long)
}

private data class NativeChatPrompt(
    val roles: Array<String>,
    val contents: Array<String>,
)

private fun String.toNativeChatPromptOrNull(config: InferenceConfig): NativeChatPrompt? {
    if (!config.shouldUseNativeChatTemplate()) return null
    if (containsKnownTemplateMarkup()) return null

    val messages = parseTranscriptMessages()
        .ifEmpty { listOf("user" to trim()) }
        .filter { (_, content) -> content.isNotBlank() }

    if (messages.isEmpty()) return null
    return NativeChatPrompt(
        roles = messages.map { it.first }.toTypedArray(),
        contents = messages.map { it.second }.toTypedArray(),
    )
}

private fun InferenceConfig.shouldUseNativeChatTemplate(): Boolean =
    promptStyle != PromptStyle.TRIGGER &&
        promptStyle != PromptStyle.DEEPSEEK_OCR &&
        promptStyle != PromptStyle.DIRECT_QUESTION &&
        promptStyle != PromptStyle.LITERT

private fun String.containsKnownTemplateMarkup(): Boolean =
    contains("<|im_start|>") ||
        contains("<|start_header_id|>") ||
        contains("[INST]") ||
        contains("<start_of_turn>")

private fun String.parseTranscriptMessages(): List<Pair<String, String>> {
    val lines = lines()
    val messages = mutableListOf<Pair<String, String>>()
    var role: String? = null
    val buffer = StringBuilder()

    fun flush() {
        val currentRole = role ?: return
        val content = buffer.toString().trim()
        if (content.isNotBlank()) {
            messages += currentRole to content
        }
        buffer.clear()
    }

    lines.forEach { line ->
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("User:") -> {
                flush()
                role = "user"
                buffer.append(trimmed.removePrefix("User:").trimStart())
            }
            trimmed.startsWith("Assistant:") -> {
                flush()
                role = "assistant"
                buffer.append(trimmed.removePrefix("Assistant:").trimStart())
            }
            role == null -> {
                role = "system"
                if (buffer.isNotEmpty()) buffer.appendLine()
                buffer.append(line)
            }
            else -> {
                if (buffer.isNotEmpty()) buffer.appendLine()
                buffer.append(line)
            }
        }
    }
    flush()

    return messages
        .dropLastWhile { (messageRole, content) -> messageRole == "assistant" && content.isBlank() }
}
