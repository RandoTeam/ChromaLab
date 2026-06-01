package com.chromalab.feature.processing.inference

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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

private const val VULKAN_MATRIX_TAG = "ChromaLabVulkanMatrix"
private const val DEFAULT_VULKAN_MATRIX_TIMEOUT_MS = 180_000L

private fun vulkanMatrixLog(message: String) {
    Log.i(VULKAN_MATRIX_TAG, message)
}

private fun vulkanMatrixError(message: String, throwable: Throwable? = null) {
    Log.e(VULKAN_MATRIX_TAG, message, throwable)
}

class GgufVulkanMatrixDiagnostics(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val manager = ModelManager(appContext)
    private val exporter = GgufVulkanMatrixArtifactExporter(appContext)

    suspend fun run(
        requestedModelId: String?,
        requestedPrompt: String?,
        requestedContextTokens: Int?,
        requestedBatchTokens: Int?,
        requestedMaxTokens: Int?,
    ): GgufVulkanMatrixSummary = withContext(Dispatchers.IO) {
        val runId = System.currentTimeMillis().toString()
        val prompt = requestedPrompt?.takeIf { it.isNotBlank() } ?: "Reply with exactly OK."
        val preflight = buildPreflight()
        val model = resolveModel(runId, requestedModelId)
        if (model == null) {
            return@withContext exportAbortSummary(
                runId = runId,
                requestedModelId = requestedModelId,
                promptChars = prompt.length,
                preflight = preflight,
                reason = "no_downloaded_text_only_gguf_model",
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
        val maxTokens = requestedMaxTokens?.coerceIn(4, 256) ?: 32

        vulkanMatrixLog(
            "START runId=$runId model=${model.info.id} ctx=$contextTokens batch=$batchTokens " +
                "maxTokens=$maxTokens acceleratedAvailable=${preflight.acceleratedBackendAvailable} " +
                "device=${preflight.deviceName}",
        )

        val passes = listOf(
            runProfile(
                runId = runId,
                profile = GgufVulkanMatrixProfile.CPU,
                model = model,
                prompt = prompt,
                contextTokens = contextTokens,
                batchTokens = batchTokens,
                maxTokens = maxTokens,
                preferAccelerated = false,
                requestedBackend = "cpu",
                fallbackReason = null,
                shouldRun = true,
            ),
            runProfile(
                runId = runId,
                profile = GgufVulkanMatrixProfile.EXPLICIT_VULKAN,
                model = model,
                prompt = prompt,
                contextTokens = contextTokens,
                batchTokens = batchTokens,
                maxTokens = maxTokens,
                preferAccelerated = true,
                requestedBackend = "vulkan",
                fallbackReason = if (preflight.acceleratedBackendAvailable) null else "explicit_vulkan_skipped_preflight_failed",
                shouldRun = preflight.acceleratedBackendAvailable,
            ),
            runProfile(
                runId = runId,
                profile = GgufVulkanMatrixProfile.AUTO,
                model = model,
                prompt = prompt,
                contextTokens = contextTokens,
                batchTokens = batchTokens,
                maxTokens = maxTokens,
                preferAccelerated = preflight.acceleratedBackendAvailable,
                requestedBackend = "auto",
                fallbackReason = if (preflight.acceleratedBackendAvailable) null else "auto_fell_back_to_cpu_no_accelerated_backend",
                shouldRun = true,
            ),
        )

        val summary = GgufVulkanMatrixSummary(
            runId = runId,
            generatedAtEpochMillis = System.currentTimeMillis(),
            modelId = model.info.id,
            promptChars = prompt.length,
            preflight = preflight,
            passes = passes,
            gate = GgufVulkanMatrixGateEvaluator.evaluate(preflight, passes),
            notes = listOf(
                "Matrix is text-only GGUF.",
                "Explicit Vulkan is skipped when preflight does not expose an accelerated llama.cpp backend.",
                "Graph analysis and chromatographic math are not changed by this diagnostic.",
            ),
        )
        exportAndLog(summary)
        summary
    }

    private fun buildPreflight(): GgufVulkanPreflightEvidence {
        val backendCodes = LlamaEngine.availableBackendCodesForDiagnostics().toList()
        val accelerated = 1 in backendCodes
        val packageManager = appContext.packageManager
        val deviceName = listOfNotNull(
            Build.MANUFACTURER?.takeIf { it.isNotBlank() },
            Build.MODEL?.takeIf { it.isNotBlank() },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL?.takeIf { it.isNotBlank() } else null,
        ).joinToString(" ").ifBlank { "unknown_android_device" }
        val fallbackReason = if (accelerated) null else "no_ggml_accelerated_backend_reported"
        return GgufVulkanPreflightEvidence(
            deviceName = deviceName,
            sdkInt = Build.VERSION.SDK_INT,
            availableBackendCodes = backendCodes,
            acceleratedBackendAvailable = accelerated,
            requiredFeatureFlags = listOf(
                PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL,
                PackageManager.FEATURE_VULKAN_HARDWARE_VERSION,
                PackageManager.FEATURE_VULKAN_DEQP_LEVEL,
            ),
            vulkanHardwareLevelAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL),
            vulkanHardwareVersionAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION),
            vulkanDeqpLevelAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_DEQP_LEVEL),
            selectedBackendHint = if (accelerated) "explicit_vulkan_and_auto_can_try_accelerated" else "cpu_only",
            fallbackReason = fallbackReason,
        )
    }

    private fun exportAbortSummary(
        runId: String,
        requestedModelId: String?,
        promptChars: Int,
        preflight: GgufVulkanPreflightEvidence,
        reason: String,
    ): GgufVulkanMatrixSummary {
        vulkanMatrixError("ABORT runId=$runId reason=$reason model=${requestedModelId.orEmpty()}")
        val summary = GgufVulkanMatrixSummary(
            runId = runId,
            generatedAtEpochMillis = System.currentTimeMillis(),
            modelId = requestedModelId,
            promptChars = promptChars,
            preflight = preflight,
            passes = emptyList(),
            gate = GgufVulkanMatrixGate(
                decision = GgufVulkanMatrixDecision.INCONCLUSIVE,
                verdict = "INCONCLUSIVE",
                reasons = listOf(reason),
            ),
            notes = listOf("Matrix did not run because preconditions failed."),
        )
        exportAndLog(summary)
        return summary
    }

    private fun exportAndLog(summary: GgufVulkanMatrixSummary) {
        val exports = exporter.export(summary)
        val structuredDiagnostics = StructuredRuntimeDiagnosticMapper.fromGgufVulkanMatrix(summary)
        vulkanMatrixLog(
            "SUMMARY runId=${summary.runId} verdict=${summary.gate.verdict} " +
                "decision=${summary.gate.decision} explicitSpeedup=${summary.gate.explicitVulkanSpeedup ?: 0.0} " +
                "autoSpeedup=${summary.gate.autoSpeedup ?: 0.0} structuredDiagnostics=${structuredDiagnostics.size}",
        )
        exports.forEach { record ->
            vulkanMatrixLog(
                "EXPORT runId=${summary.runId} file=${record.fileName} " +
                    "success=${record.success} path=${record.uriOrPath.orEmpty()} message=${record.message}",
            )
        }
        vulkanMatrixLog("DONE runId=${summary.runId}")
    }

    private fun resolveModel(runId: String, requestedModelId: String?): DownloadedModel? {
        val downloaded = manager.getDownloadedModels()
        val requestedId = requestedModelId?.takeIf { it.isNotBlank() }
        if (requestedId != null) {
            val requested = downloaded.find { it.info.id == requestedId }
            if (requested == null) {
                vulkanMatrixError("ABORT runId=$runId reason=requested_model_not_downloaded model=$requestedId")
                return null
            }
            if (requested.info.runtime != ModelRuntime.LLAMA_CPP) {
                vulkanMatrixError("ABORT runId=$runId reason=requested_model_not_llama_cpp model=$requestedId")
                return null
            }
            return requested
        }

        val active = manager.getActiveModelId()
            ?.let { id -> downloaded.find { it.info.id == id } }
        if (active?.info?.runtime == ModelRuntime.LLAMA_CPP && ModelRegistry.isChatModel(active.info)) return active

        return downloaded.firstOrNull {
            it.info.runtime == ModelRuntime.LLAMA_CPP &&
                ModelRegistry.isChatModel(it.info)
        }
    }

    private suspend fun runProfile(
        runId: String,
        profile: GgufVulkanMatrixProfile,
        model: DownloadedModel,
        prompt: String,
        contextTokens: Int,
        batchTokens: Int,
        maxTokens: Int,
        preferAccelerated: Boolean,
        requestedBackend: String,
        fallbackReason: String?,
        shouldRun: Boolean,
    ): GgufVulkanMatrixPass {
        val modelPathClass = StructuredRuntimeDiagnosticMapper.classifyModelPath(model.primaryPath).name
        if (!shouldRun) {
            vulkanMatrixLog("PASS_SKIPPED runId=$runId profile=$profile fallback=$fallbackReason")
            return GgufVulkanMatrixPass(
                profile = profile,
                modelId = model.info.id,
                modelPathClass = modelPathClass,
                contextTokens = contextTokens,
                batchTokens = batchTokens,
                maxTokens = maxTokens,
                requestedBackend = requestedBackend,
                selectedBackend = null,
                preferAccelerated = preferAccelerated,
                loadAttempted = false,
                timeoutMillis = DEFAULT_VULKAN_MATRIX_TIMEOUT_MS,
                fallbackReason = fallbackReason,
            )
        }

        val engine = LlamaEngine()
        val loadStarted = SystemClock.elapsedRealtime()
        var firstTokenMs: Long? = null
        var callbacks = 0
        var callbackChars = 0
        var generatedTokens: Int? = null
        return try {
            vulkanMatrixLog("PASS_START runId=$runId profile=$profile preferAccelerated=$preferAccelerated")
            engine.loadModel(
                basePath = model.primaryPath,
                mmprojPath = "",
                threads = manager.threadCount,
                modelFamily = model.info.family,
                contextSize = contextTokens,
                batchSize = batchTokens,
                preferAccelerated = preferAccelerated,
                mtpDraftTokens = 0,
            )
            val loadMs = SystemClock.elapsedRealtime() - loadStarted
            val selectedBackend = engine.getBackendName()
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
                    timeoutMs = DEFAULT_VULKAN_MATRIX_TIMEOUT_MS,
                ),
                onNativeToken = { chunk, tokenCount, _ ->
                    callbacks += 1
                    callbackChars += chunk.length
                    generatedTokens = tokenCount
                    if (firstTokenMs == null) {
                        firstTokenMs = SystemClock.elapsedRealtime() - inferStarted
                    }
                },
            )
            val elapsedMs = SystemClock.elapsedRealtime() - inferStarted
            val tokensPerSecond = generatedTokens
                ?.takeIf { it > 0 && elapsedMs > 0 }
                ?.let { it * 1000.0 / elapsedMs.toDouble() }
            vulkanMatrixLog(
                "PASS_DONE runId=$runId profile=$profile backend=$selectedBackend loadMs=$loadMs " +
                    "firstTokenMs=$firstTokenMs elapsedMs=$elapsedMs generatedTokens=${generatedTokens ?: 0} " +
                    "tps=${tokensPerSecond ?: 0.0}",
            )
            GgufVulkanMatrixPass(
                profile = profile,
                modelId = model.info.id,
                modelPathClass = modelPathClass,
                contextTokens = contextTokens,
                batchTokens = batchTokens,
                maxTokens = maxTokens,
                requestedBackend = requestedBackend,
                selectedBackend = selectedBackend,
                preferAccelerated = preferAccelerated,
                loadAttempted = true,
                loadTimeMillis = loadMs,
                firstTokenLatencyMillis = firstTokenMs,
                totalResponseDurationMillis = elapsedMs,
                generatedTokens = generatedTokens,
                totalTokensPerSecond = tokensPerSecond,
                tokenCallbackCount = callbacks,
                outputChars = output.length,
                timeoutMillis = DEFAULT_VULKAN_MATRIX_TIMEOUT_MS,
                fallbackReason = fallbackReason,
                missingMetricReasons = buildList {
                    add("prompt_tokens_not_exported_by_bridge")
                    if (generatedTokens == null) add("generated_tokens_not_exported_by_bridge")
                },
            )
        } catch (t: Throwable) {
            val elapsedMs = SystemClock.elapsedRealtime() - loadStarted
            val message = t.message ?: t::class.simpleName ?: "unknown_error"
            vulkanMatrixError("PASS_FAILED runId=$runId profile=$profile elapsedMs=$elapsedMs $message", t)
            GgufVulkanMatrixPass(
                profile = profile,
                modelId = model.info.id,
                modelPathClass = modelPathClass,
                contextTokens = contextTokens,
                batchTokens = batchTokens,
                maxTokens = maxTokens,
                requestedBackend = requestedBackend,
                selectedBackend = engine.getBackendName(),
                preferAccelerated = preferAccelerated,
                loadAttempted = true,
                loadTimeMillis = elapsedMs,
                firstTokenLatencyMillis = firstTokenMs,
                totalResponseDurationMillis = elapsedMs,
                generatedTokens = generatedTokens,
                tokenCallbackCount = callbacks,
                outputChars = callbackChars,
                timeoutMillis = DEFAULT_VULKAN_MATRIX_TIMEOUT_MS,
                timedOut = message.contains("timeout", ignoreCase = true),
                fallbackReason = fallbackReason,
                failureReason = message,
            )
        } finally {
            engine.unload()
        }
    }
}
