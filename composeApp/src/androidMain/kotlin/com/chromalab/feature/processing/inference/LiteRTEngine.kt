package com.chromalab.feature.processing.inference

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
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
     * @param modelPath absolute path to .litertlm file
     * @param preferGpu try GPU backend, fall back to CPU if unavailable
     */
    suspend fun loadModel(
        modelPath: String,
        preferGpu: Boolean = true,
    ) = withContext(Dispatchers.IO) {
        // Unload previous model if any
        unload()

        println("LITERT[LOAD] Loading model: $modelPath, preferGpu=$preferGpu")

        val backend = if (preferGpu) Backend.GPU() else Backend.CPU()

        // visionBackend must be set for multimodal (image) support
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
        backendName = "LiteRT ${if (preferGpu) "GPU" else "CPU"}"
        loaded = true
        println("LITERT[LOAD] Model loaded ($backendName)")
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
}
