package com.chromalab.feature.processing.model

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.chromalab.feature.processing.inference.InferenceConfig
import com.chromalab.feature.processing.inference.ModelRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Represents a model that has been downloaded and is available locally.
 */
data class DownloadedModel(
    val info: ModelInfo,
    val localDir: File,
    val isActive: Boolean,
) {
    /** Absolute path to the primary model file. */
    val primaryPath: String get() = File(localDir, info.primaryFileName).absolutePath

    /** Absolute path to mmproj file (GGUF only, null for LiteRT). */
    val mmprojPath: String?
        get() = info.files
            .find { it.type == ModelFileType.GGUF_MMPROJ }
            ?.let { File(localDir, it.fileName).absolutePath }

    /** Whether all required files exist on disk. */
    val isComplete: Boolean
        get() = info.files.all { File(localDir, it.fileName).exists() }

    /** Total size on disk in bytes. */
    val diskSizeBytes: Long
        get() = info.files.sumOf { File(localDir, it.fileName).let { f -> if (f.exists()) f.length() else 0L } }
}

data class GgufVisionPackage(
    val basePath: String,
    val mmprojPath: String,
)

/**
 * Manages model storage, download state, and active model selection.
 * Models are stored in: app internal storage / models / <model-id> /
 *
 * Thread-safe for concurrent access.
 */
class ModelManager(private val context: Context) {

    private val modelsDir: File = File(context.filesDir, "models").also { it.mkdirs() }
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chromalab_models", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_MODEL = "active_model_id"
        private const val KEY_CHROMATOGRAM_MODEL = "chromatogram_model_id"
        private const val KEY_THREAD_COUNT = "thread_count"
        private const val KEY_DOWNLOAD_PARALLELISM = "download_parallelism"
        private const val KEY_DOWNLOAD_SPEED_LIMIT_MBPS = "download_speed_limit_mbps"
        private const val KEY_ACCEL_BACKEND = "accel_backend"
        private const val KEY_AUTO_UNLOAD_MINUTES = "auto_unload_minutes"
    }

    // ===== Model Query =====

    /**
     * Get all downloaded models (both builtin and custom).
     */
    fun getDownloadedModels(): List<DownloadedModel> {
        val activeId = getActiveModelId()
        return modelsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val info = ModelRegistry.findById(dir.name) ?: buildCustomModelInfo(dir)
                info?.let {
                    DownloadedModel(
                        info = it,
                        localDir = dir,
                        isActive = dir.name == activeId,
                    )
                }
            }
            ?.filter { it.isComplete }
            ?: emptyList()
    }

    /**
     * Get the currently active model, or null if none set.
     */
    fun getActiveModel(): DownloadedModel? {
        val activeId = getActiveModelId() ?: return null
        return getDownloadedModels().find { it.info.id == activeId }
    }

    /**
     * Set the active model by ID.
     */
    fun setActiveModel(id: String) {
        prefs.edit().putString(KEY_ACTIVE_MODEL, id).apply()
        println("MODEL[MGR] Active model set to: $id")
    }

    /**
     * Clear the active model (deactivate without deleting).
     */
    fun clearActiveModel() {
        prefs.edit().remove(KEY_ACTIVE_MODEL).apply()
        println("MODEL[MGR] Active model cleared")
    }

    /**
     * Get the active model ID from preferences.
     */
    fun getActiveModelId(): String? = prefs.getString(KEY_ACTIVE_MODEL, null)

    /** Model selected specifically for chromatogram photo analysis. */
    fun getChromatogramModelId(): String? = prefs.getString(KEY_CHROMATOGRAM_MODEL, null)
        ?.takeIf { it.isNotBlank() }

    fun setChromatogramModel(id: String) {
        prefs.edit().putString(KEY_CHROMATOGRAM_MODEL, id).apply()
        println("MODEL[MGR] Chromatogram model set to: $id")
    }

    fun clearChromatogramModel() {
        prefs.edit().remove(KEY_CHROMATOGRAM_MODEL).apply()
        println("MODEL[MGR] Chromatogram model cleared")
    }

    // ===== Download / Delete =====

    /**
     * Get the local directory for a model. Creates if needed.
     */
    fun getModelDir(modelId: String): File = File(modelsDir, modelId).also { it.mkdirs() }

    /**
     * Check if a model is fully downloaded.
     */
    fun isDownloaded(modelId: String): Boolean {
        val info = ModelRegistry.findById(modelId) ?: return false
        val dir = File(modelsDir, modelId)
        return dir.exists() && info.files.all { File(dir, it.fileName).exists() }
    }

    /**
     * Delete a downloaded model and its files.
     */
    fun delete(modelId: String): Boolean {
        val dir = File(modelsDir, modelId)
        if (!dir.exists()) return false

        val deleted = dir.deleteRecursively()
        if (deleted) {
            println("MODEL[MGR] Deleted model: $modelId")
            // If this was the active model, clear selection
            if (getActiveModelId() == modelId) {
                prefs.edit().remove(KEY_ACTIVE_MODEL).apply()
            }
            if (getChromatogramModelId() == modelId) {
                prefs.edit().remove(KEY_CHROMATOGRAM_MODEL).apply()
            }
        }
        return deleted
    }

    /**
     * Import a model file from a URI (user picked from file manager).
     * Copies the file into the models directory.
     *
     * @return ModelInfo if successfully identified, null otherwise
     */
    suspend fun importFile(uri: Uri, targetModelId: String? = null): ModelInfo? =
        withContext(Dispatchers.IO) {
            val result = importFileValidated(uri, targetModelId)
            (result as? ModelImportResult.Success)?.modelInfo
        }

    /**
     * Import a model file with full validation.
     * - Validates GGUF magic bytes and metadata
     * - Detects architecture and vision encoder
     * - Matches to known model families
     * - Warns if no vision encoder found
     */
    suspend fun importFileValidated(
        uri: Uri,
        targetModelId: String? = null,
    ): ModelImportResult = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileNameFromUri(uri)
                ?: return@withContext ModelImportResult.ValidationError(
                    "Не удалось определить имя файла",
                )

            val fileType = ModelRegistry.identifyFileType(fileName)
                ?: return@withContext ModelImportResult.ValidationError(
                    "Неизвестный формат файла",
                    "Поддерживаются: .gguf, .litertlm",
                )

            // Try to match to a known builtin model by filename
            val matchedBuiltin = ModelRegistry.findByFileName(fileName)
            val modelId = targetModelId
                ?: matchedBuiltin?.id
                ?: "custom_${System.currentTimeMillis()}"

            if (matchedBuiltin != null) {
                println("MODEL[MGR] Matched import to builtin: ${matchedBuiltin.displayName} (${matchedBuiltin.id})")
            }

            val dir = getModelDir(modelId)
            val targetFile = File(dir, fileName)

            // Copy file
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 65536)
                }
            } ?: return@withContext ModelImportResult.IoError("Не удалось открыть файл")

            println("MODEL[MGR] Copied: $fileName -> ${targetFile.absolutePath}")

            // Validate based on type
            val warnings = mutableListOf<String>()
            var ggufInfo: GgufValidator.GgufInfo? = null

            when (fileType) {
                ModelFileType.GGUF_BASE, ModelFileType.GGUF_MMPROJ -> {
                    ggufInfo = GgufValidator.validate(targetFile)
                    if (!ggufInfo.isValid) {
                        // Clean up invalid file
                        targetFile.delete()
                        if (dir.listFiles()?.isEmpty() == true) dir.delete()
                        return@withContext ModelImportResult.ValidationError(
                            "Невалидный GGUF файл",
                            ggufInfo.error,
                        )
                    }
                    println("MODEL[MGR] GGUF validated: ${ggufInfo.summary}")

                    // Check for vision encoder
                    if (fileType == ModelFileType.GGUF_BASE && !ggufInfo.hasVisionEncoder) {
                        // Check if there's already an mmproj in the same dir
                        val hasMmproj = dir.listFiles()?.any {
                            it.name.contains("mmproj") && it.name.endsWith(".gguf")
                        } == true
                        if (!hasMmproj) {
                            warnings.add("Модель без vision encoder. Для анализа графиков нужен mmproj файл.")
                        }
                    }
                }
                ModelFileType.LITERT_BUNDLE -> {
                    if (!GgufValidator.isLiteRTBundle(targetFile)) {
                        warnings.add("Файл может быть повреждён (не прошёл проверку формата)")
                    }
                }
            }

            // Build model info — use builtin if matched, else create custom
            val finalInfo = if (matchedBuiltin != null) {
                matchedBuiltin
            } else {
                val family = GgufValidator.matchFamily(fileName, ggufInfo?.architecture)
                val info = buildCustomModelInfo(dir) ?: run {
                    dir.deleteRecursively()
                    return@withContext ModelImportResult.IoError("Не удалось создать запись модели")
                }
                if (family != null) {
                    info.copy(family = family, description = "Импортированная модель ($family)")
                } else info
            }

            ModelImportResult.Success(
                modelInfo = finalInfo,
                ggufInfo = ggufInfo,
                warnings = warnings,
            )
        } catch (e: Exception) {
            println("MODEL[MGR] Import failed: ${e.message}")
            ModelImportResult.IoError("Ошибка импорта: ${e.message}", e)
        }
    }

    /**
     * Export a model file to a user-chosen location.
     */
    suspend fun exportFile(modelId: String, targetUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val info = ModelRegistry.findById(modelId) ?: return@withContext false
                val dir = File(modelsDir, modelId)
                val primaryFile = File(dir, info.primaryFileName)
                if (!primaryFile.exists()) return@withContext false

                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    primaryFile.inputStream().use { input ->
                        input.copyTo(output, bufferSize = 8192)
                    }
                }

                println("MODEL[MGR] Exported: $modelId -> $targetUri")
                true
            } catch (e: Exception) {
                println("MODEL[MGR] Export failed: ${e.message}")
                false
            }
        }

    // ===== Settings =====

    /** Number of CPU threads for llama.cpp inference. */
    var threadCount: Int
        get() = prefs.getInt(KEY_THREAD_COUNT, Runtime.getRuntime().availableProcessors() / 2)
        set(value) = prefs.edit().putInt(KEY_THREAD_COUNT, value.coerceIn(1, 16)).apply()

    /** Number of HTTP range chunks used for a single model file download. */
    var downloadParallelism: Int
        get() = sanitizeDownloadParallelism(prefs.getInt(KEY_DOWNLOAD_PARALLELISM, 4))
        set(value) = prefs.edit().putInt(KEY_DOWNLOAD_PARALLELISM, sanitizeDownloadParallelism(value)).apply()

    /** Total model download speed limit in MiB/s. 0 means unlimited. */
    var downloadSpeedLimitMbps: Int
        get() = prefs.getInt(KEY_DOWNLOAD_SPEED_LIMIT_MBPS, 0).coerceIn(0, 50)
        set(value) = prefs.edit().putInt(KEY_DOWNLOAD_SPEED_LIMIT_MBPS, value.coerceIn(0, 50)).apply()

    val downloadSpeedLimitBytesPerSecond: Long
        get() = if (downloadSpeedLimitMbps <= 0) 0L else downloadSpeedLimitMbps * 1024L * 1024L

    /** Acceleration backend name. */
    var accelBackend: String
        get() = prefs.getString(KEY_ACCEL_BACKEND, "CPU") ?: "CPU"
        set(value) = prefs.edit().putString(KEY_ACCEL_BACKEND, value).apply()

    /** Auto-unload VLM model after N minutes of inactivity (0 = disabled). */
    var autoUnloadMinutes: Int
        get() = prefs.getInt(KEY_AUTO_UNLOAD_MINUTES, 5)
        set(value) = prefs.edit().putInt(KEY_AUTO_UNLOAD_MINUTES, value.coerceIn(0, 30)).apply()

    private fun sanitizeDownloadParallelism(value: Int): Int {
        val allowed = intArrayOf(1, 2, 4, 8, 10, 12, 16)
        return allowed.minBy { kotlin.math.abs(it - value) }
    }

    // ===== Device Info =====

    /** Total RAM in MB. */
    fun getDeviceRamMb(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / (1024 * 1024)).toInt()
    }

    /** Currently available RAM in MB. */
    fun getAvailableRamMb(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.availMem / (1024 * 1024)).toInt()
    }

    /** Conservative mode for older phones where accelerator probing can exhaust memory. */
    fun isConservativeDevice(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
        return activityManager.isLowRamDevice || getDeviceRamMb() < 7000
    }

    /** Whether a model can be loaded for text/chat use on this device. */
    fun canLoadForText(model: ModelInfo): Boolean {
        if (model.runtime == ModelRuntime.LLAMA_CPP && model.files.none { it.type == ModelFileType.GGUF_BASE }) {
            return false
        }
        val requiredRam = when (model.runtime) {
            ModelRuntime.LITERT_LM -> 4096
            ModelRuntime.LLAMA_CPP -> maxOf(model.minRamMb, baseModelSizeMb(model) + 768)
        }
        val availableRequired = if (isConservativeDevice()) 1800 else 1200
        return getDeviceRamMb() >= requiredRam && getAvailableRamMb() >= availableRequired
    }

    /** Whether a model can safely receive image input on this device. */
    fun canLoadForVision(model: ModelInfo): Boolean {
        if (!model.supportsVision) return false
        if (model.runtime == ModelRuntime.LLAMA_CPP && !ModelRegistry.hasGgufVisionFilePair(model)) {
            return false
        }
        val conservative = isConservativeDevice()
        val smallGgufVision = isSmallGgufVisionModel(model)
        val requiredRam = when (model.runtime) {
            ModelRuntime.LITERT_LM -> {
                // Allow Gemma E2B on 6 GB phones via CPU fallback, but keep 4 GB devices out.
                val floor = if (conservative) 5500 else 8192
                maxOf(model.minRamMb, floor)
            }
            ModelRuntime.LLAMA_CPP -> {
                // GGUF vision loads both the base model and mmproj into llama.cpp/mtmd.
                // Small OCR/VLM packages are allowed on 6 GB class test devices; heavier
                // models still need more headroom to avoid process kills.
                if (conservative && !smallGgufVision) return false
                val overhead = if (smallGgufVision) 900 else 1800
                maxOf(model.minRamMb, visionPackageSizeMb(model) + overhead)
            }
        }
        val availableRequired = when (model.runtime) {
            ModelRuntime.LITERT_LM -> if (conservative) 2600 else 3000
            ModelRuntime.LLAMA_CPP -> if (smallGgufVision) {
                maxOf(1700, visionPackageSizeMb(model) / 2)
            } else {
                maxOf(2200, visionPackageSizeMb(model) / 2)
            }
        }
        return getDeviceRamMb() >= requiredRam && getAvailableRamMb() >= availableRequired
    }

    fun canLoadForChromatogramVision(model: ModelInfo): Boolean =
        canLoadForVision(model) &&
            (ModelRegistry.isChromatogramVisionModel(model) || !model.isBuiltin)

    fun liteRtPreferAccelerator(model: ModelInfo): Boolean =
        model.runtime == ModelRuntime.LITERT_LM && !isConservativeDevice()

    fun liteRtMaxTokens(model: ModelInfo): Int? =
        if (model.runtime == ModelRuntime.LITERT_LM) {
            InferenceConfig.forModelFamily(model.family).maxTokens
        } else {
            null
        }

    fun liteRtCacheDir(): String =
        File(context.cacheDir, "litertlm").also { it.mkdirs() }.absolutePath

    fun llamaShouldLoadVisionProjector(model: ModelInfo): Boolean =
        model.runtime == ModelRuntime.LLAMA_CPP && canLoadForVision(model)

    fun requireGgufVisionPackage(model: DownloadedModel): GgufVisionPackage {
        require(model.info.runtime == ModelRuntime.LLAMA_CPP) {
            "GGUF vision package requested for non-GGUF model: ${model.info.displayName}"
        }
        val baseSpec = model.info.files.find { it.type == ModelFileType.GGUF_BASE }
            ?: throw IllegalStateException("GGUF image analysis requires a base model file for ${model.info.displayName}")
        val mmprojSpec = model.info.files.find { it.type == ModelFileType.GGUF_MMPROJ }
            ?: throw IllegalStateException("GGUF image analysis requires a matching mmproj vision projector for ${model.info.displayName}")
        val baseFile = File(model.localDir, baseSpec.fileName)
        val mmprojFile = File(model.localDir, mmprojSpec.fileName)

        if (!baseFile.isFile || baseFile.length() <= 0L) {
            throw IllegalStateException("GGUF base model file is missing or empty: ${baseSpec.fileName}")
        }
        if (!mmprojFile.isFile || mmprojFile.length() <= 0L) {
            throw IllegalStateException("GGUF mmproj vision projector is missing or empty: ${mmprojSpec.fileName}")
        }
        if (baseFile.canonicalPath == mmprojFile.canonicalPath) {
            throw IllegalStateException("GGUF base model and mmproj must be separate files for image analysis")
        }
        if (!GgufValidator.isGguf(baseFile)) {
            throw IllegalStateException("GGUF base model file is invalid: ${baseSpec.fileName}")
        }
        if (!GgufValidator.isGguf(mmprojFile)) {
            throw IllegalStateException("GGUF mmproj vision projector is invalid: ${mmprojSpec.fileName}")
        }
        return GgufVisionPackage(
            basePath = baseFile.absolutePath,
            mmprojPath = mmprojFile.absolutePath,
        )
    }

    fun llamaContextSize(model: ModelInfo, forVision: Boolean): Int {
        val config = InferenceConfig.forModelFamily(model.family)
        return when {
            isConservativeDevice() && !forVision -> 2048
            forVision && model.family.contains("paddleocr", ignoreCase = true) -> 2048
            else -> config.contextSize
        }
    }

    fun llamaBatchSize(model: ModelInfo, forVision: Boolean): Int {
        val config = InferenceConfig.forModelFamily(model.family)
        return when {
            isConservativeDevice() -> 128
            forVision && model.family.contains("paddleocr", ignoreCase = true) -> 256
            else -> config.batchSize
        }
    }

    fun compatibilityMessage(model: ModelInfo, forVision: Boolean): String {
        val total = getDeviceRamMb()
        val available = getAvailableRamMb()
        return if (forVision) {
            "Vision model cannot be loaded for ${model.displayName}: RAM ${total} MB, available ${available} MB."
        } else {
            "Model is too heavy for this device: RAM ${total} MB, available ${available} MB."
        }
    }

    /** Available storage in bytes. */
    fun getAvailableStorageBytes(): Long = modelsDir.usableSpace

    /** Total disk used by all downloaded models. */
    fun getTotalModelDiskUsage(): Long =
        getDownloadedModels().sumOf { it.diskSizeBytes }

    // ===== Internal =====

    private fun baseModelSizeMb(model: ModelInfo): Int =
        (model.files
            .filterNot { it.type == ModelFileType.GGUF_MMPROJ }
            .sumOf { it.sizeBytes } / (1024 * 1024)).toInt()

    private fun visionPackageSizeMb(model: ModelInfo): Int =
        (model.totalSizeBytes / (1024 * 1024)).toInt()

    private fun isSmallGgufVisionModel(model: ModelInfo): Boolean {
        if (model.runtime != ModelRuntime.LLAMA_CPP || !model.supportsVision) return false
        val family = model.family.lowercase()
        val id = model.id.lowercase()
        return family.contains("paddleocr") ||
            (family.contains("qwen3-vl") && id.contains("2b") && (id.contains("q2") || id.contains("q3"))) ||
            (family.contains("smolvlm") && id.contains("q4"))
    }

    private fun buildCustomModelInfo(dir: File): ModelInfo? {
        val files = dir.listFiles() ?: return null
        if (files.isEmpty()) return null

        val modelFiles = files.mapNotNull { file ->
            val type = ModelRegistry.identifyFileType(file.name) ?: return@mapNotNull null
            ModelFile(
                fileName = file.name,
                sizeBytes = file.length(),
                type = type,
                downloadUrl = "", // no download URL for imported models
            )
        }.sortedWith(
            compareBy<ModelFile> {
                when (it.type) {
                    ModelFileType.LITERT_BUNDLE -> 0
                    ModelFileType.GGUF_BASE -> 1
                    ModelFileType.GGUF_MMPROJ -> 2
                }
            }.thenBy { it.fileName.lowercase() }
        )

        if (modelFiles.isEmpty()) return null

        val runtime = if (modelFiles.any { it.type == ModelFileType.LITERT_BUNDLE })
            ModelRuntime.LITERT_LM else ModelRuntime.LLAMA_CPP

        val primaryFileName = modelFiles.first().fileName
        val family = GgufValidator.matchFamily(primaryFileName, null) ?: when {
            primaryFileName.contains("gemma", ignoreCase = true) -> "gemma"
            primaryFileName.endsWith(".litertlm", ignoreCase = true) -> "gemma"
            else -> "custom"
        }

        val hasVision = modelFiles.any { it.type == ModelFileType.LITERT_BUNDLE } ||
            (modelFiles.any { it.type == ModelFileType.GGUF_BASE } &&
                modelFiles.any { it.type == ModelFileType.GGUF_MMPROJ })

        return ModelInfo(
            id = dir.name,
            displayName = dir.name.replace("_", " ").replaceFirstChar { it.uppercase() },
            family = family,
            runtime = runtime,
            files = modelFiles,
            minRamMb = 4096,
            isBuiltin = false,
            supportsVision = hasVision,
            description = "Пользовательская модель",
        )
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (nameIndex >= 0) cursor.getString(nameIndex) else null
        } ?: uri.lastPathSegment
    }
}
