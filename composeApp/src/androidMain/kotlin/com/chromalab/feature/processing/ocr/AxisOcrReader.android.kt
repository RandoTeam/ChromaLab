package com.chromalab.feature.processing.ocr

import android.graphics.BitmapFactory
import com.chromalab.feature.processing.graph.GraphRegion
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Android OCR reader using ML Kit Text Recognition.
 *
 * Strategy:
 * 1. Run ML Kit on the graph region + margins (axis labels are outside the plot area)
 * 2. Extract text blocks with bounding boxes
 * 3. Parse numeric values from text
 * 4. Classify by position:
 *    - Elements below X axis → X axis labels (time values)
 *    - Elements left of Y axis → Y axis labels (intensity values)
 * 5. Sort and return as suggestions
 *
 * Design: NEVER fails. Returns empty result if OCR produces garbage.
 */
actual class AxisOcrReader actual constructor() {

    actual suspend fun readAxisLabels(
        imagePath: String,
        graphRegion: GraphRegion,
    ): AxisOcrResult {
        // === PASS 1: Full-image OCR ===
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: return emptyResult()

        val elements = scanWithMlKit(bitmap)
        bitmap.recycle()

        // === LEVEL 1: Spatial pre-filter ===
        // Only consider elements whose center falls within the graph region + margin.
        // This prevents elements from OTHER graphs on the same sheet from leaking in.
        // Asymmetric: bottom margin is larger because X-axis labels sit below the graph.
        val margin = graphRegion.height * 0.15f
        val bottomMargin = graphRegion.height * 0.25f  // X-axis labels can be farther below
        val graphBottom = graphRegion.y + graphRegion.height
        val graphRight = graphRegion.x + graphRegion.width

        val regionElements = elements.filter { elem ->
            val cx = elem.x + elem.width / 2f
            val cy = elem.y + elem.height / 2f
            cx >= graphRegion.x - margin &&
                cx <= graphRight + margin &&
                cy >= graphRegion.y - margin &&
                cy <= graphBottom + bottomMargin
        }

        println("OCR[RAW] ${elements.size} elements total, ${regionElements.size} in region (${graphRegion.x},${graphRegion.y},${graphRegion.width}x${graphRegion.height}), margin=$margin, bottomMargin=$bottomMargin")

        // === LEVEL 2: Classify by position ===
        val xAxisLabels = mutableListOf<OcrTextElement>()
        val yAxisLabels = mutableListOf<OcrTextElement>()
        var xUnit: String? = null
        var yUnit: String? = null

        // Tighter classification margin (10% not 15%)
        val classMargin = graphRegion.height * 0.10f

        for (elem in regionElements) {
            val cx = elem.x + elem.width / 2f
            val cy = elem.y + elem.height / 2f

            println("OCR[ELEM] text='${elem.text}' num=${elem.numericValue} pos=(${elem.x.toInt()},${elem.y.toInt()}) center=(${cx.toInt()},${cy.toInt()}) size=(${elem.width.toInt()}x${elem.height.toInt()})")

            if (elem.numericValue != null) {
                // X candidates: below graph area (near bottom axis)
                val isXCandidate = cy > graphBottom - classMargin &&
                    cx >= graphRegion.x - classMargin &&
                    cx <= graphRight + classMargin
                // Y candidates: left of graph area (near left axis)
                val isYCandidate = cx < graphRegion.x + classMargin &&
                    cy >= graphRegion.y - classMargin &&
                    cy <= graphBottom + classMargin

                if (isXCandidate && !isYCandidate) {
                    xAxisLabels.add(elem)
                } else if (isYCandidate && !isXCandidate) {
                    yAxisLabels.add(elem)
                } else if (isXCandidate && isYCandidate) {
                    val distToBottom = (graphBottom - cy).coerceAtLeast(0f)
                    val distToLeft = (cx - graphRegion.x).coerceAtLeast(0f)
                    if (distToBottom < distToLeft) {
                        xAxisLabels.add(elem)
                    } else {
                        yAxisLabels.add(elem)
                    }
                }
                // else: element is inside graph area (title, etc.) → skip
            } else {
                // Non-numeric — could be unit labels
                val lower = elem.text.lowercase()
                if (cy > graphBottom - classMargin) {
                    if (lower.contains("мин") || lower.contains("min") ||
                        lower.contains("сек") || lower.contains("sec") ||
                        lower.contains("time")
                    ) {
                        xUnit = elem.text
                    }
                }
                if (cx < graphRegion.x + classMargin) {
                    if (lower.contains("mau") || lower.contains("mv") ||
                        lower.contains("µv") || lower.contains("au") ||
                        lower.contains("intensity") || lower.contains("abundance")
                    ) {
                        yUnit = elem.text
                    }
                }
            }
        }

        println("OCR[CLASSIFY] xRaw=${xAxisLabels.map { it.numericValue }}, yRaw=${yAxisLabels.map { it.numericValue }}")

        // === LEVEL 3: Post-classification filtering ===
        var filteredX = filterAxisValues(xAxisLabels.mapNotNull { it.numericValue })
        var filteredY = filterAxisValues(yAxisLabels.mapNotNull { it.numericValue })

        println("OCR[FILTER] xFiltered=$filteredX, yFiltered=$filteredY")

        // === PASS 2: Always run targeted crop for better coverage ===
        // Cropped strip gives ML Kit zoomed-in view → small labels become large
        val allElements = elements.toMutableList()

        val retryBitmap = BitmapFactory.decodeFile(imagePath)
        if (retryBitmap != null) {
            // X-axis: crop strip below graph
            println("OCR[RETRY-X] Pass 1 found ${filteredX.size} X values, enhancing with X-axis crop...")
            val xRetry = retryAxisStrip(retryBitmap, graphRegion, axis = "X")
            if (xRetry != null && xRetry.values.isNotEmpty()) {
                allElements.addAll(xRetry.elements)
                val mergedX = (filteredX + xRetry.values).distinct().sorted()
                filteredX = filterAxisValues(mergedX)
                if (xRetry.unit != null && xUnit == null) xUnit = xRetry.unit
                println("OCR[RETRY-X] found ${xRetry.values.size} new values, merged=$filteredX")
            }

            // Y-axis: crop strip left of graph
            println("OCR[RETRY-Y] Pass 1 found ${filteredY.size} Y values, enhancing with Y-axis crop...")
            val yRetry = retryAxisStrip(retryBitmap, graphRegion, axis = "Y")
            if (yRetry != null && yRetry.values.isNotEmpty()) {
                allElements.addAll(yRetry.elements)
                val mergedY = (filteredY + yRetry.values).distinct().sorted()
                filteredY = filterAxisValues(mergedY)
                if (yRetry.unit != null && yUnit == null) yUnit = yRetry.unit
                println("OCR[RETRY-Y] found ${yRetry.values.size} new values, merged=$filteredY")
            }

            retryBitmap.recycle()
        }

        // Sort: X ascending, Y descending
        val sortedX = filteredX.sorted()
        val sortedY = filteredY.sortedDescending()

        println("OCR[FINAL] x=$sortedX, y=$sortedY, xUnit=$xUnit, yUnit=$yUnit")

        return AxisOcrResult(
            rawElements = allElements,
            suggestedXValues = sortedX,
            suggestedYValues = sortedY,
            xUnit = xUnit,
            yUnit = yUnit,
            status = OcrStatus.NOT_AVAILABLE,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * Run ML Kit text recognition on a bitmap and return parsed elements.
     */
    private suspend fun scanWithMlKit(bitmap: android.graphics.Bitmap): List<OcrTextElement> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        return suspendCoroutine { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val elements = mutableListOf<OcrTextElement>()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val box = line.boundingBox ?: continue
                            val text = line.text.trim()
                            if (text.isBlank()) continue

                            val numericValue = parseNumeric(text)
                            if (numericValue != null) {
                                elements.add(
                                    OcrTextElement(
                                        text = text,
                                        numericValue = numericValue,
                                        x = box.left.toFloat(),
                                        y = box.top.toFloat(),
                                        width = box.width().toFloat(),
                                        height = box.height().toFloat(),
                                        confidence = 0.8f,
                                    ),
                                )
                            } else {
                                val tokens = text.split("\\s+".toRegex())
                                val parsedTokens = tokens.mapNotNull { token ->
                                    parseNumeric(token)?.let { token to it }
                                }
                                if (parsedTokens.size >= 2) {
                                    val step = if (parsedTokens.size > 1)
                                        box.width().toFloat() / (parsedTokens.size - 1)
                                    else 0f
                                    parsedTokens.forEachIndexed { i, (token, value) ->
                                        elements.add(
                                            OcrTextElement(
                                                text = token,
                                                numericValue = value,
                                                x = box.left.toFloat() + i * step,
                                                y = box.top.toFloat(),
                                                width = step.coerceAtLeast(20f),
                                                height = box.height().toFloat(),
                                                confidence = 0.7f,
                                            ),
                                        )
                                    }
                                } else {
                                    elements.add(
                                        OcrTextElement(
                                            text = text,
                                            numericValue = parsedTokens.firstOrNull()?.second,
                                            x = box.left.toFloat(),
                                            y = box.top.toFloat(),
                                            width = box.width().toFloat(),
                                            height = box.height().toFloat(),
                                            confidence = 0.8f,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    cont.resume(elements)
                }
                .addOnFailureListener {
                    cont.resume(emptyList())
                }
        }
    }

    /**
     * Result from targeted axis strip OCR.
     */
    private data class AxisRetryResult(
        val values: List<Float>,
        val elements: List<OcrTextElement>,
        val unit: String?,
    )

    /**
     * Targeted OCR retry: crop a narrow strip along the specified axis
     * and re-scan with ML Kit. This makes small axis labels much larger
     * relative to the image, dramatically improving recognition.
     *
     * For X-axis: crop strip below graphRegion (where X labels sit)
     * For Y-axis: crop strip left of graphRegion (where Y labels sit)
     */
    private suspend fun retryAxisStrip(
        bitmap: android.graphics.Bitmap,
        graphRegion: GraphRegion,
        axis: String,
    ): AxisRetryResult? {
        // Define crop area
        val cropX: Int
        val cropY: Int
        val cropW: Int
        val cropH: Int

        if (axis == "X") {
            // Strip below graph: from graphBottom to graphBottom + 25% height
            val stripHeight = (graphRegion.height * 0.25f).toInt().coerceAtLeast(50)
            cropX = (graphRegion.x - graphRegion.width * 0.05f).toInt().coerceAtLeast(0)
            cropY = (graphRegion.y + graphRegion.height).coerceIn(0, bitmap.height - 1)
            cropW = (graphRegion.width * 1.1f).toInt().coerceAtMost(bitmap.width - cropX)
            cropH = stripHeight.coerceAtMost(bitmap.height - cropY)
        } else {
            // Strip left of graph: from graphLeft - 25% width to graphLeft
            val stripWidth = (graphRegion.width * 0.15f).toInt().coerceAtLeast(50)
            cropX = (graphRegion.x - stripWidth).coerceAtLeast(0)
            cropY = (graphRegion.y - graphRegion.height * 0.05f).toInt().coerceAtLeast(0)
            cropW = (graphRegion.x - cropX + graphRegion.width * 0.05f).toInt().coerceAtMost(bitmap.width - cropX)
            cropH = (graphRegion.height * 1.1f).toInt().coerceAtMost(bitmap.height - cropY)
        }

        if (cropW < 10 || cropH < 10) return null

        println("OCR[RETRY-$axis] crop: ($cropX,$cropY) ${cropW}x${cropH}")

        val cropped = android.graphics.Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
        val inputImage = InputImage.fromBitmap(cropped, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        return suspendCoroutine { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    cropped.recycle()
                    val values = mutableListOf<Float>()
                    val elems = mutableListOf<OcrTextElement>()
                    var unit: String? = null

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val box = line.boundingBox ?: continue
                            val text = line.text.trim()
                            if (text.isBlank()) continue

                            // Remap coordinates back to full-image space
                            val fullX = box.left.toFloat() + cropX
                            val fullY = box.top.toFloat() + cropY

                            val numericValue = parseNumeric(text)
                            if (numericValue != null) {
                                values.add(numericValue)
                                elems.add(
                                    OcrTextElement(
                                        text = text,
                                        numericValue = numericValue,
                                        x = fullX,
                                        y = fullY,
                                        width = box.width().toFloat(),
                                        height = box.height().toFloat(),
                                        confidence = 0.75f,
                                    ),
                                )
                                println("OCR[RETRY-$axis] found: '$text' = $numericValue at ($fullX,$fullY)")
                            } else {
                                // Try splitting merged numbers
                                val tokens = text.split("\\s+".toRegex())
                                val parsed = tokens.mapNotNull { t -> parseNumeric(t)?.let { t to it } }
                                if (parsed.size >= 2) {
                                    val step = if (parsed.size > 1) box.width().toFloat() / (parsed.size - 1) else 0f
                                    parsed.forEachIndexed { i, (tok, v) ->
                                        values.add(v)
                                        elems.add(
                                            OcrTextElement(
                                                text = tok,
                                                numericValue = v,
                                                x = fullX + i * step,
                                                y = fullY,
                                                width = step.coerceAtLeast(20f),
                                                height = box.height().toFloat(),
                                                confidence = 0.65f,
                                            ),
                                        )
                                        println("OCR[RETRY-$axis] split: '$tok' = $v at (${fullX + i * step},$fullY)")
                                    }
                                } else {
                                    // Check for unit labels
                                    val lower = text.lowercase()
                                    if (axis == "X" && (lower.contains("time") || lower.contains("мин") || lower.contains("min"))) {
                                        unit = text
                                    }
                                    if (axis == "Y" && (lower.contains("abundance") || lower.contains("mau") || lower.contains("intensity"))) {
                                        unit = text
                                    }
                                }
                            }
                        }
                    }

                    cont.resume(AxisRetryResult(values, elems, unit))
                }
                .addOnFailureListener {
                    cropped.recycle()
                    cont.resume(null)
                }
        }
    }

    /**
     * Filter axis values: deduplicate and remove statistical outliers.
     *
     * Axis labels on chromatograms are evenly spaced (e.g., 35, 40, 45, 50...).
     * Outliers like 301002.0 (from title text "0301002.D") are many orders
     * of magnitude away from the real values and can be detected via IQR.
     */
    private fun filterAxisValues(values: List<Float>): List<Float> {
        if (values.size < 2) return values

        // Step 1: Deduplicate (keep unique values)
        val unique = values.distinct().toMutableList()
        if (unique.size < 2) return unique

        // Step 2: IQR-based outlier removal
        // Sort for quartile calculation
        val sorted = unique.sorted()
        val q1 = sorted[sorted.size / 4]
        val q3 = sorted[(sorted.size * 3) / 4]
        val iqr = q3 - q1

        // For very small IQR (all values close), use range-based check
        val range = sorted.last() - sorted.first()
        val threshold = if (iqr > 0) iqr * 3f else range * 0.5f

        val filtered = if (threshold > 0) {
            unique.filter { v ->
                v >= q1 - threshold && v <= q3 + threshold
            }
        } else {
            unique // All same value
        }

        println("OCR[IQR] input=$unique, q1=$q1, q3=$q3, iqr=$iqr, threshold=$threshold, output=$filtered")

        return if (filtered.size >= 2) filtered else unique
    }

    /**
     * Try to parse a numeric value from OCR text.
     * Handles various formats: "35.00", "350", "0.5", "1,5", etc.
     */
    private fun parseNumeric(text: String): Float? {
        // Clean up common OCR artifacts
        val cleaned = text
            .replace(",", ".") // European decimal
            .replace("O", "0") // Common OCR mistake
            .replace("l", "1") // Common OCR mistake
            .replace("I", "1") // Common OCR mistake
            .replace(" ", "")
            .trim()

        return cleaned.toFloatOrNull()
    }

    private fun emptyResult(): AxisOcrResult = AxisOcrResult(
        rawElements = emptyList(),
        suggestedXValues = emptyList(),
        suggestedYValues = emptyList(),
        xUnit = null,
        yUnit = null,
        timestamp = System.currentTimeMillis(),
    )
}
