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
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: return emptyResult()

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

                            elements.add(
                                OcrTextElement(
                                    text = text,
                                    numericValue = numericValue,
                                    x = box.left.toFloat(),
                                    y = box.top.toFloat(),
                                    width = box.width().toFloat(),
                                    height = box.height().toFloat(),
                                    confidence = 0.8f, // ML Kit doesn't expose per-line confidence
                                ),
                            )
                        }
                    }

                    bitmap.recycle()

                    // Classify elements by position relative to graph region
                    val xAxisLabels = mutableListOf<Float>()
                    val yAxisLabels = mutableListOf<Float>()
                    var xUnit: String? = null
                    var yUnit: String? = null

                    val margin = graphRegion.height * 0.15f

                    for (elem in elements) {
                        if (elem.numericValue != null) {
                            // Below or on the X axis → X labels
                            if (elem.y > graphRegion.bottom - margin &&
                                elem.x >= graphRegion.x - margin &&
                                elem.x <= graphRegion.right + margin
                            ) {
                                xAxisLabels.add(elem.numericValue)
                            }

                            // Left of or on the Y axis → Y labels
                            if (elem.x < graphRegion.x + margin &&
                                elem.y >= graphRegion.y - margin &&
                                elem.y <= graphRegion.bottom + margin
                            ) {
                                yAxisLabels.add(elem.numericValue)
                            }
                        } else {
                            // Non-numeric — could be unit labels
                            val lower = elem.text.lowercase()
                            if (elem.y > graphRegion.bottom) {
                                if (lower.contains("мин") || lower.contains("min") ||
                                    lower.contains("сек") || lower.contains("sec") ||
                                    lower.contains("time")
                                ) {
                                    xUnit = elem.text
                                }
                            }
                            if (elem.x < graphRegion.x) {
                                if (lower.contains("mau") || lower.contains("mv") ||
                                    lower.contains("µv") || lower.contains("au") ||
                                    lower.contains("intensity")
                                ) {
                                    yUnit = elem.text
                                }
                            }
                        }
                    }

                    // Sort: X left→right, Y bottom→top (high value first)
                    xAxisLabels.sort()
                    yAxisLabels.sortDescending()

                    val status = if (xAxisLabels.size >= 2 || yAxisLabels.size >= 2) {
                        OcrStatus.NOT_AVAILABLE // Will be set to ACCEPTED/CORRECTED/IGNORED by user
                    } else {
                        OcrStatus.NOT_AVAILABLE
                    }

                    cont.resume(
                        AxisOcrResult(
                            rawElements = elements,
                            suggestedXValues = xAxisLabels,
                            suggestedYValues = yAxisLabels,
                            xUnit = xUnit,
                            yUnit = yUnit,
                            status = status,
                            timestamp = System.currentTimeMillis(),
                        ),
                    )
                }
                .addOnFailureListener {
                    bitmap.recycle()
                    cont.resume(emptyResult())
                }
        }
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
