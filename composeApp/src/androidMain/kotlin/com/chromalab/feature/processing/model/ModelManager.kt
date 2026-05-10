package com.chromalab.feature.processing.model

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
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
        private const val KEY_THREAD_COUNT = "thread_count"
        private const val KEY_ACCEL_BACKEND = "accel_backend"
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
     * Get the active model ID from preferences.
     */
    fun getActiveModelId(): String? = prefs.getString(KEY_ACTIVE_MODEL, null)

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
        }
        return deleted
    }

    // ===== Import / Export =====

    /**
     * Import a model file from a URI (user picked from file manager).
     * Copies the file into the models directory.
     *
     * @return ModelInfo if successfully identified, null otherwise
     */
    suspend fun importFile(uri: Uri, targetModelId: String? = null): ModelInfo? =
        withContext(Dispatchers.IO) {
            try {
                val fileName = getFileNameFromUri(uri) ?: return@withContext null
                val fileType = ModelRegistry.identifyFileType(fileName)
                    ?: return@withContext null

                val modelId = targetModelId ?: "custom_${System.currentTimeMillis()}"
                val dir = getModelDir(modelId)
                val targetFile = File(dir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 8192)
                    }
                }

                println("MODEL[MGR] Imported: $fileName -> ${targetFile.absolutePath}")

                // Try to match to a builtin model
                ModelRegistry.findById(modelId) ?: buildCustomModelInfo(dir)
            } catch (e: Exception) {
                println("MODEL[MGR] Import failed: ${e.message}")
                null
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

    /** Acceleration backend name. */
    var accelBackend: String
        get() = prefs.getString(KEY_ACCEL_BACKEND, "CPU") ?: "CPU"
        set(value) = prefs.edit().putString(KEY_ACCEL_BACKEND, value).apply()

    // ===== Device Info =====

    /** Total RAM in MB. */
    fun getDeviceRamMb(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / (1024 * 1024)).toInt()
    }

    /** Available storage in bytes. */
    fun getAvailableStorageBytes(): Long = modelsDir.usableSpace

    /** Total disk used by all downloaded models. */
    fun getTotalModelDiskUsage(): Long =
        getDownloadedModels().sumOf { it.diskSizeBytes }

    // ===== Internal =====

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
        }

        if (modelFiles.isEmpty()) return null

        val runtime = if (modelFiles.any { it.type == ModelFileType.LITERT_BUNDLE })
            ModelRuntime.LITERT_LM else ModelRuntime.LLAMA_CPP

        val hasVision = modelFiles.any { it.type == ModelFileType.GGUF_MMPROJ } ||
                modelFiles.any { it.type == ModelFileType.LITERT_BUNDLE }

        return ModelInfo(
            id = dir.name,
            displayName = dir.name.replace("_", " ").replaceFirstChar { it.uppercase() },
            family = "custom",
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
