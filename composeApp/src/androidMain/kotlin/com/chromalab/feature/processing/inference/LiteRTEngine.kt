package com.chromalab.feature.processing.inference

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * LiteRT-LM inference engine for .litertlm models (Gemma 4, etc.).
 * Uses Google's LiteRT-LM SDK v0.11.0 with GPU/CPU acceleration.
 *
 * Supports multimodal inference: Content.ImageFile + Content.Text.
 *
 * EngineConfig params verified from actual AAR bytecode (v0.11.0):
 *   EngineConfig(modelPath, backend, visionBackend, audioBackend,
 *                maxNumTokens, maxNumImages, cacheDir)
 */
class LiteRTEngine : InferenceEngine {

    private var engine: Engine? = null
    private var backendName: String = "LiteRT CPU"
    private var loaded: Boolean = false

    /**
     * Load a .litertlm model file.
     *
     * Tries backends in order: NPU → GPU → CPU.
     * NPU is fastest on Snapdragon (QNN) / MediaTek (NeuroPilot).
     * GPU uses OpenCL/OpenGL via ML Drift engine.
     * CPU is the universal fallback.
     *
     * @param modelPath absolute path to .litertlm file
     */
    suspend fun loadModel(
        modelPath: String,
        preferGpu: Boolean = true,
    ) = withContext(Dispatchers.IO) {
        // Unload previous model if any
        unload()

        println("LITERT[LOAD] Loading model: $modelPath")

        // Try backends from fastest to slowest
        val backends = if (preferGpu) {
            listOf("NPU" to Backend.NPU(), "GPU" to Backend.GPU(), "CPU" to Backend.CPU())
        } else {
            listOf("CPU" to Backend.CPU())
        }

        var lastError: Exception? = null
        for ((name, backend) in backends) {
            try {
                println("LITERT[LOAD] Trying $name backend...")

                val engineConfig = EngineConfig(
                    modelPath,
                    backend,       // backend
                    backend,       // visionBackend — same as main for image processing
                    null,          // audioBackend
                    null,          // maxNumTokens
                    1,             // maxNumImages
                    null,          // cacheDir
                )

                val eng = Engine(engineConfig)
                eng.initialize()

                engine = eng
                backendName = "LiteRT $name"
                loaded = true
                println("LITERT[LOAD] Model loaded ($backendName)")
                return@withContext
            } catch (e: Exception) {
                println("LITERT[LOAD] $name failed: ${e.message}")
                lastError = e
            }
        }

        throw lastError ?: IllegalStateException("No backend available")
    }

    override suspend fun analyzeChart(imagePath: String, prompt: String): ChartAnalysis {
        val eng = engine
        check(loaded && eng != null) { "LiteRT model not loaded" }

        return withContext(Dispatchers.IO) {
            println("LITERT[INFER] Analyzing: $imagePath")

            val conversation = eng.createConversation()
            try {
                // Build multimodal contents: image + text
                val imageFile = File(imagePath)
                val contents: Contents = if (imageFile.exists()) {
                    Contents.of(
                        Content.ImageFile(imagePath),
                        Content.Text(prompt),
                    )
                } else {
                    println("LITERT[INFER] Image not found, text-only")
                    Contents.of(Content.Text(prompt))
                }

                // Synchronous send — returns complete Message
                val response: Message = conversation.sendMessage(contents)

                // Extract text from response
                val responseText = extractText(response)
                println("LITERT[INFER] Response: ${responseText.length} chars")
                ChartPrompts.parseResponse(responseText)
            } catch (e: Exception) {
                println("LITERT[INFER] Error: ${e.message}")
                ChartAnalysis(xValues = emptyList(), yValues = emptyList(), confidence = 0f)
            } finally {
                // Conversation implements AutoCloseable
                (conversation as? AutoCloseable)?.close()
            }
        }
    }

    /**
     * Extract text content from a LiteRT-LM Message.
     * The response Message.contents is a Contents object containing List<Content>.
     * We concatenate all Content.Text entries.
     */
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

    override fun isLoaded(): Boolean = loaded

    override suspend fun inferRaw(
        imagePath: String,
        prompt: String,
        options: GenerationOptions,
    ): String {
        val eng = engine
        check(loaded && eng != null) { "LiteRT model not loaded" }

        return withContext(Dispatchers.IO) {
            println("LITERT[RAW] Inferring: $imagePath")
            val conversation = eng.createConversation(createConversationConfig(options))
            try {
                val imageFile = File(imagePath)
                val contents: Contents = if (imageFile.exists()) {
                    Contents.of(
                        Content.ImageFile(imagePath),
                        Content.Text(prompt),
                    )
                } else {
                    Contents.of(Content.Text(prompt))
                }
                val response: Message = conversation.sendMessage(contents)
                val responseText = extractText(response)
                println("LITERT[RAW] Response: ${responseText.length} chars")
                responseText
            } catch (e: Exception) {
                println("LITERT[RAW] Error: ${e.message}")
                ""
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
        println("LITERT[UNLOAD] Unloaded")
    }

    override fun getBackendName(): String = backendName

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
}
