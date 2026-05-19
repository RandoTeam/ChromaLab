package com.chromalab.feature.processing.inference

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.chromalab.feature.chat.ChatMtpRuntimeProfile
import com.chromalab.feature.chat.ChatRuntimeAccelerator
import com.chromalab.feature.processing.model.DownloadedModel
import com.chromalab.feature.processing.model.ModelManager
import com.chromalab.feature.processing.model.ModelRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MTP_AB_TAG = "ChromaLabMtpAB"

private fun mtpAbLog(message: String) {
    Log.i(MTP_AB_TAG, message)
}

private fun mtpAbError(message: String, throwable: Throwable? = null) {
    Log.e(MTP_AB_TAG, message, throwable)
}

class MtpAbDiagnostics(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val manager = ModelManager(appContext)

    suspend fun run(
        requestedModelId: String?,
        requestedPrompt: String?,
        requestedBackend: String?,
        requestedDraftTokens: Int?,
        requestedContextTokens: Int?,
        requestedBatchTokens: Int?,
        requestedMaxTokens: Int?,
    ) = withContext(Dispatchers.IO) {
        val runId = System.currentTimeMillis().toString()
        val backendMode = MtpAbBackendMode.from(requestedBackend)
        val model = resolveModel(requestedModelId)
        if (model == null) {
            mtpAbError("ABORT runId=$runId reason=no_downloaded_mtp_gguf_model")
            return@withContext
        }
        if (!model.info.supportsMtp) {
            mtpAbError("ABORT runId=$runId reason=model_does_not_advertise_mtp model=${model.info.id}")
            return@withContext
        }

        VlmEngineHolder.activeEngine?.unload()
        VlmEngineHolder.activeEngine = null
        VlmEngineHolder.activeConfig = null
        VlmEngineHolder.selectedModel = null
        VlmEngineHolder.executedModel = null

        val conservative = manager.isConservativeDevice()
        val prompt = requestedPrompt?.takeIf { it.isNotBlank() }
            ?: "Reply with exactly OK."
        val contextTokens = requestedContextTokens
            ?.coerceIn(1024, model.info.chatContextLimit)
            ?: ChatMtpRuntimeProfile.coerceContextTokens(
                requestedContextTokens = model.info.defaultChatContextSize,
                modelContextLimit = model.info.chatContextLimit,
                isConservativeDevice = conservative,
            )
        val batchTokens = requestedBatchTokens
            ?.coerceIn(32, 512)
            ?: ChatMtpRuntimeProfile.coerceBatchTokens(
                requestedBatchTokens = manager.llamaBatchSize(model.info, forVision = false),
                isConservativeDevice = conservative,
            )
        val draftTokens = requestedDraftTokens
            ?.takeIf { it > 0 }
            ?.coerceIn(1, model.info.maxMtpDraftTokens.coerceAtLeast(1))
            ?: model.info.defaultMtpDraftTokens.coerceAtLeast(1)
        val effectiveDraftTokens = ChatMtpRuntimeProfile.coerceDraftTokens(
            requestedDraftTokens = draftTokens,
            selectedAccelerator = backendMode.accelerator,
            isConservativeDevice = conservative,
        )
        val maxTokens = requestedMaxTokens?.coerceIn(4, 256) ?: 32

        mtpAbLog(
            "START runId=$runId model=${model.info.id} backend=${backendMode.logName} " +
                "threads=${manager.threadCount} ctx=$contextTokens batch=$batchTokens " +
                "draft=$effectiveDraftTokens maxTokens=$maxTokens conservative=$conservative " +
                "promptChars=${prompt.length}",
        )

        val baseline = runProbePass(
            runId = runId,
            label = "no_mtp",
            model = model,
            prompt = prompt,
            contextTokens = contextTokens,
            batchTokens = batchTokens,
            mtpDraftTokens = 0,
            maxTokens = maxTokens,
            backendMode = backendMode,
        )
        val mtp = runProbePass(
            runId = runId,
            label = "mtp",
            model = model,
            prompt = prompt,
            contextTokens = contextTokens,
            batchTokens = batchTokens,
            mtpDraftTokens = effectiveDraftTokens,
            maxTokens = maxTokens,
            backendMode = backendMode,
        )

        val verdict = when {
            baseline.error != null || mtp.error != null -> "INCONCLUSIVE"
            baseline.firstTokenMs == null || mtp.firstTokenMs == null -> "INCONCLUSIVE"
            mtp.elapsedMs < baseline.elapsedMs && mtp.firstTokenMs <= baseline.firstTokenMs * 1.2 -> "MTP_FASTER"
            mtp.firstTokenMs > baseline.firstTokenMs * 1.5 -> "MTP_SLOW_TTFT"
            mtp.elapsedMs > baseline.elapsedMs -> "MTP_SLOW_TOTAL"
            else -> "MTP_NEUTRAL"
        }
        mtpAbLog(
            "SUMMARY runId=$runId verdict=$verdict " +
                "no_mtp_loadMs=${baseline.loadMs} no_mtp_firstMs=${baseline.firstTokenMs} " +
                "no_mtp_elapsedMs=${baseline.elapsedMs} no_mtp_chars=${baseline.chars} " +
                "mtp_loadMs=${mtp.loadMs} mtp_firstMs=${mtp.firstTokenMs} " +
                "mtp_elapsedMs=${mtp.elapsedMs} mtp_chars=${mtp.chars}",
        )
        mtpAbLog("DONE runId=$runId")
    }

    private fun resolveModel(requestedModelId: String?): DownloadedModel? {
        val downloaded = manager.getDownloadedModels()
        val requested = requestedModelId
            ?.takeIf { it.isNotBlank() }
            ?.let { id -> downloaded.find { it.info.id == id } }
        if (requested?.info?.runtime == ModelRuntime.LLAMA_CPP) return requested

        val active = manager.getActiveModelId()
            ?.let { id -> downloaded.find { it.info.id == id } }
        if (active?.info?.runtime == ModelRuntime.LLAMA_CPP && active.info.supportsMtp) return active

        return downloaded.firstOrNull {
            it.info.runtime == ModelRuntime.LLAMA_CPP &&
                it.info.supportsMtp &&
                ModelRegistry.isChatModel(it.info)
        }
    }

    private suspend fun runProbePass(
        runId: String,
        label: String,
        model: DownloadedModel,
        prompt: String,
        contextTokens: Int,
        batchTokens: Int,
        mtpDraftTokens: Int,
        maxTokens: Int,
        backendMode: MtpAbBackendMode,
    ): MtpAbPassResult {
        val engine = LlamaEngine()
        val started = SystemClock.elapsedRealtime()
        var firstTokenMs: Long? = null
        var callbacks = 0
        var chars = 0
        return try {
            mtpAbLog("PASS_START runId=$runId label=$label draft=$mtpDraftTokens")
            engine.loadModel(
                basePath = model.primaryPath,
                mmprojPath = "",
                threads = manager.threadCount,
                modelFamily = model.info.family,
                contextSize = contextTokens,
                batchSize = batchTokens,
                preferAccelerated = backendMode.preferAccelerated,
                mtpDraftTokens = mtpDraftTokens,
            )
            val loadMs = SystemClock.elapsedRealtime() - started
            mtpAbLog("PASS_LOAD_DONE runId=$runId label=$label loadMs=$loadMs backend=${engine.getBackendName()}")
            val inferStarted = SystemClock.elapsedRealtime()
            val output = engine.inferRawStreaming(
                imagePath = "__chromalab_text_only__",
                prompt = "User: $prompt\nAssistant:",
                options = GenerationOptions(
                    maxTokens = maxTokens,
                    temperature = 0.15f,
                    topP = 0.95f,
                    topK = 40,
                    repeatPenalty = 1.05f,
                    repeatLastN = 128,
                ),
                onPartial = { chunk ->
                    callbacks += 1
                    chars += chunk.length
                    if (firstTokenMs == null) {
                        firstTokenMs = SystemClock.elapsedRealtime() - inferStarted
                        mtpAbLog("PASS_FIRST_TOKEN runId=$runId label=$label firstTokenMs=$firstTokenMs chunkChars=${chunk.length}")
                    }
                },
            )
            val elapsedMs = SystemClock.elapsedRealtime() - inferStarted
            val tpsApprox = if (elapsedMs > 0L) callbacks * 1000.0 / elapsedMs else 0.0
            mtpAbLog(
                "PASS_DONE runId=$runId label=$label loadMs=$loadMs firstTokenMs=$firstTokenMs " +
                    "elapsedMs=$elapsedMs callbacks=$callbacks chars=${output.length} approxTps=$tpsApprox " +
                    "preview=${output.previewForLog()}",
            )
            MtpAbPassResult(
                label = label,
                loadMs = loadMs,
                firstTokenMs = firstTokenMs,
                elapsedMs = elapsedMs,
                callbacks = callbacks,
                chars = output.length,
            )
        } catch (t: Throwable) {
            val elapsedMs = SystemClock.elapsedRealtime() - started
            mtpAbError("PASS_FAILED runId=$runId label=$label elapsedMs=$elapsedMs ${t::class.simpleName}: ${t.message}", t)
            MtpAbPassResult(
                label = label,
                loadMs = elapsedMs,
                firstTokenMs = firstTokenMs,
                elapsedMs = elapsedMs,
                callbacks = callbacks,
                chars = chars,
                error = t.message ?: t::class.simpleName,
            )
        } finally {
            engine.unload()
        }
    }
}

private data class MtpAbPassResult(
    val label: String,
    val loadMs: Long,
    val firstTokenMs: Long?,
    val elapsedMs: Long,
    val callbacks: Int,
    val chars: Int,
    val error: String? = null,
)

private enum class MtpAbBackendMode(
    val logName: String,
    val accelerator: ChatRuntimeAccelerator,
    val preferAccelerated: Boolean,
) {
    CPU("cpu", ChatRuntimeAccelerator.CPU, false),
    VULKAN("vulkan", ChatRuntimeAccelerator.VULKAN, true);

    companion object {
        fun from(value: String?): MtpAbBackendMode =
            when (value?.trim()?.lowercase()) {
                "vulkan", "gpu", "accelerated" -> VULKAN
                else -> CPU
            }
    }
}

private fun String.previewForLog(maxChars: Int = 180): String =
    replace(Regex("\\s+"), " ")
        .trim()
        .let { if (it.length <= maxChars) it else it.take(maxChars) + "..." }
