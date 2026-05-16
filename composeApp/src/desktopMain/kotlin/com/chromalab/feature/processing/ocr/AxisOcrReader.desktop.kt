package com.chromalab.feature.processing.ocr

import com.chromalab.feature.processing.graph.GraphRegion
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
        val config = DesktopAxisVlmConfig.fromEnvironment() ?: return@withContext unavailableAxisOcrResult()
        runCatching {
            val image = ImageIO.read(File(imagePath)) ?: return@runCatching unavailableAxisOcrResult()
            val bands = AxisBandSet.from(image.width, image.height, graphRegion)
            if (!bands.isUsable) return@runCatching unavailableAxisOcrResult()

            val rawResponse = DesktopOpenAiVisionClient(config).readAxisBands(
                xBandImage = image.crop(bands.xBand),
                yBandImage = image.crop(bands.yBand),
                titleBandImage = image.crop(bands.titleBand),
            )
            val modelResult = parseAxisBandModelResult(rawResponse) ?: return@runCatching unavailableAxisOcrResult()
            modelResult.toAxisOcrResult(bands, config.minConfidence)
        }.getOrElse {
            unavailableAxisOcrResult()
        }
    }
}

private data class DesktopAxisVlmConfig(
    val baseUrl: String,
    val model: String,
    val timeoutMs: Long,
    val minConfidence: Float,
) {
    companion object {
        fun fromEnvironment(): DesktopAxisVlmConfig? {
            val baseUrl = System.getenv("CHROMALAB_DESKTOP_VLM_BASE_URL")
                ?.trim()
                ?.trimEnd('/')
                ?.takeIf { it.isNotBlank() }
                ?: return null
            val model = System.getenv("CHROMALAB_DESKTOP_VLM_MODEL")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "local-model"
            val timeoutMs = System.getenv("CHROMALAB_DESKTOP_VLM_TIMEOUT_MS")
                ?.toLongOrNull()
                ?.coerceIn(5_000L, 180_000L)
                ?: 90_000L
            val minConfidence = System.getenv("CHROMALAB_DESKTOP_VLM_MIN_CONFIDENCE")
                ?.toFloatOrNull()
                ?.coerceIn(0.1f, 0.99f)
                ?: 0.65f
            return DesktopAxisVlmConfig(baseUrl, model, timeoutMs, minConfidence)
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

    fun readAxisBands(
        xBandImage: BufferedImage,
        yBandImage: BufferedImage,
        titleBandImage: BufferedImage,
    ): String? {
        val payload = buildJsonObject {
            put("model", config.model)
            put("temperature", 0)
            put("max_tokens", 900)
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put(
                                "content",
                                "You extract chromatogram axis tick labels from cropped axis bands. Return strict JSON only.",
                            )
                        },
                    )
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put("content", buildContentArray(xBandImage, yBandImage, titleBandImage))
                        },
                    )
                },
            )
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${config.baseUrl}/chat/completions"))
            .timeout(Duration.ofMillis(config.timeoutMs))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) return null
        return response.contentText(json)
    }

    private fun buildContentArray(
        xBandImage: BufferedImage,
        yBandImage: BufferedImage,
        titleBandImage: BufferedImage,
    ): JsonArray = buildJsonArray {
        add(
            buildJsonObject {
                put("type", "text")
                put(
                    "text",
                    """
                    Read the chromatogram scale from three cropped images.
                    Image 1 is the X-axis label/tick band. Image 2 is the Y-axis label/tick band. Image 3 is the graph title/ion band.
                    Return only this JSON schema:
                    {
                      "xAxis": {
                        "unit": "min|s|null",
                        "ticks": [{"text":"5.00","value":5.0,"position":0.0,"confidence":0.0}]
                      },
                      "yAxis": {
                        "unit": "Abundance|Intensity|null",
                        "ticks": [{"text":"100000","value":100000.0,"position":0.0,"confidence":0.0}]
                      },
                      "title": "visible title or null",
                      "confidence": 0.0,
                      "warnings": []
                    }
                    position is normalized: X ticks left-to-right inside image 1, Y ticks top-to-bottom inside image 2.
                    Include only numeric tick labels visible in the crops. Do not infer hidden ticks.
                    Use dot decimals and no thousands separators in numeric values.
                    """.trimIndent(),
                )
            },
        )
        add(imageElement(xBandImage))
        add(imageElement(yBandImage))
        add(imageElement(titleBandImage))
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
}

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
) {
    fun toAxisOcrResult(bands: AxisBandSet, minConfidence: Float): AxisOcrResult {
        val xElements = xTicks.map { tick -> tick.toTextElement(bands.xBand, AxisBandDirection.HORIZONTAL) }
        val yElements = yTicks.map { tick -> tick.toTextElement(bands.yBand, AxisBandDirection.VERTICAL) }
        val allTicks = xTicks + yTicks
        val averageConfidence = allTicks
            .map { it.confidence ?: 0.5f }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()
            ?: confidence
        val accepted = xTicks.distinctValueCount() >= 2 &&
            yTicks.distinctValueCount() >= 2 &&
            (averageConfidence ?: 0f) >= minConfidence
        return AxisOcrResult(
            rawElements = xElements + yElements,
            suggestedXValues = xTicks.map { it.value }.distinct().sorted(),
            suggestedYValues = yTicks.map { it.value }.distinct().sortedDescending(),
            xUnit = xUnit,
            yUnit = yUnit,
            status = if (accepted) OcrStatus.AUTO_ACCEPTED else OcrStatus.NOT_AVAILABLE,
            confidence = averageConfidence,
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

private fun unavailableAxisOcrResult(): AxisOcrResult = AxisOcrResult(
    rawElements = emptyList(),
    suggestedXValues = emptyList(),
    suggestedYValues = emptyList(),
    xUnit = null,
    yUnit = null,
    status = OcrStatus.NOT_AVAILABLE,
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

private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

private fun JsonElement.asArray(): JsonArray? = this as? JsonArray

private fun JsonObject.stringOrNull(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull

private fun JsonObject.floatOrNull(key: String): Float? =
    stringOrNull(key)?.parseAxisFloat()

private fun String.parseAxisFloat(): Float? {
    val match = Regex("""-?\d+(?:[.,]\d+)?""").find(replace(" ", "")) ?: return null
    return match.value.replace(',', '.').toFloatOrNull()
}
