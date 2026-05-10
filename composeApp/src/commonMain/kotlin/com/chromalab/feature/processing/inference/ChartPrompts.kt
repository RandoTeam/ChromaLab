package com.chromalab.feature.processing.inference

/**
 * Standard prompt for chromatogram axis extraction.
 * Used by both LiteRT and llama.cpp engines.
 */
object ChartPrompts {

    /**
     * Prompt to extract axis values from a chromatogram graph crop.
     * Returns structured JSON that can be parsed into ChartAnalysis.
     */
    val AXIS_EXTRACTION = """
Analyze this chromatogram graph image. Extract:
1. All X-axis tick values (usually Time in minutes)
2. All Y-axis tick values (usually Abundance or Intensity)
3. Units for each axis

Return ONLY valid JSON, no other text:
{"x": [35, 40, 45, 50, 55, 60, 65], "y": [0, 50, 100, 150, 200, 250, 300, 350], "x_unit": "min", "y_unit": "Abundance"}
""".trimIndent()

    /**
     * Parse VLM text response into ChartAnalysis.
     * Extracts JSON from the response, handling markdown code blocks.
     */
    fun parseResponse(response: String): ChartAnalysis {
        // Extract JSON from response (may be wrapped in ```json ... ```)
        val jsonStr = response
            .replace("```json", "")
            .replace("```", "")
            .trim()
            .let { text ->
                // Find first { and last }
                val start = text.indexOf('{')
                val end = text.lastIndexOf('}')
                if (start >= 0 && end > start) text.substring(start, end + 1) else text
            }

        return try {
            // Manual JSON parsing to avoid heavy serialization dependency
            val xValues = extractFloatArray(jsonStr, "\"x\"")
            val yValues = extractFloatArray(jsonStr, "\"y\"")
            val xUnit = extractString(jsonStr, "\"x_unit\"")
            val yUnit = extractString(jsonStr, "\"y_unit\"")

            ChartAnalysis(
                xValues = xValues,
                yValues = yValues,
                xUnit = xUnit,
                yUnit = yUnit,
                confidence = if (xValues.isNotEmpty() || yValues.isNotEmpty()) 0.9f else 0.1f,
            )
        } catch (e: Exception) {
            println("VLM[PARSE] Failed to parse response: ${e.message}")
            println("VLM[PARSE] Raw response: $jsonStr")
            ChartAnalysis(
                xValues = emptyList(),
                yValues = emptyList(),
                confidence = 0f,
            )
        }
    }

    private fun extractFloatArray(json: String, key: String): List<Float> {
        val keyIdx = json.indexOf(key)
        if (keyIdx < 0) return emptyList()
        val arrStart = json.indexOf('[', keyIdx)
        val arrEnd = json.indexOf(']', arrStart)
        if (arrStart < 0 || arrEnd < 0) return emptyList()
        val arrContent = json.substring(arrStart + 1, arrEnd).trim()
        if (arrContent.isEmpty()) return emptyList()
        return arrContent.split(',').mapNotNull { it.trim().toFloatOrNull() }
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
        return after.substring(quoteStart + 1, quoteEnd)
    }
}
