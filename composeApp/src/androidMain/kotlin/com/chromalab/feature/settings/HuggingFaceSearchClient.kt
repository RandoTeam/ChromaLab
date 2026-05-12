package com.chromalab.feature.settings

import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.model.ModelFile
import com.chromalab.feature.processing.model.ModelFileType
import com.chromalab.feature.processing.model.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.ceil

class HuggingFaceSearchClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(
        query: String,
        sort: HuggingFaceSortOption,
        deviceRamMb: Int,
        availableStorageBytes: Long,
    ): List<HuggingFaceSearchResult> = withContext(Dispatchers.IO) {
        val models = fetchModelList(query, sort)
        models.mapNotNull { model ->
            runCatching {
                buildResult(model, deviceRamMb, availableStorageBytes)
            }.getOrNull()
        }.take(MAX_RESULTS)
    }

    private fun fetchModelList(
        query: String,
        sort: HuggingFaceSortOption,
    ): List<JsonObject> {
        val trimmed = query.trim()
        val params = mutableListOf(
            "sort=${sort.apiValue()}",
            "direction=-1",
            "limit=18",
            "full=true",
        )

        val author = trimmed.substringAfter(':', "").trim().takeIf {
            trimmed.startsWith("author:", ignoreCase = true) && it.isNotBlank()
        }
        if (author != null) {
            params += "author=${encode(author)}"
        } else {
            params += "search=${encode(trimmed.ifBlank { "gguf chat" })}"
        }

        val url = "$HF_API/models?${params.joinToString("&")}"
        val raw = httpGet(url)
        return json.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
            element as? JsonObject
        }
    }

    private fun buildResult(
        model: JsonObject,
        deviceRamMb: Int,
        availableStorageBytes: Long,
    ): HuggingFaceSearchResult? {
        val repoId = model.string("id") ?: model.string("modelId") ?: return null
        val siblings = model["siblings"] as? JsonArray ?: return null
        val fileNames = siblings.mapNotNull { it.jsonObject.string("rfilename") }
        val primary = choosePrimaryFile(fileNames) ?: return null
        val mmproj = chooseMmproj(fileNames)
        val runtime = if (primary.endsWith(".litertlm", ignoreCase = true)) {
            ModelRuntime.LITERT_LM
        } else {
            ModelRuntime.LLAMA_CPP
        }

        val primarySize = resolveSize(repoId, primary)
        if (primarySize <= 0L) return null

        val files = mutableListOf(
            ModelFile(
                fileName = primary.substringAfterLast('/'),
                sizeBytes = primarySize,
                type = if (runtime == ModelRuntime.LITERT_LM) ModelFileType.LITERT_BUNDLE else ModelFileType.GGUF_BASE,
                downloadUrl = resolveUrl(repoId, primary),
            )
        )

        if (runtime == ModelRuntime.LLAMA_CPP && mmproj != null) {
            val mmprojSize = resolveSize(repoId, mmproj)
            if (mmprojSize > 0L) {
                files += ModelFile(
                    fileName = mmproj.substringAfterLast('/'),
                    sizeBytes = mmprojSize,
                    type = ModelFileType.GGUF_MMPROJ,
                    downloadUrl = resolveUrl(repoId, mmproj),
                )
            }
        }

        val totalSize = files.sumOf { it.sizeBytes }
        val minRamMb = estimateRamMb(totalSize, runtime)
        val storageOk = totalSize < availableStorageBytes * 0.9
        val ramOk = minRamMb <= deviceRamMb
        val compatible = ramOk && storageOk
        val displayName = buildDisplayName(repoId, primary)

        val info = ModelInfo(
            id = buildModelId(repoId, primary),
            displayName = displayName,
            family = inferFamily(repoId),
            runtime = runtime,
            files = files,
            minRamMb = minRamMb,
            isBuiltin = false,
            supportsVision = runtime == ModelRuntime.LITERT_LM || files.any { it.type == ModelFileType.GGUF_MMPROJ },
            description = "Hugging Face: $repoId",
            quantLabel = extractQuant(primary),
        )

        return HuggingFaceSearchResult(
            repoId = repoId,
            author = model.string("author") ?: repoId.substringBefore('/'),
            displayName = displayName,
            downloads = model.long("downloads"),
            likes = model.long("likes"),
            lastModified = model.string("lastModified").orEmpty(),
            tags = (model["tags"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
            selectedFileName = primary.substringAfterLast('/'),
            modelInfo = info,
            isCompatible = compatible,
            compatibilityLabel = when {
                !ramOk -> "needs ${ceil(minRamMb / 1024f).toInt()} GB RAM"
                !storageOk -> "no storage"
                else -> "fits"
            },
        )
    }

    private fun choosePrimaryFile(fileNames: List<String>): String? {
        val litert = fileNames
            .filter { it.endsWith(".litertlm", ignoreCase = true) }
            .minByOrNull { it.length }
        if (litert != null) return litert

        return fileNames
            .filter { it.endsWith(".gguf", ignoreCase = true) }
            .filterNot { it.contains("mmproj", ignoreCase = true) }
            .filterNot { it.contains("-of-", ignoreCase = true) }
            .minWithOrNull(compareBy<String> { quantRank(it) }.thenBy { it.length })
    }

    private fun chooseMmproj(fileNames: List<String>): String? =
        fileNames
            .filter { it.endsWith(".gguf", ignoreCase = true) }
            .filter { it.contains("mmproj", ignoreCase = true) }
            .minWithOrNull(compareBy<String> { quantRank(it) }.thenBy { it.length })

    private fun quantRank(path: String): Int {
        val name = path.uppercase()
        val preferred = listOf(
            "Q4_K_M",
            "Q4_0",
            "Q5_K_M",
            "Q3_K_M",
            "Q2_K",
            "Q6_K",
            "Q8_0",
            "F16",
            "BF16",
        )
        return preferred.indexOfFirst { name.contains(it) }.takeIf { it >= 0 } ?: 100
    }

    private fun extractQuant(path: String): String? {
        val name = path.uppercase()
        return listOf("Q2_K", "Q3_K_M", "Q4_0", "Q4_K_M", "Q5_K_M", "Q6_K", "Q8_0", "F16", "BF16")
            .firstOrNull { name.contains(it) }
    }

    private fun estimateRamMb(totalSizeBytes: Long, runtime: ModelRuntime): Int {
        val modelMb = totalSizeBytes / (1024f * 1024f)
        val overhead = if (runtime == ModelRuntime.LITERT_LM) 1536 else 2048
        return (modelMb * 1.25f + overhead).toInt().coerceAtLeast(2048)
    }

    private fun inferFamily(repoId: String): String {
        val lower = repoId.lowercase()
        return when {
            "gemma" in lower -> "gemma"
            "qwen" in lower -> "qwen"
            "smolvlm" in lower -> "smolvlm"
            "paddleocr" in lower -> "paddleocr-vl"
            "deepseek" in lower -> "deepseek"
            "moondream" in lower -> "moondream"
            else -> "custom"
        }
    }

    private fun buildDisplayName(repoId: String, fileName: String): String {
        val base = repoId.substringAfter('/')
        val quant = extractQuant(fileName)
        return if (quant == null) base else "$base · $quant"
    }

    private fun buildModelId(repoId: String, fileName: String): String {
        val raw = "hf_${repoId}_${fileName.substringAfterLast('/')}"
        return raw.lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "_")
            .take(120)
    }

    private fun resolveUrl(repoId: String, filePath: String): String {
        val encodedPath = filePath.split('/').joinToString("/") { encode(it) }
        return "https://huggingface.co/$repoId/resolve/main/$encodedPath"
    }

    private fun resolveSize(repoId: String, filePath: String): Long {
        val url = resolveUrl(repoId, filePath)
        return headContentLength(url).takeIf { it > 0L } ?: rangedContentLength(url)
    }

    private fun headContentLength(url: String, redirects: Int = 0): Long {
        if (redirects > 5) return -1L
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            connection.connect()
            val code = connection.responseCode
            if (code in 300..399) {
                val next = connection.getHeaderField("Location") ?: return -1L
                headContentLength(next, redirects + 1)
            } else {
                connection.getHeaderFieldLong("Content-Length", -1L)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun rangedContentLength(url: String, redirects: Int = 0): Long {
        if (redirects > 5) return -1L
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("Range", "bytes=0-0")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            connection.connect()
            val code = connection.responseCode
            if (code in 300..399) {
                val next = connection.getHeaderField("Location") ?: return -1L
                rangedContentLength(next, redirects + 1)
            } else {
                connection.getHeaderField("Content-Range")
                    ?.substringAfterLast('/')
                    ?.toLongOrNull()
                    ?: connection.getHeaderFieldLong("Content-Length", -1L)
            }
        } finally {
            runCatching { connection.inputStream.close() }
            connection.disconnect()
        }
    }

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun HuggingFaceSortOption.apiValue(): String = when (this) {
        HuggingFaceSortOption.DOWNLOADS -> "downloads"
        HuggingFaceSortOption.LIKES -> "likes"
        HuggingFaceSortOption.UPDATED -> "lastModified"
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.long(key: String): Long =
        this[key]?.jsonPrimitive?.longOrNull ?: 0L

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private companion object {
        const val HF_API = "https://huggingface.co/api"
        const val MAX_RESULTS = 10
        const val NETWORK_TIMEOUT_MS = 15_000
        const val USER_AGENT = "ChromaLab/1.0"
    }
}
