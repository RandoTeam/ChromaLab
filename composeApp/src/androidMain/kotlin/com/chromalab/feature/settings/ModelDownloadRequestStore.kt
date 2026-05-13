package com.chromalab.feature.settings

import android.content.Context
import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.model.ModelFile
import com.chromalab.feature.processing.model.ModelFileType
import com.chromalab.feature.processing.model.ModelInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

internal data class ModelDownloadRequest(
    val model: ModelInfo,
    val parallelism: Int,
)

internal object ModelDownloadRequestStore {
    private const val PREFS = "chromalab_model_downloads"
    private const val KEY_ACTIVE_IDS = "active_ids"
    private const val KEY_REQUEST_PREFIX = "request_"

    private val json = Json { ignoreUnknownKeys = true }

    fun save(context: Context, model: ModelInfo, parallelism: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_ACTIVE_IDS, emptySet()).orEmpty().toMutableSet()
        ids += model.id
        prefs.edit()
            .putStringSet(KEY_ACTIVE_IDS, ids)
            .putString(KEY_REQUEST_PREFIX + model.id, encode(model, parallelism).toString())
            .apply()
    }

    fun remove(context: Context, modelId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_ACTIVE_IDS, emptySet()).orEmpty().toMutableSet()
        ids -= modelId
        prefs.edit()
            .putStringSet(KEY_ACTIVE_IDS, ids)
            .remove(KEY_REQUEST_PREFIX + modelId)
            .apply()
    }

    fun hasPending(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_ACTIVE_IDS, emptySet())
            .orEmpty()
            .isNotEmpty()

    fun read(context: Context, modelId: String): ModelDownloadRequest? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REQUEST_PREFIX + modelId, null)
            ?: return null
        return runCatching { decode(json.parseToJsonElement(raw).jsonObject) }.getOrNull()
    }

    fun readAll(context: Context): List<ModelDownloadRequest> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ACTIVE_IDS, emptySet())
            .orEmpty()
            .mapNotNull { read(context, it) }
    }

    private fun encode(model: ModelInfo, parallelism: Int): JsonObject = buildJsonObject {
        put("parallelism", parallelism)
        put("id", model.id)
        put("displayName", model.displayName)
        put("family", model.family)
        put("runtime", model.runtime.name)
        put("minRamMb", model.minRamMb)
        put("isBuiltin", model.isBuiltin)
        put("supportsVision", model.supportsVision)
        put("description", model.description)
        model.groupId?.let { put("groupId", it) }
        model.quantLabel?.let { put("quantLabel", it) }
        put(
            "files",
            buildJsonArray {
                model.files.forEach { file ->
                    add(
                        buildJsonObject {
                            put("fileName", file.fileName)
                            put("sizeBytes", file.sizeBytes)
                            put("type", file.type.name)
                            put("downloadUrl", file.downloadUrl)
                        }
                    )
                }
            },
        )
    }

    private fun decode(obj: JsonObject): ModelDownloadRequest {
        val files = obj["files"]!!.jsonArray.map { element ->
            val file = element.jsonObject
            ModelFile(
                fileName = file.string("fileName"),
                sizeBytes = file.long("sizeBytes"),
                type = ModelFileType.valueOf(file.string("type")),
                downloadUrl = file.string("downloadUrl"),
            )
        }

        return ModelDownloadRequest(
            model = ModelInfo(
                id = obj.string("id"),
                displayName = obj.string("displayName"),
                family = obj.string("family"),
                runtime = ModelRuntime.valueOf(obj.string("runtime")),
                files = files,
                minRamMb = obj.int("minRamMb"),
                isBuiltin = obj.boolean("isBuiltin"),
                supportsVision = obj.boolean("supportsVision"),
                description = obj.optionalString("description").orEmpty(),
                groupId = obj.optionalString("groupId"),
                quantLabel = obj.optionalString("quantLabel"),
            ),
            parallelism = obj.int("parallelism"),
        )
    }

    private fun JsonObject.string(key: String): String = this[key]!!.jsonPrimitive.content
    private fun JsonObject.optionalString(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.long(key: String): Long = this[key]!!.jsonPrimitive.long
    private fun JsonObject.int(key: String): Int = this[key]!!.jsonPrimitive.int
    private fun JsonObject.boolean(key: String): Boolean = this[key]!!.jsonPrimitive.boolean
}
