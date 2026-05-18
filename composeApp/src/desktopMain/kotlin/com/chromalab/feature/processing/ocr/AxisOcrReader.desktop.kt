package com.chromalab.feature.processing.ocr

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.geometry.TickOcrCropArtifact
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.Base64
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

actual class AxisOcrReader actual constructor() {
    actual suspend fun readAxisLabels(
        imagePath: String,
        graphRegion: GraphRegion,
    ): AxisOcrResult = withContext(Dispatchers.IO) {
        val config = DesktopAxisVlmConfig.fromEnvironment()
            ?: return@withContext unavailableAxisOcrResult("desktop_axis_vlm.endpoint_not_configured")
        runCatching {
            val image = ImageIO.read(File(imagePath))
                ?: return@runCatching unavailableAxisOcrResult("desktop_axis_vlm.image_read_failed")
            val bands = AxisBandSet.from(image.width, image.height, graphRegion)
            if (!bands.isUsable) {
                return@runCatching unavailableAxisOcrResult("desktop_axis_vlm.axis_bands_unusable")
            }

            val rawResponse = config.readReplayResponse() ?: run {
                val client = DesktopOpenAiVisionClient(config)
                val preflight = client.checkEndpoint()
                if (!preflight.ready || preflight.model == null) {
                    return@runCatching unavailableAxisOcrResult(preflight.warnings)
                }
                client.readAxisBands(
                    model = preflight.model,
                    xBandImage = image.crop(bands.xBand),
                    yBandImage = image.crop(bands.yBand),
                ).withWarnings(preflight.warnings)
            }
            if (rawResponse.content == null) {
                return@runCatching unavailableAxisOcrResult(rawResponse.warnings)
            }
            val modelResult = parseAxisBandModelResult(rawResponse.content)
                ?: return@runCatching unavailableAxisOcrResult(
                    rawResponse.warnings + "desktop_axis_vlm.response_json_unparseable",
                )
            modelResult.toAxisOcrResult(bands, config.minConfidence, rawResponse.warnings)
        }.getOrElse {
            val warning = if (it is HttpTimeoutException) {
                "desktop_axis_vlm.request_timeout"
            } else {
                "desktop_axis_vlm.request_failed"
            }
            unavailableAxisOcrResult(warning)
        }
    }

    actual suspend fun readTickLabelCrops(crops: List<TickOcrCropArtifact>): AxisOcrResult =
        unavailableAxisOcrResult(
            buildList {
                if (crops.isEmpty()) {
                    add("desktop_tick_crop_ocr.no_crops")
                } else {
                    add("desktop_tick_crop_ocr.local_crops_available:${crops.size}")
                    add("desktop_tick_crop_ocr.engine_not_configured")
                }
            },
        )
}

private fun DesktopAxisVlmConfig.readReplayResponse(): DesktopVlmTextResult? {
    val path = responseFile ?: return null
    return runCatching {
        File(path).readText()
    }.fold(
        onSuccess = { content ->
            DesktopVlmTextResult(
                content = content,
                warnings = listOf("desktop_axis_vlm.replay_response_file"),
            )
        },
        onFailure = {
            DesktopVlmTextResult(
                content = null,
                warnings = listOf("desktop_axis_vlm.replay_file_read_failed"),
            )
        },
    )
}

private data class DesktopAxisVlmConfig(
    val baseUrl: String?,
    val model: String?,
    val timeoutMs: Long,
    val minConfidence: Float,
    val responseFile: String?,
    val apiToken: String?,
) {
    companion object {
        fun fromEnvironment(): DesktopAxisVlmConfig? {
            val baseUrl = System.getenv("CHROMALAB_DESKTOP_VLM_BASE_URL")
                ?.trim()
                ?.trimEnd('/')
                ?.takeIf { it.isNotBlank() }
            val responseFile = System.getenv("CHROMALAB_DESKTOP_VLM_RESPONSE_FILE")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            if (baseUrl == null && responseFile == null) return null
            val apiToken = System.getenv("CHROMALAB_DESKTOP_VLM_API_TOKEN")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: System.getenv("CHROMALAB_DESKTOP_VLM_API_KEY")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            val model = System.getenv("CHROMALAB_DESKTOP_VLM_MODEL")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            val timeoutMs = System.getenv("CHROMALAB_DESKTOP_VLM_TIMEOUT_MS")
                ?.toLongOrNull()
                ?.coerceIn(5_000L, 900_000L)
                ?: 300_000L
            val minConfidence = System.getenv("CHROMALAB_DESKTOP_VLM_MIN_CONFIDENCE")
                ?.toFloatOrNull()
                ?.coerceIn(0.1f, 0.99f)
                ?: 0.65f
            return DesktopAxisVlmConfig(baseUrl, model, timeoutMs, minConfidence, responseFile, apiToken)
        }
    }
}

private class DesktopOpenAiVisionClient(
    private val config: DesktopAxisVlmConfig,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(config.timeoutMs))
        .build()

    fun checkEndpoint(): DesktopVlmPreflightResult {
        val baseUrl = config.baseUrl ?: return DesktopVlmPreflightResult(
            ready = false,
            model = null,
            warnings = listOf("desktop_axis_vlm.endpoint_not_configured"),
        )
        config.model?.let { configuredModel ->
            return DesktopVlmPreflightResult(
                ready = true,
                model = configuredModel,
                warnings = emptyList(),
            )
        }
        return runCatching {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/models"))
                .timeout(Duration.ofMillis(config.timeoutMs.coerceIn(1_000L, 3_000L)))
                .header("Accept", "application/json")
                .GET()
            config.apiToken?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }
            val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                val warnings = buildList {
                    if (response.statusCode() == 401 || response.statusCode() == 403) {
                        add("desktop_axis_vlm.auth_required")
                    }
                    add("desktop_axis_vlm.models_http_status_${response.statusCode()}")
                }
                return DesktopVlmPreflightResult(ready = false, model = null, warnings = warnings)
            }
            val modelIds = response.modelIds(json)
            val resolvedModel = modelIds.firstOrNull()
            val warnings = buildList {
                if (resolvedModel != null) {
                    add("desktop_axis_vlm.model_auto_selected")
                }
                if (resolvedModel == null) {
                    add("desktop_axis_vlm.model_not_discovered")
                }
            }
            DesktopVlmPreflightResult(
                ready = resolvedModel != null,
                model = resolvedModel,
                warnings = warnings,
            )
        }.getOrElse {
            val warning = if (it is HttpTimeoutException) {
                "desktop_axis_vlm.models_request_timeout"
            } else {
                "desktop_axis_vlm.models_request_failed"
            }
            DesktopVlmPreflightResult(
                ready = false,
                model = null,
                warnings = listOf(warning),
            )
        }
    }

    fun readAxisBands(
        model: String,
        xBandImage: BufferedImage,
        yBandImage: BufferedImage,
    ): DesktopVlmTextResult {
        val xResult = readAxisBandSafely(
            model = model,
            axis = DesktopAxisBandRequest.X,
            image = xBandImage,
        )
        val yResult = readAxisBandSafely(
            model = model,
            axis = DesktopAxisBandRequest.Y,
            image = yBandImage,
        )
        val warnings = (xResult.warnings + yResult.warnings).distinct()
        val xAxis = xResult.content?.extractFirstJsonObject()?.parseJsonObject(json)
        val yAxis = yResult.content?.extractFirstJsonObject()?.parseJsonObject(json)
        val parseWarnings = buildList {
            if (xResult.content != null && xAxis == null) add("desktop_axis_vlm.x_response_json_unparseable")
            if (yResult.content != null && yAxis == null) add("desktop_axis_vlm.y_response_json_unparseable")
        }
        if (xAxis == null && yAxis == null) {
            return DesktopVlmTextResult(
                content = null,
                warnings = warnings + parseWarnings,
            )
        }

        val content = buildJsonObject {
            put("xAxis", xAxis ?: emptyAxisObject("min"))
            put("yAxis", yAxis ?: emptyAxisObject("Abundance"))
            put("title", "null")
            put("confidence", minOf(xAxis?.floatOrNull("confidence") ?: 0f, yAxis?.floatOrNull("confidence") ?: 0f))
            put(
                "warnings",
                buildJsonArray {
                    (warnings + parseWarnings).forEach { warning -> add(JsonPrimitive(warning)) }
                },
            )
        }.toString()

        return DesktopVlmTextResult(
            content = content,
            warnings = warnings + parseWarnings,
        )
    }

    private fun readAxisBandSafely(
        model: String,
        axis: DesktopAxisBandRequest,
        image: BufferedImage,
    ): DesktopVlmTextResult =
        runCatching {
            readAxisBand(model, axis, image)
        }.getOrElse {
            DesktopVlmTextResult(
                content = null,
                warnings = listOf(
                    if (it is HttpTimeoutException) {
                        "desktop_axis_vlm.${axis.warningPrefix}_request_timeout"
                    } else {
                        "desktop_axis_vlm.${axis.warningPrefix}_request_failed"
                    },
                ),
            )
        }

    private fun readAxisBand(
        model: String,
        axis: DesktopAxisBandRequest,
        image: BufferedImage,
    ): DesktopVlmTextResult {
        val baseUrl = config.baseUrl ?: return DesktopVlmTextResult(
            content = null,
            warnings = listOf("desktop_axis_vlm.endpoint_not_configured"),
        )
        val payload = buildJsonObject {
            put("model", model)
            put("temperature", 0)
            put("max_tokens", 260)
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put(
                                "content",
                                "You extract chromatogram axis tick labels from one cropped axis band. Return strict JSON only.",
                            )
                        },
                    )
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put("content", buildContentArray(axis, image))
                        },
                    )
                },
            )
        }
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/chat/completions"))
            .timeout(Duration.ofMillis(config.timeoutMs))
            .header("Content-Type", "application/json")
        config.apiToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }
        val request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            return DesktopVlmTextResult(
                content = null,
                warnings = listOf("desktop_axis_vlm.${axis.warningPrefix}_http_status_${response.statusCode()}"),
            )
        }
        val content = response.contentText(json)
        return DesktopVlmTextResult(
            content = content,
            warnings = if (content == null) {
                listOf("desktop_axis_vlm.${axis.warningPrefix}_response_content_missing")
            } else {
                emptyList()
            },
        )
    }

    private fun buildContentArray(
        axis: DesktopAxisBandRequest,
        image: BufferedImage,
    ): JsonArray = buildJsonArray {
        add(
            buildJsonObject {
                put("type", "text")
                put("text", axis.prompt)
            },
        )
        add(imageElement(image))
    }

    private fun imageElement(image: BufferedImage): JsonObject = buildJsonObject {
        put("type", "image_url")
        put(
            "image_url",
            buildJsonObject {
                put("url", image.toPngDataUrl())
            },
        )
    }

    private fun HttpResponse<String>.contentText(json: Json): String? {
        val root = runCatching { json.parseToJsonElement(body()).jsonObject }.getOrNull() ?: return null
        val choices = root["choices"]?.asArray() ?: return null
        val first = choices.firstOrNull()?.asObject() ?: return null
        val message = first["message"]?.asObject() ?: return null
        return message["content"]?.jsonPrimitive?.contentOrNull
    }

    private fun HttpResponse<String>.modelIds(json: Json): List<String> {
        val root = runCatching { json.parseToJsonElement(body()).jsonObject }.getOrNull() ?: return emptyList()
        return root["data"]
            ?.asArray()
            ?.mapNotNull { item -> item.asObject()?.stringOrNull("id") }
            .orEmpty()
    }
}

private enum class DesktopAxisBandRequest(
    val warningPrefix: String,
    val prompt: String,
) {
    X(
        warningPrefix = "x",
        prompt = """
            Read visible X-axis numeric tick labels from this chromatogram crop.
            Return only this compact JSON object:
            {"unit":"min|s|null","ticks":[{"text":"5.00","value":5.0,"position":0.0}],"confidence":0.0,"warnings":[]}
            Position is normalized left-to-right inside the image.
            Include only numeric tick labels visible in the crop. Do not infer hidden ticks.
            Use dot decimals and no thousands separators in numeric values.
        """.trimIndent(),
    ),
    Y(
        warningPrefix = "y",
        prompt = """
            Read visible Y-axis numeric tick labels from this chromatogram crop.
            Return only this compact JSON object:
            {"unit":"Abundance|Intensity|null","ticks":[{"text":"100000","value":100000.0,"position":0.0}],"confidence":0.0,"warnings":[]}
            Position is normalized top-to-bottom inside the image.
            Include only numeric tick labels visible in the crop. Do not infer hidden ticks.
            Use dot decimals and no thousands separators in numeric values.
        """.trimIndent(),
    ),
}

private data class DesktopVlmTextResult(
    val content: String?,
    val warnings: List<String>,
)

private fun DesktopVlmTextResult.withWarnings(upstreamWarnings: List<String>): DesktopVlmTextResult =
    copy(warnings = (upstreamWarnings + warnings).distinct())

private data class DesktopVlmPreflightResult(
    val ready: Boolean,
    val model: String?,
    val warnings: List<String>,
)

private data class AxisBandSet(
    val panel: AxisBandRect,
    val xBand: AxisBandRect,
    val yBand: AxisBandRect,
    val titleBand: AxisBandRect,
) {
    val isUsable: Boolean get() = xBand.isUsable && yBand.isUsable && titleBand.isUsable

    companion object {
        fun from(imageWidth: Int, imageHeight: Int, region: GraphRegion): AxisBandSet {
            val panel = AxisBandRect(
                x = (region.x - region.width * 0.02f).toInt().coerceAtLeast(0),
                y = (region.y - region.height * 0.02f).toInt().coerceAtLeast(0),
                width = (region.width * 1.04f).toInt(),
                height = (region.height * 1.05f).toInt(),
            ).clamp(imageWidth, imageHeight)
            val xHeight = (panel.height * 0.24f).toInt().coerceAtLeast(28).coerceAtMost(panel.height)
            val yWidth = (panel.width * 0.24f).toInt().coerceAtLeast(38).coerceAtMost(panel.width)
            val titleHeight = (panel.height * 0.22f).toInt().coerceAtLeast(26).coerceAtMost(panel.height)
            return AxisBandSet(
                panel = panel,
                xBand = AxisBandRect(panel.x, panel.bottom - xHeight, panel.width, xHeight).clamp(imageWidth, imageHeight),
                yBand = AxisBandRect(panel.x, panel.y, yWidth, panel.height).clamp(imageWidth, imageHeight),
                titleBand = AxisBandRect(panel.x, panel.y, panel.width, titleHeight).clamp(imageWidth, imageHeight),
            )
        }
    }
}

private data class AxisBandRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    val right: Int get() = x + width
    val bottom: Int get() = y + height
    val isUsable: Boolean get() = width > 4 && height > 4

    fun clamp(imageWidth: Int, imageHeight: Int): AxisBandRect {
        val safeX = x.coerceIn(0, imageWidth.coerceAtLeast(1) - 1)
        val safeY = y.coerceIn(0, imageHeight.coerceAtLeast(1) - 1)
        val safeRight = right.coerceIn(safeX + 1, imageWidth.coerceAtLeast(safeX + 1))
        val safeBottom = bottom.coerceIn(safeY + 1, imageHeight.coerceAtLeast(safeY + 1))
        return copy(x = safeX, y = safeY, width = safeRight - safeX, height = safeBottom - safeY)
    }
}

private data class AxisBandModelResult(
    val xTicks: List<AxisBandTick>,
    val yTicks: List<AxisBandTick>,
    val xUnit: String?,
    val yUnit: String?,
    val confidence: Float?,
    val warnings: List<String>,
) {
    fun toAxisOcrResult(
        bands: AxisBandSet,
        minConfidence: Float,
        upstreamWarnings: List<String>,
    ): AxisOcrResult {
        val xElements = xTicks.map { tick -> tick.toTextElement(bands.xBand, AxisBandDirection.HORIZONTAL) }
        val yElements = yTicks.map { tick -> tick.toTextElement(bands.yBand, AxisBandDirection.VERTICAL) }
        val allTicks = xTicks + yTicks
        val tickConfidences = allTicks.mapNotNull { it.confidence }
        val averageConfidence = when {
            tickConfidences.isNotEmpty() -> tickConfidences.average().toFloat()
            allTicks.isNotEmpty() -> confidence
            else -> null
        }
        val accepted = xTicks.distinctValueCount() >= 2 &&
            yTicks.distinctValueCount() >= 2 &&
            (averageConfidence ?: 0f) >= minConfidence
        val acceptanceWarnings = buildList {
            if (xTicks.distinctValueCount() < 2) add("desktop_axis_vlm.x_requires_two_ticks")
            if (yTicks.distinctValueCount() < 2) add("desktop_axis_vlm.y_requires_two_ticks")
            if (averageConfidence == null) {
                add("desktop_axis_vlm.confidence_missing")
            } else if (averageConfidence < minConfidence) {
                add("desktop_axis_vlm.confidence_below_threshold")
            }
        }
        return AxisOcrResult(
            rawElements = xElements + yElements,
            suggestedXValues = xTicks.map { it.value }.distinct().sorted(),
            suggestedYValues = yTicks.map { it.value }.distinct().sortedDescending(),
            xUnit = xUnit,
            yUnit = yUnit,
            status = if (accepted) OcrStatus.AUTO_ACCEPTED else OcrStatus.NOT_AVAILABLE,
            confidence = averageConfidence,
            warnings = (upstreamWarnings + warnings + acceptanceWarnings).distinct(),
            timestamp = System.currentTimeMillis(),
        )
    }
}

private data class AxisBandTick(
    val text: String,
    val value: Float,
    val position: Float,
    val confidence: Float?,
) {
    fun toTextElement(rect: AxisBandRect, direction: AxisBandDirection): OcrTextElement {
        val textWidth = (text.length * 7f).coerceAtLeast(14f)
        val textHeight = 12f
        val centerX = when (direction) {
            AxisBandDirection.HORIZONTAL -> rect.x + position.coerceIn(0f, 1f) * rect.width
            AxisBandDirection.VERTICAL -> rect.x + rect.width * 0.5f
        }
        val centerY = when (direction) {
            AxisBandDirection.HORIZONTAL -> rect.y + rect.height * 0.56f
            AxisBandDirection.VERTICAL -> rect.y + position.coerceIn(0f, 1f) * rect.height
        }
        return OcrTextElement(
            text = text,
            numericValue = value,
            x = centerX - textWidth / 2f,
            y = centerY - textHeight / 2f,
            width = textWidth,
            height = textHeight,
            confidence = confidence ?: 0.5f,
        )
    }
}

private enum class AxisBandDirection {
    HORIZONTAL,
    VERTICAL,
}

private fun parseAxisBandModelResult(rawResponse: String?): AxisBandModelResult? {
    val jsonText = rawResponse?.extractFirstJsonObject() ?: return null
    val root = runCatching { Json.parseToJsonElement(jsonText).jsonObject }.getOrNull() ?: return null
    val xAxis = root["xAxis"]?.asObject()
    val yAxis = root["yAxis"]?.asObject()
    return AxisBandModelResult(
        xTicks = xAxis.parseTicks(),
        yTicks = yAxis.parseTicks(),
        xUnit = xAxis?.stringOrNull("unit")?.takeUnless { it.equals("null", ignoreCase = true) },
        yUnit = yAxis?.stringOrNull("unit")?.takeUnless { it.equals("null", ignoreCase = true) },
        confidence = root.floatOrNull("confidence"),
        warnings = root.stringArrayOrEmpty("warnings"),
    )
}

private fun JsonObject?.parseTicks(): List<AxisBandTick> {
    val ticks = this?.get("ticks")?.asArray() ?: return emptyList()
    return ticks.mapNotNull { element ->
        val obj = element.asObject() ?: return@mapNotNull null
        val text = obj.stringOrNull("text").orEmpty()
        val value = obj.floatOrNull("value") ?: text.parseAxisFloat() ?: return@mapNotNull null
        val position = obj.floatOrNull("position") ?: obj.floatOrNull("normalizedPosition") ?: return@mapNotNull null
        if (position !in -0.03f..1.03f) return@mapNotNull null
        AxisBandTick(
            text = text.ifBlank { value.toString() },
            value = value,
            position = position.coerceIn(0f, 1f),
            confidence = obj.floatOrNull("confidence"),
        )
    }
}

private fun List<AxisBandTick>.distinctValueCount(): Int =
    map { it.value }
        .distinctBy { (it * 100f).toInt() }
        .size

private fun BufferedImage.crop(rect: AxisBandRect): BufferedImage =
    getSubimage(rect.x, rect.y, rect.width, rect.height)

private fun BufferedImage.toPngDataUrl(): String {
    val bytes = ByteArrayOutputStream()
    ImageIO.write(this, "png", bytes)
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray())
}

private fun unavailableAxisOcrResult(vararg warnings: String): AxisOcrResult =
    unavailableAxisOcrResult(warnings.toList())

private fun unavailableAxisOcrResult(warnings: List<String>): AxisOcrResult = AxisOcrResult(
    rawElements = emptyList(),
    suggestedXValues = emptyList(),
    suggestedYValues = emptyList(),
    xUnit = null,
    yUnit = null,
    status = OcrStatus.NOT_AVAILABLE,
    warnings = warnings.distinct(),
    timestamp = System.currentTimeMillis(),
)

private fun String.extractFirstJsonObject(): String? {
    val start = indexOf('{')
    if (start < 0) return null
    var depth = 0
    var inString = false
    var escaped = false
    for (index in start until length) {
        val char = this[index]
        if (escaped) {
            escaped = false
            continue
        }
        if (char == '\\' && inString) {
            escaped = true
            continue
        }
        if (char == '"') {
            inString = !inString
            continue
        }
        if (inString) continue
        when (char) {
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return substring(start, index + 1)
            }
        }
    }
    return null
}

private fun String.parseJsonObject(json: Json): JsonObject? =
    runCatching { json.parseToJsonElement(this).jsonObject }.getOrNull()

private fun emptyAxisObject(unit: String): JsonObject = buildJsonObject {
    put("unit", unit)
    put("ticks", buildJsonArray { })
    put("confidence", 0.0)
    put("warnings", buildJsonArray { })
}

private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

private fun JsonElement.asArray(): JsonArray? = this as? JsonArray

private fun JsonObject.stringOrNull(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull

private fun JsonObject.floatOrNull(key: String): Float? =
    stringOrNull(key)?.parseAxisFloat()

private fun JsonObject.stringArrayOrEmpty(key: String): List<String> =
    this[key]
        ?.asArray()
        ?.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf { warning -> warning.isNotBlank() } }
        .orEmpty()

private fun String.parseAxisFloat(): Float? {
    val match = Regex("""-?\d+(?:[.,]\d+)?""").find(replace(" ", "")) ?: return null
    return match.value.replace(',', '.').toFloatOrNull()
}
