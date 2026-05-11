package com.chromalab.feature.processing.inference

/**
 * Professional VLM prompts for chromatogram analysis.
 *
 * Design principles (LLM inference best practices):
 *
 * 1. **SYSTEM role defines the persona** — narrow the model to a specific expert.
 *    A constrained persona prevents creative hallucination.
 *
 * 2. **No example values in prompts** — examples with fake data cause
 *    small models (3B–9B) to copy them verbatim. We show JSON SCHEMA only,
 *    with placeholder brackets like [numbers].
 *
 * 3. **Explicit negative constraints** — "Do NOT invent", "Do NOT interpolate".
 *    Small models hallucinate less when told what NOT to do.
 *
 * 4. **Output format last** — the model remembers the end of the prompt best
 *    (recency bias). Place the JSON schema right before generation starts.
 *
 * 5. **Per-model prompt format** — each model family has its own expected format:
 *    - Qwen: ChatML (<|im_start|>system/user/assistant)
 *    - PaddleOCR-VL: Trigger phrases ("Chart Recognition:")
 *    - DeepSeek-OCR: Grounding marker (<|grounding|>)
 *    - Moondream2: Direct question (no roles)
 *    - SmolVLM2: Raw system+user concatenation
 *    See [PromptStyle] for the full mapping.
 *
 * 6. **Greedy sampling (temp=0)** — set at the C++ sampler level.
 *    The prompt does NOT control sampling, but is designed for deterministic output.
 */
object ChartPrompts {

    // ─── ChatML template tokens ─────────────────────────────────
    // These are text tokens (not special IDs) — llama.cpp mtmd_tokenize
    // with parse_special=true will convert them to proper token IDs.
    private const val IM_START = "<|im_start|>"
    private const val IM_END = "<|im_end|>"

    /**
     * Wrap content in ChatML format for Qwen VL Instruct models.
     *
     * Format:
     * <|im_start|>system
     * {system}<|im_end|>
     * <|im_start|>user
     * {user}<|im_end|>
     * <|im_start|>assistant
     *
     * The trailing newline after "assistant" is intentional —
     * it primes the model to begin generating immediately.
     */
    private fun chatML(system: String, user: String): String {
        return buildString {
            append("${IM_START}system\n")
            append(system)
            append("${IM_END}\n")
            append("${IM_START}user\n")
            append(user)
            append("${IM_END}\n")
            append("${IM_START}assistant\n")
        }
    }

    // ─── AXIS_EXTRACTION ────────────────────────────────────────
    //
    // Primary prompt — reads numeric tick labels from both axes.
    // This is the core VLM task: OCR of axis values from a chart photo.

    private val AXIS_SYSTEM = """
You are a scientific instrument data reader specialized in chromatography.
You read numeric labels printed on chart axes with perfect accuracy.
You never invent, estimate, or interpolate values.
You report only what is visually printed on the image.
""".trimIndent()

    private val AXIS_USER = """
Read all visible numeric tick labels on the X-axis and Y-axis of this chromatography chart.

RULES:
1. X-axis: horizontal axis at the bottom of the chart (typically Time).
2. Y-axis: vertical axis on the left side of the chart (typically Signal/Abundance).
3. Report ONLY numbers that are actually printed as tick labels on the axes.
4. Do NOT invent values between printed ticks.
5. Do NOT estimate or round — copy the exact printed numbers.
6. If an axis has no visible numeric labels, return an empty array for that axis.
7. If units are printed near the axis (e.g. "min", "mAU"), include them.
8. Numbers may use comma or period as decimal separator — normalize to period.

Respond with ONLY this JSON, no other text:
{"x": [<numbers>], "y": [<numbers>], "x_unit": "<unit or null>", "y_unit": "<unit or null>"}
""".trimIndent()

    /** ChatML-formatted axis extraction prompt for Qwen VL Instruct models. */
    val AXIS_EXTRACTION: String = chatML(AXIS_SYSTEM, AXIS_USER)

    /**
     * Raw axis extraction prompt (no ChatML wrapper).
     * Used for non-Qwen models (Gemma via LiteRT) that don't expect ChatML.
     */
    val AXIS_EXTRACTION_RAW: String = "$AXIS_SYSTEM\n\n$AXIS_USER"

    // ─── GRAPH_REGION ───────────────────────────────────────────
    //
    // Detects the bounding box of the plot area within a photograph.
    // Used as a VLM hint for the CV-based GraphRegionDetector.

    private val REGION_SYSTEM = """
You are a document layout analyzer for scientific papers and reports.
You identify the exact boundaries of chart plot areas in photographs.
""".trimIndent()

    private val REGION_USER = """
Locate the chart plot area in this image. The plot area is the rectangular region where the data curve is drawn, bounded by the axes. Do NOT include axis labels, titles, or legends.

If there are multiple separate charts, count them.

Respond with ONLY this JSON, no other text:
{"left_pct": <0-100>, "top_pct": <0-100>, "right_pct": <0-100>, "bottom_pct": <0-100>, "num_graphs": <integer>}

Values are percentages of image width and height from the top-left corner.
""".trimIndent()

    val GRAPH_REGION: String = chatML(REGION_SYSTEM, REGION_USER)
    val GRAPH_REGION_RAW: String = "$REGION_SYSTEM\n\n$REGION_USER"

    // ─── AXIS_STRUCTURE ─────────────────────────────────────────
    //
    // Identifies axis positions and chart structural metadata.
    // Lightweight hint for the CV axis detector.

    private val STRUCTURE_SYSTEM = """
You are a scientific chart structure analyzer.
You identify axis positions and visual properties of charts.
""".trimIndent()

    private val STRUCTURE_USER = """
Describe the axis structure of this chart.

Respond with ONLY this JSON, no other text:
{"x_axis_position": "bottom", "y_axis_position": "left", "has_secondary_y": false, "grid_visible": true}

Valid values:
- x_axis_position: "bottom" or "top"
- y_axis_position: "left" or "right"
- has_secondary_y: true if there is a second Y-axis on the opposite side
- grid_visible: true if gridlines are drawn on the plot area
""".trimIndent()

    val AXIS_STRUCTURE: String = chatML(STRUCTURE_SYSTEM, STRUCTURE_USER)
    val AXIS_STRUCTURE_RAW: String = "$STRUCTURE_SYSTEM\n\n$STRUCTURE_USER"

    // ─── PaddleOCR-VL trigger prompts ───────────────────────────
    //
    // PaddleOCR-VL 1.5 uses task-specific trigger phrases as the ENTIRE prompt.
    // No system/user roles. The trigger phrase routes the model's behavior.
    // Reference: https://huggingface.co/PaddlePaddle/PaddleOCR-VL-1.5

    /** PaddleOCR-VL: detect chart area and read structure. */
    val PADDLE_CHART = "Chart Recognition:"
    /** PaddleOCR-VL: pure text OCR (axis labels, numbers). */
    val PADDLE_OCR = "OCR:"

    // ─── DeepSeek-OCR prompts ───────────────────────────────────
    //
    // DeepSeek-OCR requires <|grounding|> prefix marker.
    // Outputs Markdown-structured text with layout info.
    // Reference: https://huggingface.co/deepseek-ai/DeepSeek-OCR

    /** DeepSeek-OCR: full document/chart to markdown. */
    val DEEPSEEK_CHART = "<|grounding|>Convert the document to markdown."
    /** DeepSeek-OCR: plain text extraction. */
    val DEEPSEEK_OCR_TEXT = "<|grounding|>Free OCR."

    // ─── Moondream2 direct prompts ──────────────────────────────
    //
    // Moondream2 uses plain question strings, no roles.
    // For structured output, ask for JSON explicitly.
    // Reference: https://github.com/vikhyat/moondream

    private val MOONDREAM_REGION = """
Locate the chart plot area in this image. The plot area is the rectangle where the data curve is drawn, bounded by the axes.
Respond with ONLY this JSON: {"left_pct": <0-100>, "top_pct": <0-100>, "right_pct": <0-100>, "bottom_pct": <0-100>, "num_graphs": <integer>}
""".trimIndent()

    private val MOONDREAM_AXIS = """
Read all numeric tick labels on the X-axis and Y-axis of this chromatography chart.
Report ONLY numbers that are actually printed as tick labels. Do NOT invent values.
Respond with ONLY this JSON: {"x": [<numbers>], "y": [<numbers>], "x_unit": "<unit or null>", "y_unit": "<unit or null>"}
""".trimIndent()

    private val MOONDREAM_STRUCTURE = """
Describe the axis structure of this chart.
Respond with ONLY this JSON: {"x_axis_position": "bottom", "y_axis_position": "left", "has_secondary_y": false, "grid_visible": true}
""".trimIndent()

    // ─── Prompt routing ─────────────────────────────────────────
    //
    // These functions select the correct prompt variant based on
    // the model's PromptStyle. Called from ChartAnalysisReader.

    /**
     * Select the graph region detection prompt for this model.
     */
    fun graphRegionPrompt(style: PromptStyle): String = when (style) {
        PromptStyle.CHATML -> GRAPH_REGION
        PromptStyle.TRIGGER -> PADDLE_CHART
        PromptStyle.DEEPSEEK_OCR -> DEEPSEEK_CHART
        PromptStyle.DIRECT_QUESTION -> MOONDREAM_REGION
        PromptStyle.RAW -> GRAPH_REGION_RAW
        PromptStyle.LITERT -> GRAPH_REGION_RAW
    }

    /**
     * Select the axis extraction prompt for this model.
     */
    fun axisExtractionPrompt(style: PromptStyle): String = when (style) {
        PromptStyle.CHATML -> AXIS_EXTRACTION
        PromptStyle.TRIGGER -> PADDLE_OCR
        PromptStyle.DEEPSEEK_OCR -> DEEPSEEK_OCR_TEXT
        PromptStyle.DIRECT_QUESTION -> MOONDREAM_AXIS
        PromptStyle.RAW -> AXIS_EXTRACTION_RAW
        PromptStyle.LITERT -> AXIS_EXTRACTION_RAW
    }

    /**
     * Select the axis structure prompt for this model.
     */
    fun axisStructurePrompt(style: PromptStyle): String = when (style) {
        PromptStyle.CHATML -> AXIS_STRUCTURE
        PromptStyle.TRIGGER -> PADDLE_CHART
        PromptStyle.DEEPSEEK_OCR -> DEEPSEEK_CHART
        PromptStyle.DIRECT_QUESTION -> MOONDREAM_STRUCTURE
        PromptStyle.RAW -> AXIS_STRUCTURE_RAW
        PromptStyle.LITERT -> AXIS_STRUCTURE_RAW
    }

    // ─── Response parsing ───────────────────────────────────────

    /**
     * Parse VLM text response into [ChartAnalysis].
     *
     * Handles:
     * - Markdown code blocks (```json ... ```)
     * - ChatML leftovers (thinking tags, role markers)
     * - Trailing garbage after the JSON object
     * - Nested braces in JSON
     */
    fun parseResponse(response: String): ChartAnalysis {
        val jsonStr = extractJson(response)

        return try {
            val xValues = extractFloatArray(jsonStr, "\"x\"")
            val yValues = extractFloatArray(jsonStr, "\"y\"")
            val xUnit = extractString(jsonStr, "\"x_unit\"")
            val yUnit = extractString(jsonStr, "\"y_unit\"")

            // Confidence heuristic: more values = higher confidence
            val totalValues = xValues.size + yValues.size
            val confidence = when {
                totalValues >= 10 -> 0.95f
                totalValues >= 5 -> 0.85f
                totalValues >= 2 -> 0.7f
                totalValues >= 1 -> 0.5f
                else -> 0.1f
            }

            println("VLM[PARSE] Parsed: ${xValues.size} X, ${yValues.size} Y, conf=$confidence")
            if (xValues.isNotEmpty()) println("VLM[PARSE] X: ${xValues.joinToString()}")
            if (yValues.isNotEmpty()) println("VLM[PARSE] Y: ${yValues.joinToString()}")

            ChartAnalysis(
                xValues = xValues,
                yValues = yValues,
                xUnit = xUnit,
                yUnit = yUnit,
                confidence = confidence,
            )
        } catch (e: Exception) {
            println("VLM[PARSE] Failed: ${e.message}")
            println("VLM[PARSE] Raw JSON: $jsonStr")
            ChartAnalysis(
                xValues = emptyList(),
                yValues = emptyList(),
                confidence = 0f,
            )
        }
    }

    /**
     * Parse graph region detection response.
     */
    fun parseGraphRegion(response: String): GraphBounds? {
        val jsonStr = extractJson(response)
        return try {
            val left = extractFloat(jsonStr, "\"left_pct\"")
            val top = extractFloat(jsonStr, "\"top_pct\"")
            val right = extractFloat(jsonStr, "\"right_pct\"")
            val bottom = extractFloat(jsonStr, "\"bottom_pct\"")
            val numGraphs = extractInt(jsonStr, "\"num_graphs\"")

            if (left != null && top != null && right != null && bottom != null &&
                right > left && bottom > top
            ) {
                println("VLM[PARSE] GraphBounds: L=$left T=$top R=$right B=$bottom, graphs=${numGraphs ?: 1}")
                GraphBounds(
                    leftPct = left,
                    topPct = top,
                    rightPct = right,
                    bottomPct = bottom,
                    numGraphs = numGraphs ?: 1,
                )
            } else {
                println("VLM[PARSE] Invalid bounds: L=$left T=$top R=$right B=$bottom")
                null
            }
        } catch (e: Exception) {
            println("VLM[PARSE] GraphRegion parse failed: ${e.message}")
            null
        }
    }

    /**
     * Parse axis structure response.
     */
    fun parseAxisStructure(response: String): AxisStructure? {
        val jsonStr = extractJson(response)
        return try {
            val xPos = extractString(jsonStr, "\"x_axis_position\"") ?: "bottom"
            val yPos = extractString(jsonStr, "\"y_axis_position\"") ?: "left"
            val hasSecondaryY = extractBool(jsonStr, "\"has_secondary_y\"")
            val gridVisible = extractBool(jsonStr, "\"grid_visible\"")

            println("VLM[PARSE] AxisStructure: x=$xPos y=$yPos secondaryY=$hasSecondaryY grid=$gridVisible")
            AxisStructure(
                xAxisPosition = xPos,
                yAxisPosition = yPos,
                hasSecondaryY = hasSecondaryY ?: false,
                gridVisible = gridVisible ?: false,
            )
        } catch (e: Exception) {
            println("VLM[PARSE] AxisStructure parse failed: ${e.message}")
            null
        }
    }

    // ─── JSON extraction helpers ────────────────────────────────

    /**
     * Extract the JSON object from a VLM response.
     * Strips markdown code fences, ChatML tokens, thinking blocks.
     */
    private fun extractJson(response: String): String {
        var text = response
            // Strip ChatML markers that may leak into output
            .replace(IM_START, "")
            .replace(IM_END, "")
            .replace("<|endoftext|>", "")
            // Strip markdown code fences
            .replace("```json", "")
            .replace("```", "")

        // Strip <think>...</think> blocks (Qwen3.5 thinking mode)
        val thinkStart = text.indexOf("<think>")
        val thinkEnd = text.indexOf("</think>")
        if (thinkStart >= 0 && thinkEnd > thinkStart) {
            text = text.substring(0, thinkStart) + text.substring(thinkEnd + "</think>".length)
        }

        text = text.trim()

        // Find the JSON object — match first { to its closing }
        val start = text.indexOf('{')
        if (start < 0) return text

        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }

        // Fallback: return from first { to end
        return text.substring(start)
    }

    private fun extractFloatArray(json: String, key: String): List<Float> {
        val keyIdx = json.indexOf(key)
        if (keyIdx < 0) return emptyList()
        val arrStart = json.indexOf('[', keyIdx)
        val arrEnd = json.indexOf(']', arrStart)
        if (arrStart < 0 || arrEnd < 0) return emptyList()
        val arrContent = json.substring(arrStart + 1, arrEnd).trim()
        if (arrContent.isEmpty()) return emptyList()
        return arrContent.split(',').mapNotNull { token ->
            // Handle comma as decimal separator: "1,5" → "1.5"
            val cleaned = token.trim().replace(',', '.')
            cleaned.toFloatOrNull()
        }
    }

    private fun extractString(json: String, key: String): String? {
        val keyIdx = json.indexOf(key)
        if (keyIdx < 0) return null
        val colonIdx = json.indexOf(':', keyIdx)
        if (colonIdx < 0) return null
        val after = json.substring(colonIdx + 1).trim()
        if (after.startsWith("null")) return null
        val quoteStart = after.indexOf('"')
        if (quoteStart < 0) return null
        val quoteEnd = after.indexOf('"', quoteStart + 1)
        if (quoteEnd < 0) return null
        val value = after.substring(quoteStart + 1, quoteEnd)
        return if (value.isBlank() || value == "null") null else value
    }

    private fun extractFloat(json: String, key: String): Float? {
        val keyIdx = json.indexOf(key)
        if (keyIdx < 0) return null
        val colonIdx = json.indexOf(':', keyIdx)
        if (colonIdx < 0) return null
        val after = json.substring(colonIdx + 1).trimStart()
        // Read until comma, }, or whitespace
        val numStr = buildString {
            for (ch in after) {
                if (ch.isDigit() || ch == '.' || ch == '-') append(ch)
                else if (isNotEmpty()) break
            }
        }
        return numStr.toFloatOrNull()
    }

    private fun extractInt(json: String, key: String): Int? {
        return extractFloat(json, key)?.toInt()
    }

    private fun extractBool(json: String, key: String): Boolean? {
        val keyIdx = json.indexOf(key)
        if (keyIdx < 0) return null
        val colonIdx = json.indexOf(':', keyIdx)
        if (colonIdx < 0) return null
        val after = json.substring(colonIdx + 1).trimStart()
        return when {
            after.startsWith("true") -> true
            after.startsWith("false") -> false
            else -> null
        }
    }
}

// ─── Data classes for new VLM hint results ──────────────────────

/**
 * VLM-detected bounding box of the chart plot area.
 * Values are percentages of image dimensions (0–100).
 */
data class GraphBounds(
    val leftPct: Float,
    val topPct: Float,
    val rightPct: Float,
    val bottomPct: Float,
    val numGraphs: Int = 1,
)

/**
 * VLM-detected axis structure metadata.
 */
data class AxisStructure(
    val xAxisPosition: String = "bottom",
    val yAxisPosition: String = "left",
    val hasSecondaryY: Boolean = false,
    val gridVisible: Boolean = false,
)
