package com.chromalab.feature.processing.inference

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

private const val TAG = "ChromaLabLiteRT"

/**
 * LiteRT-LM inference engine for .litertlm models.
 */
class LiteRTEngine : InferenceEngine {

    private var engine: Engine? = null
    private var backendName: String = "LiteRT CPU"
    private var loaded: Boolean = false
    private var visionEnabled: Boolean = true

    suspend fun loadModel(
        modelPath: String,
        preferGpu: Boolean = true,
        enableVision: Boolean = true,
        maxNumTokens: Int? = null,
        cacheDir: String? = null,
    ) = withContext(Dispatchers.IO) {
        unload()

        log("LOAD model=$modelPath vision=$enableVision maxTokens=$maxNumTokens preferGpu=$preferGpu cacheDir=$cacheDir")

        val backends = if (preferGpu) {
            listOf("GPU" to Backend.GPU(), "CPU" to Backend.CPU())
        } else {
            listOf("CPU" to Backend.CPU())
        }

        var lastError: Exception? = null
        for ((name, backend) in backends) {
            var candidate: Engine? = null
            try {
                log("LOAD trying backend=$name")

                val engineConfig = EngineConfig(
                    modelPath = modelPath,
                    backend = backend,
                    visionBackend = if (enableVision) backend else null,
                    audioBackend = null,
                    maxNumTokens = maxNumTokens,
                    maxNumImages = if (enableVision) 1 else null,
                    cacheDir = cacheDir,
                )

                candidate = Engine(engineConfig)
                candidate.initialize()

                engine = candidate
                backendName = if (enableVision) "LiteRT $name" else "LiteRT $name text-only"
                loaded = true
                visionEnabled = enableVision
                log("LOAD success backend=$backendName")
                return@withContext
            } catch (e: Exception) {
                logError("LOAD backend=$name failed: ${e.message}", e)
                runCatching { candidate?.close() }
                lastError = e
            }
        }

        throw lastError ?: IllegalStateException("No backend available")
    }

    override suspend fun analyzeChart(imagePath: String, prompt: String): ChartAnalysis {
        val eng = engine
        check(loaded && eng != null) { "LiteRT model not loaded" }

        if (!visionEnabled) {
            log("INFER vision disabled; skipping image analysis")
            return ChartAnalysis(xValues = emptyList(), yValues = emptyList(), confidence = 0f)
        }

        return withContext(Dispatchers.IO) {
            log("INFER analyze image=$imagePath backend=$backendName")
            val conversation = eng.createConversation()
            try {
                val responseText = sendMessageText(
                    conversation = conversation,
                    contents = buildContents(imagePath, prompt),
                    timeoutMs = DEFAULT_INFERENCE_TIMEOUT_MS,
                )
                log("INFER response chars=${responseText.length}")
                ChartPrompts.parseResponse(responseText)
            } catch (e: Exception) {
                logError("INFER error: ${e.message}", e)
                ChartAnalysis(xValues = emptyList(), yValues = emptyList(), confidence = 0f)
            } finally {
                (conversation as? AutoCloseable)?.close()
            }
        }
    }

    override fun isLoaded(): Boolean = loaded

    override fun supportsImageInput(): Boolean = loaded && visionEnabled

    override suspend fun inferRaw(
        imagePath: String,
        prompt: String,
        options: GenerationOptions,
    ): String {
        val eng = engine
        check(loaded && eng != null) { "LiteRT model not loaded" }

        return withContext(Dispatchers.IO) {
            log("RAW infer image=$imagePath backend=$backendName maxTokens=${options.maxTokens} timeoutMs=${options.timeoutMs}")
            val conversation = eng.createConversation(createConversationConfig(options))
            try {
                val responseText = sendMessageText(
                    conversation = conversation,
                    contents = buildContents(imagePath, prompt),
                    timeoutMs = options.timeoutMs ?: DEFAULT_INFERENCE_TIMEOUT_MS,
                )
                log("RAW response chars=${responseText.length}")
                responseText
            } catch (e: Exception) {
                logError("RAW error: ${e.message}", e)
                ""
            } finally {
                (conversation as? AutoCloseable)?.close()
            }
        }
    }

    override suspend fun inferRawStreaming(
        imagePath: String,
        prompt: String,
        options: GenerationOptions,
        onPartial: (String) -> Unit,
    ): String {
        val eng = engine
        check(loaded && eng != null) { "LiteRT model not loaded" }

        return withContext(Dispatchers.IO) {
            log("STREAM infer image=$imagePath backend=$backendName maxTokens=${options.maxTokens} timeoutMs=${options.timeoutMs}")
            val conversation = eng.createConversation(createConversationConfig(options))
            val result = StringBuilder()

            try {
                withTimeout(options.timeoutMs ?: DEFAULT_INFERENCE_TIMEOUT_MS) {
                    conversation.sendMessageAsync(buildContents(imagePath, prompt)).collect { message ->
                        val chunk = extractText(message).ifBlank { message.toString() }
                        if (chunk.isNotEmpty()) {
                            result.append(chunk)
                            onPartial(chunk)
                        }
                    }
                }
                log("STREAM response chars=${result.length}")
                result.toString()
            } catch (e: TimeoutCancellationException) {
                runCatching { conversation.cancelProcess() }
                logError("STREAM timeout after ${options.timeoutMs ?: DEFAULT_INFERENCE_TIMEOUT_MS}ms", e)
                result.toString()
            } finally {
                (conversation as? AutoCloseable)?.close()
            }
        }
    }

    override fun unload() {
        try {
            engine?.close()
        } catch (e: Exception) {
            println("LITERT[UNLOAD] Error: ${e.message}")
        }
        engine = null
        loaded = false
        visionEnabled = true
        log("UNLOAD done")
    }

    override fun getBackendName(): String = backendName

    private fun buildContents(imagePath: String, prompt: String): Contents {
        val imageFile = File(imagePath)
        return if (visionEnabled && imageFile.exists()) {
            Contents.of(
                Content.ImageFile(imagePath),
                Content.Text(prompt),
            )
        } else {
            if (imageFile.exists() && !visionEnabled) {
                log("RAW vision disabled, using text-only prompt")
            } else if (!imageFile.exists()) {
                log("RAW image not found, using text-only prompt")
            }
            Contents.of(Content.Text(prompt))
        }
    }

    private fun extractText(message: Message): String {
        val contentList = message.contents.contents
        val sb = StringBuilder()
        for (item in contentList) {
            if (item is Content.Text) {
                sb.append(item.text)
            }
        }
        return sb.toString()
    }

    private fun createConversationConfig(options: GenerationOptions): ConversationConfig {
        val sampler = if (
            options.temperature != null ||
            options.topP != null ||
            options.topK != null
        ) {
            SamplerConfig(
                topK = options.topK?.coerceIn(1, 256) ?: 40,
                topP = options.topP?.toDouble()?.coerceIn(0.05, 1.0) ?: 0.95,
                temperature = options.temperature?.toDouble()?.coerceIn(0.0, 2.0) ?: 0.7,
                seed = 0,
            )
        } else {
            null
        }

        return if (sampler != null) {
            ConversationConfig(samplerConfig = sampler)
        } else {
            ConversationConfig()
        }
    }

    private suspend fun sendMessageText(
        conversation: Conversation,
        contents: Contents,
        timeoutMs: Long,
    ): String = try {
        val result = StringBuilder()
        withTimeout(timeoutMs) {
            conversation.sendMessageAsync(contents).collect { message ->
                val chunk = extractText(message)
                if (chunk.isNotEmpty()) result.append(chunk)
            }
        }
        result.toString()
    } catch (e: TimeoutCancellationException) {
        runCatching { conversation.cancelProcess() }
        logError("TIMEOUT after ${timeoutMs}ms", e)
        ""
    }

    private fun log(message: String) {
        Log.i(TAG, "LITERT[$message]")
        println("LITERT[$message]")
    }

    private fun logError(message: String, error: Throwable? = null) {
        Log.e(TAG, "LITERT[$message]", error)
        println("LITERT[$message]")
    }

    private companion object {
        const val DEFAULT_INFERENCE_TIMEOUT_MS = 600_000L
    }
}
