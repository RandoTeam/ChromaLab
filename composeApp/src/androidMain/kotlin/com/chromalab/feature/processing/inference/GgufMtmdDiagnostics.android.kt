package com.chromalab.feature.processing.inference

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.chromalab.feature.processing.debug.StructuredRuntimeDiagnosticMapper
import com.chromalab.feature.processing.model.DownloadedModel
import com.chromalab.feature.processing.model.ModelFileType
import com.chromalab.feature.processing.model.ModelManager
import com.chromalab.feature.processing.model.ModelRegistry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val MTMD_DIAGNOSTICS_TAG = "ChromaLabMtmdDiag"
private const val DEFAULT_MTMD_PROMPT = "Read only visible text in this crop. Do not infer chromatographic metrics."
private const val DEFAULT_MTMD_FIXTURE_ASSET = "validation/white_tiger_ion71_fixture.jpg"

private fun mtmdLog(message: String) {
    Log.i(MTMD_DIAGNOSTICS_TAG, message)
}

private fun mtmdError(message: String, throwable: Throwable? = null) {
    Log.e(MTMD_DIAGNOSTICS_TAG, message, throwable)
}

class GgufMtmdDiagnostics(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val manager = ModelManager(appContext)
    private val exporter = GgufMtmdDiagnosticsArtifactExporter(appContext)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun run(
        requestedModelId: String?,
        requestedImagePath: String?,
        requestedBackend: String?,
        requestedRunOcrProbe: Boolean,
    ): GgufMtmdDiagnosticsSummary = withContext(Dispatchers.IO) {
        val runId = System.currentTimeMillis().toString()
        val backendMode = MtmdDiagnosticsBackendMode.from(requestedBackend)
        val prompt = DEFAULT_MTMD_PROMPT
        mtmdLog(
            "START runId=$runId modelId=${requestedModelId.orEmpty()} " +
                "backend=${backendMode.logName} imagePathProvided=${requestedImagePath?.isNotBlank() == true} " +
                "ocrProbe=$requestedRunOcrProbe",
        )

        val model = resolveModel(requestedModelId)
        if (model == null) {
            return@withContext exportAndLog(
                abortSummary(
                    runId = runId,
                    modelId = requestedModelId,
                    reason = "no_downloaded_gguf_vision_model",
                ),
            )
        }
        if (!ModelRegistry.hasGgufVisionFilePair(model.info)) {
            return@withContext exportAndLog(
                abortSummary(
                    runId = runId,
                    modelId = model.info.id,
                    reason = "model_missing_gguf_base_mmproj_pair",
                    model = model,
                ),
            )
        }

        val imageFile = resolveImage(runId, requestedImagePath)
        val contextTokens = manager.llamaContextSize(model.info, forVision = true)
        val batchTokens = manager.llamaBatchSize(model.info, forVision = true)
        val visionPackage = runCatching { manager.requireGgufVisionPackage(model) }
            .getOrElse { error ->
                return@withContext exportAndLog(
                    abortSummary(
                        runId = runId,
                        modelId = model.info.id,
                        reason = "vision_package_invalid:${error.message ?: "unknown"}",
                        model = model,
                        imagePathClass = StructuredRuntimeDiagnosticMapper.classifyModelPath(imageFile?.absolutePath).name,
                    ),
                )
            }

        val engine = LlamaEngine()
        val loadStarted = SystemClock.elapsedRealtime()
        var loadResult = "loaded"
        var loadTimeMs: Long? = null
        var backend: String? = null
        var nativeProbe: GgufMtmdNativeProbeResult? = null
        var cropProbe: GgufMtmdCropOcrProbe? = null
        try {
            engine.loadModel(
                basePath = visionPackage.basePath,
                mmprojPath = visionPackage.mmprojPath,
                threads = manager.threadCount,
                modelFamily = model.info.family,
                contextSize = contextTokens,
                batchSize = batchTokens,
                preferAccelerated = backendMode.preferAccelerated,
            )
            loadTimeMs = SystemClock.elapsedRealtime() - loadStarted
            backend = engine.getBackendName()
            mtmdLog("LOAD_DONE runId=$runId model=${model.info.id} backend=$backend loadMs=$loadTimeMs")

            nativeProbe = if (imageFile != null) {
                decodeNativeProbe(
                    engine.probeMtmdDiagnostics(
                        imagePath = imageFile.absolutePath,
                        prompt = prompt,
                    ),
                )
            } else {
                GgufMtmdNativeProbeResult(
                    available = true,
                    contextTokens = contextTokens,
                    batchTokens = batchTokens,
                    error = "image_probe_skipped_no_image",
                )
            }

            cropProbe = if (requestedRunOcrProbe && imageFile != null) {
                runCropOcrProbe(engine, imageFile, prompt)
            } else {
                GgufMtmdCropOcrProbe(
                    attempted = false,
                    failureReason = if (imageFile == null) "skipped_no_image" else "skipped_not_requested",
                )
            }
        } catch (t: Throwable) {
            loadResult = t.message ?: t::class.simpleName ?: "unknown_error"
            mtmdError("FAILED runId=$runId model=${model.info.id}: $loadResult", t)
            if (cropProbe == null) {
                cropProbe = GgufMtmdCropOcrProbe(
                    attempted = false,
                    failureReason = loadResult,
                )
            }
        } finally {
            engine.unload()
        }

        val gate = ocrResearchGate(model)
        val summary = GgufMtmdDiagnosticsSummary(
            runId = runId,
            generatedAtEpochMillis = System.currentTimeMillis(),
            modelId = model.info.id,
            modelFamily = model.info.family,
            backend = backend,
            contextTokens = contextTokens,
            batchTokens = batchTokens,
            imagePathClass = StructuredRuntimeDiagnosticMapper.classifyModelPath(imageFile?.absolutePath).name,
            promptChars = prompt.length,
            loadAttempted = true,
            loadResult = loadResult,
            loadTimeMillis = loadTimeMs,
            baseModel = fileDiagnostic("base", model, ModelFileType.GGUF_BASE),
            mmproj = fileDiagnostic("mmproj", model, ModelFileType.GGUF_MMPROJ),
            nativeProbe = nativeProbe,
            cropOcrProbe = cropProbe,
            ocrResearchGate = gate,
            gateDecision = GgufMtmdDiagnosticsGateEvaluator.evaluate(
                GgufMtmdDiagnosticsSummary(
                    runId = runId,
                    generatedAtEpochMillis = System.currentTimeMillis(),
                    modelId = model.info.id,
                    loadAttempted = true,
                    loadResult = loadResult,
                    nativeProbe = nativeProbe,
                    ocrResearchGate = gate,
                    gateDecision = gate.decision,
                    gateReasons = emptyList(),
                ),
            ),
            gateReasons = buildList {
                if (nativeProbe?.available != true) add(nativeProbe?.error ?: "native_probe_not_available")
                if (cropProbe.forbiddenNumericFieldDetected) add("ocr_output_contains_forbidden_numeric_authority_fields")
                if (gate.decision != GgufMtmdResearchGateDecision.ADVISORY_DIAGNOSTICS_ALLOWED) {
                    add("deepseek_ocr2_research_gate_not_production_ready")
                }
            },
            notes = listOf(
                "mtmd diagnostics are debug-only and exported as TECHNICAL_EVIDENCE.",
                "OCR text is advisory only and cannot create pixel geometry, calibration, or peak metrics.",
            ),
        )
        exportAndLog(summary)
    }

    private fun resolveModel(requestedModelId: String?): DownloadedModel? {
        val downloaded = manager.getDownloadedModels()
        val requested = requestedModelId
            ?.takeIf { it.isNotBlank() }
            ?.let { id -> downloaded.find { it.info.id == id } }
        if (requested != null) return requested

        val active = manager.getActiveModelId()
            ?.let { id -> downloaded.find { it.info.id == id } }
        if (active?.info?.runtime == ModelRuntime.LLAMA_CPP && ModelRegistry.hasGgufVisionFilePair(active.info)) {
            return active
        }

        return downloaded.firstOrNull { model ->
            model.info.runtime == ModelRuntime.LLAMA_CPP &&
                ModelRegistry.hasGgufVisionFilePair(model.info) &&
                model.info.family.contains("deepseek-ocr", ignoreCase = true)
        } ?: downloaded.firstOrNull { model ->
            model.info.runtime == ModelRuntime.LLAMA_CPP &&
                ModelRegistry.hasGgufVisionFilePair(model.info)
        }
    }

    private fun resolveImage(runId: String, requestedImagePath: String?): File? {
        requestedImagePath
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.isFile && it.length() > 0L }
            ?.let { return it }

        return runCatching {
            val dir = File(appContext.cacheDir, "mtmd-diagnostics/$runId").apply { mkdirs() }
            val file = File(dir, DEFAULT_MTMD_FIXTURE_ASSET.substringAfterLast('/'))
            appContext.assets.open(DEFAULT_MTMD_FIXTURE_ASSET).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        }.getOrElse { error ->
            mtmdError("Default mtmd fixture unavailable: ${error.message}", error)
            null
        }
    }

    private fun decodeNativeProbe(rawJson: String): GgufMtmdNativeProbeResult =
        runCatching {
            json.decodeFromString(GgufMtmdNativeProbeResult.serializer(), rawJson)
        }.getOrElse { error ->
            GgufMtmdNativeProbeResult(
                available = false,
                error = "native_probe_json_decode_failed:${error.message ?: "unknown"}",
            )
        }

    private suspend fun runCropOcrProbe(
        engine: LlamaEngine,
        imageFile: File,
        prompt: String,
    ): GgufMtmdCropOcrProbe {
        val started = SystemClock.elapsedRealtime()
        return try {
            val output = engine.inferRaw(
                imagePath = imageFile.absolutePath,
                prompt = prompt,
                options = GenerationOptions(
                    maxTokens = 64,
                    temperature = 0f,
                    topP = 1f,
                    topK = 0,
                    timeoutMs = 180_000,
                ),
            )
            val policy = GgufMtmdOcrAdvisoryPolicy.evaluate(output)
            GgufMtmdCropOcrProbe(
                attempted = true,
                latencyMillis = SystemClock.elapsedRealtime() - started,
                outputChars = output.length,
                advisoryOnly = policy.advisoryOnly,
                forbiddenNumericFieldDetected = policy.forbiddenNumericFieldDetected,
                forbiddenNumericFields = policy.forbiddenNumericFields,
            )
        } catch (t: Throwable) {
            GgufMtmdCropOcrProbe(
                attempted = true,
                latencyMillis = SystemClock.elapsedRealtime() - started,
                failureReason = t.message ?: t::class.simpleName ?: "unknown_error",
            )
        }
    }

    private fun fileDiagnostic(
        role: String,
        model: DownloadedModel?,
        type: ModelFileType,
    ): GgufMtmdModelFileDiagnostic {
        val spec = model?.info?.files?.firstOrNull { it.type == type }
        val file = if (model != null && spec != null) File(model.localDir, spec.fileName) else null
        return GgufMtmdModelFileDiagnostic(
            role = role,
            fileName = spec?.fileName,
            exists = file?.isFile ?: false,
            sizeBytes = file?.takeIf { it.exists() }?.length() ?: spec?.sizeBytes,
            pathClass = StructuredRuntimeDiagnosticMapper.classifyModelPath(file?.absolutePath).name,
        )
    }

    private fun abortSummary(
        runId: String,
        modelId: String?,
        reason: String,
        model: DownloadedModel? = null,
        imagePathClass: String = "NOT_AVAILABLE",
    ): GgufMtmdDiagnosticsSummary {
        val gate = ocrResearchGate(model)
        return GgufMtmdDiagnosticsSummary(
            runId = runId,
            generatedAtEpochMillis = System.currentTimeMillis(),
            modelId = model?.info?.id ?: modelId,
            modelFamily = model?.info?.family,
            imagePathClass = imagePathClass,
            loadAttempted = false,
            loadResult = reason,
            baseModel = fileDiagnostic("base", model, ModelFileType.GGUF_BASE),
            mmproj = fileDiagnostic("mmproj", model, ModelFileType.GGUF_MMPROJ),
            nativeProbe = GgufMtmdNativeProbeResult(
                available = false,
                error = reason,
            ),
            cropOcrProbe = GgufMtmdCropOcrProbe(
                attempted = false,
                failureReason = reason,
            ),
            ocrResearchGate = gate,
            gateDecision = GgufMtmdResearchGateDecision.BLOCKED_PRECONDITION,
            gateReasons = listOf(reason),
        )
    }

    private fun ocrResearchGate(model: DownloadedModel?): GgufMtmdOcrResearchGate {
        val deepSeekV1 = model?.takeIf { it.info.id == "deepseek-ocr-q80" }
            ?: manager.getDownloadedModels().firstOrNull { it.info.id == "deepseek-ocr-q80" }
        val deepSeekV1Info = ModelRegistry.findById("deepseek-ocr-q80")
        val baseSpec = deepSeekV1Info?.files?.firstOrNull { it.type == ModelFileType.GGUF_BASE }
        val mmprojSpec = deepSeekV1Info?.files?.firstOrNull { it.type == ModelFileType.GGUF_MMPROJ }
        val v1BaseFile = deepSeekV1?.let { File(it.localDir, baseSpec?.fileName.orEmpty()) }
        val v1MmprojFile = deepSeekV1?.let { File(it.localDir, mmprojSpec?.fileName.orEmpty()) }
        val v1Availability = "DeepSeek-OCR v1 registry availability: " +
            "base=${v1BaseFile?.isFile == true}, mmproj=${v1MmprojFile?.isFile == true}."
        return GgufMtmdOcrResearchGate(
            modelId = "deepseek-ocr-2",
            modelAvailable = false,
            mmprojAvailable = false,
            expectedBaseFileName = "deepseek-ocr-2-Q4_K_M.gguf",
            expectedMmprojFileName = "mmproj-deepseek-ocr-2-q8_0.gguf",
            expectedDownloadBytes = null,
            compatibility = "llama.cpp mtmd support merged 2026-05-29; app registry still exposes DeepSeek-OCR v1 only. $v1Availability",
            decision = GgufMtmdResearchGateDecision.RESEARCH_ONLY,
            safetyBoundaries = listOf(
                "OCR text may assist labels and warnings only.",
                "OCR/VLM cannot create graph pixels, calibration coefficients, RT, height, area, FWHM, S/N, baseline, or Kovats values.",
                "DeepSeekOCR 2 requires separate base/mmproj download-size verification and Android smoke tests before registry exposure.",
            ),
            sources = listOf(
                "https://github.com/ggml-org/llama.cpp/pull/20975",
                "https://huggingface.co/deepseek-ai/DeepSeek-OCR-2",
                "https://huggingface.co/sabafallah/DeepSeek-OCR-2-GGUF",
            ),
        )
    }

    private fun exportAndLog(summary: GgufMtmdDiagnosticsSummary): GgufMtmdDiagnosticsSummary {
        val exports = exporter.export(summary)
        val structuredDiagnostic = StructuredRuntimeDiagnosticMapper.fromGgufMtmdDiagnostics(summary)
        mtmdLog(
            "SUMMARY runId=${summary.runId} model=${summary.modelId ?: "none"} " +
                "decision=${summary.gateDecision.name} imageTokens=${summary.nativeProbe?.imageTokenCount ?: 0} " +
                "fit=${summary.nativeProbe?.fitStatus?.name ?: "UNKNOWN"} structured=${structuredDiagnostic.source}",
        )
        exports.forEach { record ->
            mtmdLog(
                "EXPORT runId=${summary.runId} file=${record.fileName} " +
                    "success=${record.success} path=${record.uriOrPath.orEmpty()} message=${record.message}",
            )
        }
        mtmdLog("DONE runId=${summary.runId}")
        return summary
    }
}

private enum class MtmdDiagnosticsBackendMode(
    val logName: String,
    val preferAccelerated: Boolean,
) {
    CPU("cpu", false),
    ACCELERATED("accelerated", true);

    companion object {
        fun from(value: String?): MtmdDiagnosticsBackendMode =
            when (value?.trim()?.lowercase()) {
                "accelerated", "gpu", "vulkan" -> ACCELERATED
                else -> CPU
            }
    }
}
