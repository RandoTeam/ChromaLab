package com.chromalab.feature.processing.inference

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.chromalab.feature.processing.model.DownloadedModel
import com.chromalab.feature.processing.model.ModelFileType
import com.chromalab.feature.processing.model.ModelManager
import com.chromalab.feature.processing.model.ModelRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val GGUF_PARITY_TAG = "GgufParity"

private fun parityLog(message: String) {
    Log.i(GGUF_PARITY_TAG, message)
}

private fun parityError(message: String, throwable: Throwable? = null) {
    Log.e(GGUF_PARITY_TAG, message, throwable)
}

/**
 * Debug-only GGUF parity runner.
 *
 * This class is intentionally outside the production chromatogram pipeline. It
 * runs fixed probes against the current llama.cpp bridge so logcat can be
 * compared with LM Studio / llama.cpp reference behavior on the same files.
 */
class GgufParityDiagnostics(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val manager = ModelManager(appContext)

    suspend fun run(
        requestedModelId: String?,
        requestedImagePath: String?,
    ) = withContext(Dispatchers.IO) {
        val runId = System.currentTimeMillis().toString()
        parityLog("START runId=$runId requestedModelId=${requestedModelId.orEmpty()} imagePath=${requestedImagePath.orEmpty()}")

        val downloaded = manager.getDownloadedModels()
        val model = resolveModel(requestedModelId, downloaded)
        if (model == null) {
            parityError("No downloaded GGUF model found for parity diagnostics")
            return@withContext
        }
        if (model.info.runtime != ModelRuntime.LLAMA_CPP) {
            parityError("Selected model is not GGUF: ${model.info.id} runtime=${model.info.runtime}")
            return@withContext
        }

        val config = InferenceConfig.forModelFamily(model.info.family)
        logModelSnapshot(model, config)

        runTextProbe(model, config)

        val imageFile = requestedImagePath
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.isFile && it.length() > 0L }

        if (imageFile == null) {
            parityLog("IMAGE probe skipped: pass --es imagePath /absolute/path/to/image.jpg")
        } else {
            runImageProbe(model, config, imageFile)
        }

        parityLog("DONE runId=$runId model=${model.info.id}")
    }

    private fun resolveModel(
        requestedModelId: String?,
        downloaded: List<DownloadedModel>,
    ): DownloadedModel? {
        val activeId = requestedModelId
            ?.takeIf { it.isNotBlank() }
            ?: manager.getActiveModelId()

        val selected = activeId?.let { id -> downloaded.find { it.info.id == id } }
        if (selected?.info?.runtime == ModelRuntime.LLAMA_CPP) return selected

        return downloaded.firstOrNull {
            it.info.runtime == ModelRuntime.LLAMA_CPP && ModelRegistry.isChatModel(it.info)
        } ?: downloaded.firstOrNull { it.info.runtime == ModelRuntime.LLAMA_CPP }
    }

    private fun logModelSnapshot(model: DownloadedModel, config: InferenceConfig) {
        val baseFile = File(model.primaryPath)
        val mmprojFile = model.mmprojPath?.let(::File)
        val baseSpec = model.info.files.firstOrNull { it.type == ModelFileType.GGUF_BASE }
        val mmprojSpec = model.info.files.firstOrNull { it.type == ModelFileType.GGUF_MMPROJ }
        parityLog(
            "MODEL id=${model.info.id} name=${model.info.displayName} family=${model.info.family} " +
                "runtime=${model.info.runtime} promptStyle=${config.promptStyle} " +
                "threads=${manager.threadCount} ctxText=${manager.llamaContextSize(model.info, false)} " +
                "ctxVision=${manager.llamaContextSize(model.info, true)} batchText=${manager.llamaBatchSize(model.info, false)} " +
                "batchVision=${manager.llamaBatchSize(model.info, true)}",
        )
        parityLog(
            "DEVICE ramMb=${manager.getDeviceRamMb()} availableRamMb=${manager.getAvailableRamMb()} " +
                "conservative=${manager.isConservativeDevice()} canText=${manager.canLoadForText(model.info)} " +
                "canVision=${manager.canLoadForVision(model.info)}",
        )
        parityLog(
            "FILES base=${baseSpec?.fileName.orEmpty()} exists=${baseFile.isFile} bytes=${baseFile.length()} " +
                "mmproj=${mmprojSpec?.fileName.orEmpty()} exists=${mmprojFile?.isFile ?: false} bytes=${mmprojFile?.length() ?: 0L}",
        )
    }

    private suspend fun runTextProbe(model: DownloadedModel, config: InferenceConfig) {
        val prompt = "Reply with exactly OK."
        val renderedPrompt = formatGgufTextPrompt(prompt, config.promptStyle)
        logPromptSnapshot("TEXT", prompt, renderedPrompt)

        val engine = LlamaEngine()
        val started = SystemClock.elapsedRealtime()
        try {
            parityLog("TEXT load start base=${model.primaryPath}")
            engine.loadModel(
                basePath = model.primaryPath,
                mmprojPath = "",
                threads = manager.threadCount,
                modelFamily = model.info.family,
                contextSize = manager.llamaContextSize(model.info, forVision = false),
                batchSize = manager.llamaBatchSize(model.info, forVision = false),
            )
            parityLog("TEXT load done backend=${engine.getBackendName()} supportsImage=${engine.supportsImageInput()}")

            val output = engine.inferRaw(
                imagePath = "",
                prompt = prompt,
                options = GenerationOptions(
                    maxTokens = 64,
                    temperature = 0f,
                    topP = 1f,
                    topK = 0,
                    repeatPenalty = config.repeatPenalty,
                    repeatLastN = config.repeatLastN,
                ),
            )
            parityLog(
                "TEXT result elapsedMs=${SystemClock.elapsedRealtime() - started} chars=${output.length} " +
                    "preview=${output.preview()}",
            )
        } catch (t: Throwable) {
            parityError("TEXT failed elapsedMs=${SystemClock.elapsedRealtime() - started}: ${t.message}", t)
        } finally {
            engine.unload()
        }
    }

    private suspend fun runImageProbe(
        model: DownloadedModel,
        config: InferenceConfig,
        imageFile: File,
    ) {
        if (!ModelRegistry.hasGgufVisionFilePair(model.info)) {
            parityLog("IMAGE probe skipped: model has no GGUF base+mmproj pair")
            return
        }

        val prompt = "What is shown in this image? Answer in one sentence."
        val renderedPrompt = formatGgufTextPrompt(prompt, config.promptStyle)
        logPromptSnapshot("IMAGE", prompt, renderedPrompt)

        val visionPackage = runCatching { manager.requireGgufVisionPackage(model) }
            .getOrElse { error ->
                parityError("IMAGE probe skipped: ${error.message}", error)
                return
            }

        val engine = LlamaEngine()
        val started = SystemClock.elapsedRealtime()
        try {
            parityLog("IMAGE load start base=${visionPackage.basePath} mmproj=${visionPackage.mmprojPath} image=${imageFile.absolutePath} imageBytes=${imageFile.length()}")
            engine.loadModel(
                basePath = visionPackage.basePath,
                mmprojPath = visionPackage.mmprojPath,
                threads = manager.threadCount,
                modelFamily = model.info.family,
                contextSize = manager.llamaContextSize(model.info, forVision = true),
                batchSize = manager.llamaBatchSize(model.info, forVision = true),
            )
            parityLog("IMAGE load done backend=${engine.getBackendName()} supportsImage=${engine.supportsImageInput()}")

            val output = engine.inferRaw(
                imagePath = imageFile.absolutePath,
                prompt = renderedPrompt,
                options = GenerationOptions(
                    maxTokens = 96,
                    temperature = 0f,
                    topP = 1f,
                    topK = 0,
                    repeatPenalty = config.repeatPenalty,
                    repeatLastN = config.repeatLastN,
                ),
            )
            parityLog(
                "IMAGE result elapsedMs=${SystemClock.elapsedRealtime() - started} chars=${output.length} " +
                    "preview=${output.preview()}",
            )
        } catch (t: Throwable) {
            parityError("IMAGE failed elapsedMs=${SystemClock.elapsedRealtime() - started}: ${t.message}", t)
        } finally {
            engine.unload()
        }
    }

    private fun logPromptSnapshot(label: String, rawPrompt: String, renderedPrompt: String) {
        val roleMarkers = renderedPrompt.countOccurrences("<|im_start|>")
        val mediaMarkers = renderedPrompt.countOccurrences("<__media__>")
        parityLog(
            "$label prompt rawChars=${rawPrompt.length} renderedChars=${renderedPrompt.length} " +
                "roleMarkers=$roleMarkers " +
                "mediaMarkersBeforeNative=$mediaMarkers " +
                "preview=${renderedPrompt.preview()}",
        )
    }
}

private fun String.countOccurrences(needle: String): Int {
    if (needle.isEmpty()) return 0
    var count = 0
    var index = indexOf(needle)
    while (index >= 0) {
        count++
        index = indexOf(needle, index + needle.length)
    }
    return count
}

private fun String.preview(maxChars: Int = 360): String =
    replace(Regex("\\s+"), " ")
        .trim()
        .let { if (it.length <= maxChars) it else it.take(maxChars) + "..." }
