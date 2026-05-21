package com.chromalab.feature.processing.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.geometry.GeometryAxis
import com.chromalab.feature.processing.geometry.TickOcrCropArtifact
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
                // Primary rule: if element center is LEFT of graph → always Y-axis
                // This prevents Y-labels (50, 100, 150...) from being classified as X
                val isDefinitelyY = cx < graphRegion.x

                // X candidates: below graph area (near bottom axis)
                val isXCandidate = !isDefinitelyY &&
                    cy > graphBottom - classMargin &&
                    cx >= graphRegion.x - classMargin &&
                    cx <= graphRight + classMargin
                // Y candidates: left of graph area (near left axis)
                val isYCandidate = isDefinitelyY || (
                    cx < graphRegion.x + classMargin &&
                    cy >= graphRegion.y - classMargin &&
                    cy <= graphBottom + classMargin)

                if (isXCandidate && !isYCandidate) {
                    xAxisLabels.add(elem)
                } else if (isYCandidate && !isXCandidate) {
                    yAxisLabels.add(elem)
                } else if (isXCandidate && isYCandidate) {
                    // Ambiguous corner — prefer Y if closer to left edge
                    val distToLeft = (cx - graphRegion.x).coerceAtLeast(0f)
                    val distToBottom = (graphBottom - cy).coerceAtLeast(0f)
                    if (distToLeft < distToBottom) {
                        yAxisLabels.add(elem)
                    } else {
                        xAxisLabels.add(elem)
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

        // === PASS 2: Multi-level progressive crop scanning ===
        // Each level uses different margins. Small crops are upscaled for better ML Kit recognition.
        val allElements = elements.toMutableList()
        var allXValues = filteredX.toMutableList()
        var allYValues = filteredY.toMutableList()

        val retryBitmap = BitmapFactory.decodeFile(imagePath)
        if (retryBitmap != null) {
            // Define scan levels: (marginFactor, description)
            // Wider margins → more context. Tighter margins → bigger text relative to frame.
            val scanLevels = listOf(
                0.25f to "wide",     // Level 1: 25% margin — standard
                0.15f to "medium",   // Level 2: 15% margin — tighter
                0.08f to "tight",    // Level 3: 8% margin — very focused
            )

            for ((marginFactor, levelName) in scanLevels) {
                // X-axis scan
                val xResult = scanAxisLevel(retryBitmap, graphRegion, "X", marginFactor, levelName)
                if (xResult != null && xResult.values.isNotEmpty()) {
                    allElements.addAll(xResult.elements)
                    allXValues.addAll(xResult.values)
                    if (xResult.unit != null && xUnit == null) xUnit = xResult.unit
                }

                // Y-axis scan
                val yResult = scanAxisLevel(retryBitmap, graphRegion, "Y", marginFactor, levelName)
                if (yResult != null && yResult.values.isNotEmpty()) {
                    allElements.addAll(yResult.elements)
                    allYValues.addAll(yResult.values)
                    if (yResult.unit != null && yUnit == null) yUnit = yResult.unit
                }
            }

            retryBitmap.recycle()

            // Final dedup + filter on accumulated values
            allXValues = filterAxisValues(allXValues.distinct().sorted()).toMutableList()
            allYValues = filterAxisValues(allYValues.distinct().sorted()).toMutableList()

            println("OCR[MULTI] after ${scanLevels.size} levels: x=$allXValues, y=$allYValues")
        }

        filteredX = allXValues
        filteredY = allYValues

        // === CROSS-AXIS RESCUE ===
        // Detect misclassified values: if a Y value fits the X step pattern, move it to X.
        // Example: X=[45,50,55,60,65], Y=[35,40] → 35,40 belong in X (step=5).
        val rescuedPair = crossAxisRescue(filteredX, filteredY)
        filteredX = rescuedPair.first.toMutableList()
        filteredY = rescuedPair.second.toMutableList()

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
            confidence = estimateOcrConfidence(allElements, sortedX, sortedY),
            timestamp = System.currentTimeMillis(),
        )
    }

    actual suspend fun readTickLabelCrops(crops: List<TickOcrCropArtifact>): AxisOcrResult {
        if (crops.isEmpty()) return emptyResult().copy(warnings = listOf("tick_crop_ocr.no_crops"))
        val elements = mutableListOf<OcrTextElement>()
        val xValues = mutableListOf<Float>()
        val yValues = mutableListOf<Float>()
        val warnings = mutableListOf<String>()

        crops.forEachIndexed { index, crop ->
            val bitmap = BitmapFactory.decodeFile(crop.path)
            if (bitmap == null) {
                warnings.add("tick_crop_ocr.crop_read_failed:$index")
                return@forEachIndexed
            }
            val cropElements = try {
                scanTickCrop(bitmap).map {
                    it.copy(
                        x = it.x + crop.cropRegion.x,
                        y = it.y + crop.cropRegion.y,
                        confidence = (it.confidence * 0.92f).coerceIn(0f, 1f),
                    )
                }
            } finally {
                bitmap.recycle()
            }
            val numericElements = cropElements.filter { it.numericValue != null }
            if (numericElements.isEmpty()) {
                warnings.add("tick_crop_ocr.no_numeric_text:$index")
                return@forEachIndexed
            }
            if (numericElements.size > 1) {
                warnings.add("tick_crop_ocr.multiple_numeric_candidates:$index")
            }
            val selectedElement = numericElements.minByOrNull { element ->
                val elementCenter = when (crop.axis) {
                    GeometryAxis.X -> element.x + element.width / 2f
                    GeometryAxis.Y -> element.y + element.height / 2f
                }
                val axisDistance = abs(elementCenter - crop.tickPixelPosition)
                val crossAxisBias = when (crop.axis) {
                    GeometryAxis.X -> {
                        val preferredY = crop.cropRegion.y + crop.cropRegion.height * 0.72f
                        val centerY = element.y + element.height / 2f
                        val upperBandPenalty = if (centerY < crop.cropRegion.y + crop.cropRegion.height * 0.35f) {
                            crop.cropRegion.height * 2f
                        } else {
                            0f
                        }
                        abs(centerY - preferredY) * 0.25f + upperBandPenalty
                    }
                    GeometryAxis.Y -> {
                        val preferredX = crop.cropRegion.x + crop.cropRegion.width * 0.62f
                        val centerX = element.x + element.width / 2f
                        abs(centerX - preferredX) * 0.12f
                    }
                }
                axisDistance + crossAxisBias
            } ?: return@forEachIndexed
            val sourcedElement = selectedElement.copy(
                sourceAxis = crop.axis.name,
                sourceTickPixelPosition = crop.tickPixelPosition,
                sourceCropPath = crop.path,
            )
            elements.add(sourcedElement)
            when (crop.axis) {
                GeometryAxis.X -> xValues.addNotNull(sourcedElement.numericValue)
                GeometryAxis.Y -> yValues.addNotNull(sourcedElement.numericValue)
            }
        }

        val x = filterAxisValues(xValues.distinct().sorted())
        val y = filterAxisValues(yValues.distinct().sorted()).sortedDescending()
        return AxisOcrResult(
            rawElements = elements,
            suggestedXValues = x,
            suggestedYValues = y,
            xUnit = null,
            yUnit = null,
            status = OcrStatus.NOT_AVAILABLE,
            confidence = estimateOcrConfidence(elements, x, y),
            warnings = warnings.distinct() + "tick_crop_ocr.local_crops_used",
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

    private suspend fun scanTickCrop(bitmap: Bitmap): List<OcrTextElement> {
        val scale = if (maxOf(bitmap.width, bitmap.height) < 180) 2 else 1
        if (scale == 1) return scanWithMlKit(bitmap)
        val scaled = Bitmap.createScaledBitmap(bitmap, bitmap.width * scale, bitmap.height * scale, false)
        return try {
            scanWithMlKit(scaled).map {
                it.copy(
                    x = it.x / scale,
                    y = it.y / scale,
                    width = it.width / scale,
                    height = it.height / scale,
                )
            }
        } finally {
            scaled.recycle()
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
     * Multi-level axis scanning: crop a strip along the axis with configurable margin,
     * then upscale if the crop is too small for reliable OCR.
     *
     * @param marginFactor Controls strip size: 0.25 = 25% of graph dimension,
     *                     0.08 = 8% (tighter, more zoomed on labels)
     * @param levelName For logging
     */
    private suspend fun scanAxisLevel(
        bitmap: android.graphics.Bitmap,
        graphRegion: GraphRegion,
        axis: String,
        marginFactor: Float,
        levelName: String,
    ): AxisRetryResult? {
        val cropX: Int
        val cropY: Int
        val cropW: Int
        val cropH: Int

        if (axis == "X") {
            val stripHeight = (graphRegion.height * marginFactor).toInt().coerceAtLeast(30)
            cropX = (graphRegion.x - graphRegion.width * 0.05f).toInt().coerceAtLeast(0)
            cropY = (graphRegion.y + graphRegion.height).coerceIn(0, bitmap.height - 1)
            cropW = (graphRegion.width * 1.1f).toInt().coerceAtMost(bitmap.width - cropX)
            cropH = stripHeight.coerceAtMost(bitmap.height - cropY)
        } else {
            val stripWidth = (graphRegion.width * marginFactor).toInt().coerceAtLeast(30)
            cropX = (graphRegion.x - stripWidth).coerceAtLeast(0)
            cropY = (graphRegion.y - graphRegion.height * 0.05f).toInt().coerceAtLeast(0)
            cropW = (graphRegion.x - cropX + graphRegion.width * 0.05f).toInt()
                .coerceAtMost(bitmap.width - cropX)
                .coerceAtLeast(1)
            cropH = (graphRegion.height * 1.1f).toInt().coerceAtMost(bitmap.height - cropY)
        }

        if (cropW < 5 || cropH < 5) return null

        var cropped = android.graphics.Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)

        // Upscale small crops — ML Kit needs at least ~200px in the short dimension
        val minDimension = 200
        val shortSide = minOf(cropped.width, cropped.height)
        val scaleFactor = if (shortSide < minDimension) {
            (minDimension.toFloat() / shortSide).coerceAtMost(4f)
        } else 1f

        if (scaleFactor > 1f) {
            val scaledW = (cropped.width * scaleFactor).toInt()
            val scaledH = (cropped.height * scaleFactor).toInt()
            val scaled = android.graphics.Bitmap.createScaledBitmap(cropped, scaledW, scaledH, true)
            cropped.recycle()
            cropped = scaled
            println("OCR[SCAN-$axis/$levelName] crop: ($cropX,$cropY) ${cropW}x${cropH} → upscaled ${scaledW}x${scaledH} (${scaleFactor}x)")
        } else {
            println("OCR[SCAN-$axis/$levelName] crop: ($cropX,$cropY) ${cropW}x${cropH}")
        }

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

                            // Remap coordinates: undo upscale, then add crop offset
                            val fullX = box.left.toFloat() / scaleFactor + cropX
                            val fullY = box.top.toFloat() / scaleFactor + cropY

                            val numericValue = parseNumeric(text)
                            if (numericValue != null) {
                                values.add(numericValue)
                                elems.add(
                                    OcrTextElement(
                                        text = text,
                                        numericValue = numericValue,
                                        x = fullX,
                                        y = fullY,
                                        width = box.width().toFloat() / scaleFactor,
                                        height = box.height().toFloat() / scaleFactor,
                                        confidence = 0.75f,
                                    ),
                                )
                                println("OCR[SCAN-$axis/$levelName] found: '$text' = $numericValue at ($fullX,$fullY)")
                            } else {
                                // Try splitting merged numbers
                                val tokens = text.split("\\s+".toRegex())
                                val parsed = tokens.mapNotNull { t -> parseNumeric(t)?.let { t to it } }
                                if (parsed.size >= 2) {
                                    val step = if (parsed.size > 1) box.width().toFloat() / scaleFactor / (parsed.size - 1) else 0f
                                    parsed.forEachIndexed { i, (tok, v) ->
                                        values.add(v)
                                        elems.add(
                                            OcrTextElement(
                                                text = tok,
                                                numericValue = v,
                                                x = fullX + i * step,
                                                y = fullY,
                                                width = step.coerceAtLeast(20f),
                                                height = box.height().toFloat() / scaleFactor,
                                                confidence = 0.65f,
                                            ),
                                        )
                                        println("OCR[SCAN-$axis/$levelName] split: '$tok' = $v at (${fullX + i * step},$fullY)")
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
     * Filter axis values: deduplicate, remove statistical outliers, and enforce step consistency.
     *
     * Axis labels on chromatograms are evenly spaced (e.g., 35, 40, 45, 50...).
     * Outliers like 301002.0 (from title text "0301002.D") are many orders
     * of magnitude away from the real values and can be detected via IQR.
     * Values that don't fit the dominant step pattern (e.g., "29" in [100,150,200,250,300])
     * are removed by step-based filtering.
     */
    private fun filterAxisValues(values: List<Float>): List<Float> {
        if (values.size < 2) return values

        // Step 1: Deduplicate (keep unique values)
        val unique = values.distinct().toMutableList()
        if (unique.size < 2) return unique

        // Step 2: IQR-based outlier removal
        val sorted = unique.sorted()
        val q1 = sorted[sorted.size / 4]
        val q3 = sorted[(sorted.size * 3) / 4]
        val iqr = q3 - q1

        val range = sorted.last() - sorted.first()
        val threshold = if (iqr > 0) iqr * 3f else range * 0.5f

        val afterIqr = if (threshold > 0) {
            unique.filter { v ->
                v >= q1 - threshold && v <= q3 + threshold
            }
        } else {
            unique
        }

        println("OCR[IQR] input=$unique, q1=$q1, q3=$q3, iqr=$iqr, threshold=$threshold, output=$afterIqr")

        val iqrResult = if (afterIqr.size >= 2) afterIqr else unique

        // Step 3: Step-based filter — detect dominant step and remove outliers
        return filterByStep(iqrResult)
    }

    /**
     * Cross-axis rescue: detect misclassified values and migrate them.
     *
     * Strategy: find dominant step in X, check if any Y values fit that step.
     * Then do the reverse. Move values that improve step consistency.
     *
     * Example: X=[0,45,50,55,60,65], Y=[35,40]
     * → X step=5 (from 45,50,55,60,65). 35 and 40 fit step=5 → move from Y to X.
     * → 0 doesn't fit step=5 from base 35 → move to Y.
     * Result: X=[35,40,45,50,55,60,65], Y=[0]
     */
    private fun crossAxisRescue(
        xValues: List<Float>,
        yValues: List<Float>,
    ): Pair<List<Float>, List<Float>> {
        if (xValues.isEmpty() && yValues.isEmpty()) return xValues to yValues

        val xSorted = xValues.sorted().toMutableList()
        val ySorted = yValues.sorted().toMutableList()

        // Find dominant step for X axis (need at least 2 values)
        val xStep = if (xSorted.size >= 3) {
            val diffs = (1 until xSorted.size).map { xSorted[it] - xSorted[it - 1] }
            findDominantStep(diffs)
        } else if (xSorted.size == 2) {
            xSorted[1] - xSorted[0]
        } else 0f

        // Try to rescue Y values into X
        if (xStep > 0 && ySorted.isNotEmpty()) {
            val tolerance = xStep * 0.15f
            val toMoveToX = mutableListOf<Float>()

            for (yVal in ySorted) {
                // Check if yVal fits as a continuation of the X series
                val fitsX = xSorted.any { xBase ->
                    val diff = abs(yVal - xBase)
                    val nearestMultiple = kotlin.math.round(diff / xStep) * xStep
                    abs(diff - nearestMultiple) < tolerance && diff > tolerance
                }
                if (fitsX) {
                    toMoveToX.add(yVal)
                }
            }

            if (toMoveToX.isNotEmpty()) {
                println("OCR[RESCUE] Moving Y→X: $toMoveToX (xStep=$xStep)")
                xSorted.addAll(toMoveToX)
                ySorted.removeAll(toMoveToX.toSet())
                xSorted.sort()
            }
        }

        // Check if any X values are outliers that belong in Y
        if (xStep > 0 && xSorted.size >= 3) {
            val tolerance = xStep * 0.15f
            val toMoveToY = mutableListOf<Float>()

            for (xVal in xSorted) {
                val fitsAny = xSorted.any { other ->
                    if (other == xVal) return@any false
                    val diff = abs(xVal - other)
                    val nearestMultiple = kotlin.math.round(diff / xStep) * xStep
                    abs(diff - nearestMultiple) < tolerance
                }
                if (!fitsAny) {
                    toMoveToY.add(xVal)
                }
            }

            if (toMoveToY.isNotEmpty()) {
                println("OCR[RESCUE] Moving X→Y: $toMoveToY (doesn't fit xStep=$xStep)")
                ySorted.addAll(toMoveToY)
                xSorted.removeAll(toMoveToY.toSet())
                ySorted.sort()
            }
        }

        // Re-filter after migration
        val finalX = filterAxisValues(xSorted)
        val finalY = filterAxisValues(ySorted)
        return finalX to finalY
    }

    /**
     * Remove values that don't fit the dominant step pattern.
     *
     * Real axis labels are evenly spaced: [100, 150, 200, 250, 300] → step=50.
     * A noise value like "29" doesn't fit any multiple of 50 → remove it.
     */
    private fun filterByStep(values: List<Float>): List<Float> {
        if (values.size < 3) return values

        val sorted = values.sorted()

        // Calculate all adjacent differences
        val diffs = (1 until sorted.size).map { sorted[it] - sorted[it - 1] }

        // Find dominant step: most common difference (with 10% tolerance)
        val dominantStep = findDominantStep(diffs)
        if (dominantStep <= 0f) return values

        // Find the largest group of consecutive values sharing the dominant step.
        // Don't assume sorted[0] is a good base — it could be noise (e.g., "2.0" from "2D").
        val tolerance = dominantStep * 0.15f
        
        // Build compatibility groups: each group is a maximal set of step-compatible values
        val groups = mutableListOf<MutableList<Float>>()
        for (v in sorted) {
            var added = false
            for (group in groups) {
                val fitsGroup = group.any { base ->
                    val diff = kotlin.math.abs(v - base)
                    val nearestMultiple = kotlin.math.round(diff / dominantStep) * dominantStep
                    kotlin.math.abs(diff - nearestMultiple) < tolerance
                }
                if (fitsGroup) {
                    group.add(v)
                    added = true
                    break
                }
            }
            if (!added) {
                groups.add(mutableListOf(v))
            }
        }

        // Pick the largest group — these are the real axis labels
        val bestGroup = groups.maxByOrNull { it.size } ?: return values
        
        // Log rejected values
        val rejected = sorted.filter { it !in bestGroup }
        for (v in rejected) {
            println("OCR[STEP] rejected $v — doesn't fit step=$dominantStep (best group=${bestGroup.first()}..${bestGroup.last()})")
        }

        println("OCR[STEP] input=$sorted, step=$dominantStep, output=$bestGroup")
        return if (bestGroup.size >= 2) bestGroup else values
    }

    /**
     * Find the most common step size among differences.
     * Groups similar diffs (within 10% tolerance) and returns the median of the largest group.
     */
    private fun findDominantStep(diffs: List<Float>): Float {
        if (diffs.isEmpty()) return 0f
        if (diffs.size == 1) return diffs[0]

        val sorted = diffs.sorted()
        // Group by similarity (10% tolerance)
        val groups = mutableListOf(mutableListOf(sorted[0]))
        for (i in 1 until sorted.size) {
            val last = groups.last().last()
            if (sorted[i] - last < last * 0.2f + 1f) {
                groups.last().add(sorted[i])
            } else {
                groups.add(mutableListOf(sorted[i]))
            }
        }
        // Return median of largest group
        val largest = groups.maxByOrNull { it.size } ?: return 0f
        return largest[largest.size / 2]
    }

    /**
     * Try to parse a numeric value from OCR text.
     * Handles various formats: "35.00", "350", "0.5", "1,5", etc.
     */
    private fun parseNumeric(text: String): Float? {
        val raw = text.trim()
        if (raw.none { it.isDigit() }) return null

        // Clean up common OCR artifacts
        val cleaned = raw
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
        confidence = null,
        timestamp = System.currentTimeMillis(),
    )

    private fun estimateOcrConfidence(
        elements: List<OcrTextElement>,
        xValues: List<Float>,
        yValues: List<Float>,
    ): Float? {
        val values = elements.mapNotNull { it.confidence.takeIf { confidence -> confidence > 0f } }
        if (values.isEmpty() || (xValues.isEmpty() && yValues.isEmpty())) return null
        return values.average().toFloat().coerceIn(0f, 1f)
    }
}

private fun MutableList<Float>.addNotNull(value: Float?) {
    if (value != null) add(value)
}
