package com.chromalab.feature.processing.ocr

import kotlinx.serialization.Serializable

/**
 * Status of OCR recognition result.
 */
enum class OcrStatus {
    /** OCR result accepted by user as-is */
    ACCEPTED,
    /** OCR result corrected by user */
    CORRECTED,
    /** OCR result ignored, user entered manually */
    IGNORED,
    /** OCR not attempted or failed */
    NOT_AVAILABLE,
}

/**
 * A single OCR-detected text element with position.
 */
@Serializable
data class OcrTextElement(
    val text: String,
    val numericValue: Float?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val confidence: Float,
)

/**
 * OCR result for axis labels.
 * Used as a HINT only — never as ground truth without user confirmation.
 */
@Serializable
data class AxisOcrResult(
    val rawElements: List<OcrTextElement>,
    val suggestedXValues: List<Float>,
    val suggestedYValues: List<Float>,
    val xUnit: String?,
    val yUnit: String?,
    val status: OcrStatus = OcrStatus.NOT_AVAILABLE,
    val confirmedXValues: List<Float>? = null,
    val confirmedYValues: List<Float>? = null,
    val timestamp: Long,
) {
    /** Whether OCR found usable numeric values */
    val hasXSuggestions: Boolean get() = suggestedXValues.size >= 2
    val hasYSuggestions: Boolean get() = suggestedYValues.size >= 2
}
