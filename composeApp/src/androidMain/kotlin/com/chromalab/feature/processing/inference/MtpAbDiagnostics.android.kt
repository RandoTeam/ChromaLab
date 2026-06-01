package com.chromalab.feature.processing.inference

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.chromalab.feature.chat.ChatMtpRuntimeProfile
import com.chromalab.feature.chat.ChatRuntimeAccelerator
import com.chromalab.feature.processing.debug.StructuredRuntimeDiagnosticMapper
import com.chromalab.feature.processing.model.DownloadedModel
import com.chromalab.feature.processing.model.ModelManager
import com.chromalab.feature.processing.model.ModelRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MTP_AB_TAG = "ChromaLabMtpAB"
private const val DEFAULT_MTP_AB_TIMEOUT_MS = 180_000L

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
    private val exporter = MtpAbBenchmarkArtifactExporter(appContext)

    suspend fun run(
        requestedModelId: String?,
        requestedPrompt: String?,
        requestedBackend: String?,
        requestedDraftTokens: Int?,
        requestedContextTokens: Int?,
        requestedBatchTokens: Int?,
        requestedMaxTokens: Int?,
    ): GgufMtpBenchmarkSummary? = withContext(Dispatchers.IO) {
        val runId = System.currentTimeMillis().toString()
        val backendMode = MtpAbBackendMode.from(requestedBackend)
        val prompt = requestedPrompt?.takeIf { it.isNotBlank() } ?: "Reply with exactly OK."
        val acceleratedBackends = LlamaEngine.availableBackendCodesForDiagnostics().toSet()
        if (backendMode.preferAccelerated && 1 !in acceleratedBackends) {
            return@withContext exportAbortSummary(
                runId = runId,
                requestedModelId = requestedModelId,
                backendMode = backendMode,
                promptChars = prompt.length,
                reason = "vulkan_preflight_failed_no_accelerated_backend",
            )
        }

        val model = resolveModel(runId, requestedModelId)
        if (model == null) {
            return@withContext exportAbortSummary(
                runId = runId,
                requestedModelId = requestedModelId,
                backendMode = backendMode,
                promptChars = prompt.length,
                reason = "no_downloaded_mtp_gguf_model",
            )
        }
        if (!model.info.supportsMtp) {
            return@withContext exportAbortSummary(
                runId = runId,
                requestedModelId = model.info.id,
                backendMode = backendMode,
                promptChars = prompt.length,
                reason = "model_does_not_advertise_mtp",
            )
        }

        VlmEngineHolder.activeEngine?.unload()
        VlmEngineHolder.activeEngine = null
        VlmEngineHolder.activeConfig = null
        VlmEngineHolder.selectedModel = null
        VlmEngineHolder.executedModel = null

        val conservative = manager.isConservativeDevice()
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
        val effectiveDraftTokens = resolveDiagnosticDraftTokens(
            model = model,
            requestedDraftTokens = requestedDraftTokens,
            backendMode = backendMode,
            conservative = conservative,
        )
        val maxTokens = requestedMaxTokens?.coerceIn(4, 256) ?: 32

        mtpAbLog(
            "START runId=$runId model=${model.info.id} backend=${backendMode.logName} " +
                "threads=${manager.threadCount} ctx=$contextTokens batch=$batchTokens " +
                "draft=$effectiveDraftTokens requestedDraft=${requestedDraftTokens ?: 0} " +
                "profileDraftLimit=${ChatMtpRuntimeProfile.maxDraftTokens(backendMode.accelerator, conservative)} " +
                "maxTokens=$maxTokens conservative=$conservative promptChars=${prompt.length}",
        )

        val baseline = runProbePass(
            runId = runId,
            label = "no_mtp",
            mode = GgufMtpBenchmarkMode.NO_MTP,
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
            label = "mtp_draft_$effectiveDraftTokens",
            mode = GgufMtpBenchmarkMode.DRAFT_MTP,
            model = model,
            prompt = prompt,
            contextTokens = contextTokens,
            batchTokens = batchTokens,
            mtpDraftTokens = effectiveDraftTokens,
            maxTokens = maxTokens,
            backendMode = backendMode,
        )
        val summary = GgufMtpBenchmarkSummary(
            runId = runId,
            generatedAtEpochMillis = System.currentTimeMillis(),
            modelId = model.info.id,
            backend = backendMode.benchmarkBackend,
            promptChars = prompt.length,
            modelSupportsMtp = model.info.supportsMtp,
            passes = listOf(baseline, mtp),
            gate = GgufMtpBenchmarkGateEvaluator.evaluate(listOf(baseline, mtp)),
            notes = listOf(
                "GGUF MTP benchmark is text-only.",
                "MTP remains disallowed for mmproj vision and strict chromatogram numeric analysis.",
            ),
        )
        exportAndLog(summary)
        summary
    }

    private fun exportAbortSummary(
        runId: String,
        requestedModelId: String?,
        backendMode: MtpAbBackendMode,
        promptChars: Int,
        reason: String,
    ): GgufMtpBenchmarkSummary {
        mtpAbError("ABORT runId=$runId reason=$reason model=${requestedModelId.orEmpty()}")
        val summary = GgufMtpBenchmarkSummary(
            runId = runId,
            generatedAtEpochMillis = System.currentTimeMillis(),
            modelId = requestedModelId,
            backend = backendMode.benchmarkBackend,
            promptChars = promptChars,
            modelSupportsMtp = false,
            passes = emptyList(),
            gate = GgufMtpBenchmarkGate(
                decision = GgufMtpGateDecision.INCONCLUSIVE,
                verdict = "INCONCLUSIVE",
                reasons = listOf(reason),
            ),
            notes = listOf("Benchmark did not run because preconditions failed."),
        )
        exportAndLog(summary)
        return summary
    }

    private fun exportAndLog(summary: GgufMtpBenchmarkSummary) {
        val exports = exporter.export(summary)
        val structuredDiagnostics = StructuredRuntimeDiagnosticMapper.fromGgufMtpBenchmark(summary)
        mtpAbLog(
            "SUMMARY runId=${summary.runId} verdict=${summary.gate.verdict} " +
                "decision=${summary.gate.decision} speedup=${summary.gate.speedup ?: 0.0} " +
                "firstTokenSlowdown=${summary.gate.firstTokenSlowdown ?: 0.0} " +
                "structuredDiagnostics=${structuredDiagnostics.size}",
        )
        exports.forEach { record ->
            mtpAbLog(
                "EXPORT runId=${summary.runId} file=${record.fileName} " +
                    "success=${record.success} path=${record.uriOrPath.orEmpty()} message=${record.message}",
            )
        }
        mtpAbLog("DONE runId=${summary.runId}")
    }

    private fun resolveModel(runId: String, requestedModelId: String?): DownloadedModel? {
        val downloaded = manager.getDownloadedModels()
        val requestedId = requestedModelId?.takeIf { it.isNotBlank() }
        if (requestedId != null) {
            val requested = downloaded.find { it.info.id == requestedId }
            if (requested == null) {
                mtpAbError("ABORT runId=$runId reason=requested_model_not_downloaded model=$requestedId")
                return null
            }
            if (requested.info.runtime != ModelRuntime.LLAMA_CPP) {
                mtpAbError("ABORT runId=$runId reason=requested_model_not_llama_cpp model=$requestedId")
                return null
            }
            return requested
        }

        val active = manager.getActiveModelId()
            ?.let { id -> downloaded.find { it.info.id == id } }
        if (active?.info?.runtime == ModelRuntime.LLAMA_CPP && active.info.supportsMtp) return active

        return downloaded.firstOrNull {
            it.info.runtime == ModelRuntime.LLAMA_CPP &&
                it.info.supportsMtp &&
                ModelRegistry.isChatModel(it.info)
        }
    }

    private fun resolveDiagnosticDraftTokens(
        model: DownloadedModel,
        requestedDraftTokens: Int?,
        backendMode: MtpAbBackendMode,
        conservative: Boolean,
    ): Int {
        val modelMax = model.info.maxMtpDraftTokens.coerceAtLeast(1)
        val requested = requestedDraftTokens?.takeIf { it > 0 }
        val draftTokens = if (requested != null) {
            requested.coerceIn(1, modelMax)
        } else {
            ChatMtpRuntimeProfile.coerceDraftTokens(
                requestedDraftTokens = model.info.defaultMtpDraftTokens.coerceAtLeast(1),
                selectedAccelerator = backendMode.accelerator,
                isConservativeDevice = conservative,
            )
        }
        return GgufMtpSafetyPolicy.resolveLoadPolicy(
            supportsMtp = model.info.supportsMtp,
            requestedDraftTokens = draftTokens,
            hasVisionProjector = false,
            strictChromatogramAnalysis = false,
        ).effectiveDraftTokens
    }

    private suspend fun runProbePass(
        runId: String,
        label: String,
        mode: GgufMtpBenchmarkMode,
        model: DownloadedModel,
        prompt: String,
        contextTokens: Int,
        batchTokens: Int,
        mtpDraftTokens: Int,
        maxTokens: Int,
        backendMode: MtpAbBackendMode,
    ): GgufMtpBenchmarkPass {
        val engine = LlamaEngine()
        val loadStarted = SystemClock.elapsedRealtime()
        var firstTokenMs: Long? = null
        var callbacks = 0
        var callbackChars = 0
        var generatedTokens: Int? = null
        val modelPathClass = StructuredRuntimeDiagnosticMapper.classifyModelPath(model.primaryPath).name
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
            val loadMs = SystemClock.elapsedRealtime() - loadStarted
            mtpAbLog("PASS_LOAD_DONE runId=$runId label=$label loadMs=$loadMs backend=${engine.getBackendName()}")
            val inferStarted = SystemClock.elapsedRealtime()
            val output = engine.inferTextOnlyForDiagnostics(
                prompt = "User: $prompt\nAssistant:",
                options = GenerationOptions(
                    maxTokens = maxTokens,
                    temperature = 0.15f,
                    topP = 0.95f,
                    topK = 40,
                    repeatPenalty = 1.05f,
                    repeatLastN = 128,
                    timeoutMs = DEFAULT_MTP_AB_TIMEOUT_MS,
                ),
                onNativeToken = { chunk, tokenCount, _ ->
                    callbacks += 1
                    callbackChars += chunk.length
                    generatedTokens = tokenCount
                    if (firstTokenMs == null) {
                        firstTokenMs = SystemClock.elapsedRealtime() - inferStarted
                        mtpAbLog("PASS_FIRST_TOKEN runId=$runId label=$label firstTokenMs=$firstTokenMs tokenCount=$tokenCount")
                    }
                },
            )
            val elapsedMs = SystemClock.elapsedRealtime() - inferStarted
            val tokensPerSecond = generatedTokens
                ?.takeIf { it > 0 && elapsedMs > 0 }
                ?.let { it * 1000.0 / elapsedMs.toDouble() }
            val missing = buildList {
                add("prompt_tokens_not_exported_by_bridge")
                if (generatedTokens == null) add("generated_tokens_not_exported_by_bridge")
                if (mtpDraftTokens > 0) {
                    add("drafted_tokens_not_exported_by_bridge")
                    add("accepted_tokens_not_exported_by_bridge")
                    add("acceptance_rate_not_exported_by_bridge")
                }
            }
            mtpAbLog(
                "PASS_DONE runId=$runId label=$label loadMs=$loadMs firstTokenMs=$firstTokenMs " +
                    "elapsedMs=$elapsedMs callbacks=$callbacks generatedTokens=${generatedTokens ?: 0} " +
                    "chars=${output.length} tps=${tokensPerSecond ?: 0.0} preview=${output.previewForLog()}",
            )
            GgufMtpBenchmarkPass(
                label = label,
                mode = mode,
                backend = backendMode.benchmarkBackend,
                modelId = model.info.id,
                modelPathClass = modelPathClass,
                contextTokens = contextTokens,
                batchTokens = batchTokens,
                maxTokens = maxTokens,
                mtpDraftTokens = mtpDraftTokens,
                generatedTokens = generatedTokens,
                firstTokenLatencyMillis = firstTokenMs,
                loadTimeMillis = loadMs,
                totalResponseDurationMillis = elapsedMs,
                totalTokensPerSecond = tokensPerSecond,
                tokenCallbackCount = callbacks,
                outputChars = output.length,
                timeoutMillis = DEFAULT_MTP_AB_TIMEOUT_MS,
                missingMetricReasons = missing,
            )
        } catch (t: Throwable) {
            val elapsedMs = SystemClock.elapsedRealtime() - loadStarted
            val message = t.message ?: t::class.simpleName ?: "unknown_error"
            mtpAbError("PASS_FAILED runId=$runId label=$label elapsedMs=$elapsedMs ${t::class.simpleName}: $message", t)
            GgufMtpBenchmarkPass(
                label = label,
                mode = mode,
                backend = backendMode.benchmarkBackend,
                modelId = model.info.id,
                modelPathClass = modelPathClass,
                contextTokens = contextTokens,
                batchTokens = batchTokens,
                maxTokens = maxTokens,
                mtpDraftTokens = mtpDraftTokens,
                generatedTokens = generatedTokens,
                firstTokenLatencyMillis = firstTokenMs,
                loadTimeMillis = elapsedMs,
                totalResponseDurationMillis = elapsedMs,
                tokenCallbackCount = callbacks,
                outputChars = callbackChars,
                timedOut = message.contains("timeout", ignoreCase = true),
                timeoutMillis = DEFAULT_MTP_AB_TIMEOUT_MS,
                failureReason = message,
            )
        } finally {
            engine.unload()
        }
    }
}

private enum class MtpAbBackendMode(
    val logName: String,
    val accelerator: ChatRuntimeAccelerator,
    val benchmarkBackend: GgufMtpBenchmarkBackend,
    val preferAccelerated: Boolean,
) {
    CPU("cpu", ChatRuntimeAccelerator.CPU, GgufMtpBenchmarkBackend.CPU, false),
    VULKAN("vulkan", ChatRuntimeAccelerator.VULKAN, GgufMtpBenchmarkBackend.VULKAN, true);

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
