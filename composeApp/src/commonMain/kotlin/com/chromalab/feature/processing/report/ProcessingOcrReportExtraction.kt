package com.chromalab.feature.processing.report

import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrStatus
import com.chromalab.feature.processing.ocr.OcrTextElement
import com.chromalab.feature.reports.AxisCalibrationCandidate
import com.chromalab.feature.reports.AxisCalibrationCandidatePoint
import com.chromalab.feature.reports.AxisCalibrationCandidateStatus
import com.chromalab.feature.reports.AxisReport
import com.chromalab.feature.reports.ChromatogramIdentification
import com.chromalab.feature.reports.PixelRect
import com.chromalab.feature.reports.ReportAxisCalibration
import com.chromalab.feature.reports.ReportAxisName
import com.chromalab.feature.reports.ReportDoubleValue
import com.chromalab.feature.reports.ReportTextValue
import com.chromalab.feature.reports.ReportValueSource
import com.chromalab.feature.reports.ReportValueStatus
import kotlin.math.abs

internal data class ProcessingOcrReportExtraction(
    val titleOcrConfidence: Double? = null,
    val identification: ChromatogramIdentification? = null,
    val axisCalibration: ReportAxisCalibration? = null,
)

internal fun AxisOcrResult?.toProcessingOcrReportExtraction(
    detectedGraphBounds: PixelRect?,
): ProcessingOcrReportExtraction {
    val result = this ?: return ProcessingOcrReportExtraction()
    val graphElements = result.rawElements
        .filterForGraph(detectedGraphBounds)
        .distinctBy { element ->
            listOf(
                element.text.normalizedTextKey(),
                (element.x / 4f).toInt(),
                (element.y / 4f).toInt(),
            ).joinToString("|")
        }

    val titleElement = graphElements.bestTitleCandidate(detectedGraphBounds)
    val titleText = titleElement?.text?.cleanText()
    val titleConfidence = titleElement?.confidence.toReportConfidence()
    val ionValue = titleText?.extractIonValue()
    val ionRange = titleText?.extractIonRange()
    val sampleLabel = titleText?.extractSampleLabel()
    val sampleName = sampleLabel?.extractSampleName()

    val xLabel = result.xUnit
        ?.normalizeXAxisLabel()
        ?: graphElements.bestAxisLabel(detectedGraphBounds, AxisSide.X)?.text?.normalizeXAxisLabel()
    val yLabel = result.yUnit
        ?.normalizeYAxisLabel()
        ?: graphElements.bestAxisLabel(detectedGraphBounds, AxisSide.Y)?.text?.normalizeYAxisLabel()

    val xSource = result.axisValueSource(result.confirmedXValues)
    val ySource = result.axisValueSource(result.confirmedYValues)
    val xTickValues = result.confirmedXValues.takeUnless { it.isNullOrEmpty() } ?: result.suggestedXValues
    val yTickValues = result.confirmedYValues.takeUnless { it.isNullOrEmpty() } ?: result.suggestedYValues

    val xTickUnit = result.xUnit.normalizeAxisUnit() ?: inferAxisUnitFromLabel(xLabel, AxisSide.X)
    val yTickUnit = result.yUnit.normalizeAxisUnit() ?: inferAxisUnitFromLabel(yLabel, AxisSide.Y)
    val xTicks = xTickValues.toDetectedTicks(xTickUnit, xSource)
    val yTicks = yTickValues.toDetectedTicks(yTickUnit, ySource)
    val calibrationCandidates = listOfNotNull(
        buildAxisCalibrationCandidate(
            axis = AxisSide.X,
            values = xTickValues,
            unit = xTickUnit,
            source = xSource,
            confidence = result.confidence.toReportConfidence(),
            elements = graphElements,
            bounds = detectedGraphBounds,
        ),
        buildAxisCalibrationCandidate(
            axis = AxisSide.Y,
            values = yTickValues,
            unit = yTickUnit,
            source = ySource,
            confidence = result.confidence.toReportConfidence(),
            elements = graphElements,
            bounds = detectedGraphBounds,
        ),
    )
    val xLabelValue = xLabel?.let { detectedText(it, titleConfidence, ReportValueSource.OCR) }
    val yLabelValue = yLabel?.let { detectedText(it, titleConfidence, ReportValueSource.OCR) }
    val xUnitValue = result.xUnit.toAxisUnitValue(xLabel, AxisSide.X, result.confidence)
    val yUnitValue = result.yUnit.toAxisUnitValue(yLabel, AxisSide.Y, result.confidence)

    val identification = buildIdentification(
        titleText = titleText,
        titleConfidence = titleConfidence,
        ionValue = ionValue,
        ionRange = ionRange,
        sampleLabel = sampleLabel,
        sampleName = sampleName,
    )
    val axisCalibration = buildAxisCalibration(
        xLabel = xLabelValue,
        yLabel = yLabelValue,
        xUnit = xUnitValue,
        yUnit = yUnitValue,
        xTicks = xTicks,
        yTicks = yTicks,
        confidence = result.confidence.toReportConfidence(),
        calibrationCandidates = calibrationCandidates,
    )

    return ProcessingOcrReportExtraction(
        titleOcrConfidence = titleConfidence,
        identification = identification,
        axisCalibration = axisCalibration,
    )
}

private enum class AxisSide {
    X,
    Y,
}

private fun buildIdentification(
    titleText: String?,
    titleConfidence: Double?,
    ionValue: String?,
    ionRange: String?,
    sampleLabel: String?,
    sampleName: String?,
): ChromatogramIdentification? {
    if (titleText == null && ionValue == null && ionRange == null && sampleLabel == null && sampleName == null) {
        return null
    }

    return ChromatogramIdentification(
        chromatogramTitle = titleText?.let { detectedText(it, titleConfidence, ReportValueSource.OCR) }
            ?: ReportTextValue.notCalculated(),
        chromatogramMode = if (ionValue != null) {
            detectedText("EIC", titleConfidence, ReportValueSource.OCR)
        } else {
            ReportTextValue.notCalculated()
        },
        ionOrChannel = ionValue?.let { detectedText("m/z $it", titleConfidence, ReportValueSource.OCR) }
            ?: ReportTextValue.notCalculated(),
        ionRange = ionRange?.let { detectedText(it, titleConfidence, ReportValueSource.OCR) }
            ?: ReportTextValue.notCalculated(),
        sampleName = sampleName?.let { detectedText(it, titleConfidence, ReportValueSource.OCR) }
            ?: ReportTextValue.notCalculated(),
        samplePathOrInstrumentLabel = sampleLabel?.let { detectedText(it, titleConfidence, ReportValueSource.OCR) }
            ?: ReportTextValue.notCalculated(),
    )
}

private fun buildAxisCalibration(
    xLabel: ReportTextValue?,
    yLabel: ReportTextValue?,
    xUnit: ReportTextValue?,
    yUnit: ReportTextValue?,
    xTicks: List<ReportDoubleValue>,
    yTicks: List<ReportDoubleValue>,
    confidence: Double?,
    calibrationCandidates: List<AxisCalibrationCandidate>,
): ReportAxisCalibration? {
    val hasX = xLabel != null ||
        xUnit != null ||
        xTicks.isNotEmpty() ||
        calibrationCandidates.any { it.axis == ReportAxisName.X }
    val hasY = yLabel != null ||
        yUnit != null ||
        yTicks.isNotEmpty() ||
        calibrationCandidates.any { it.axis == ReportAxisName.Y }
    if (!hasX && !hasY) return null

    return ReportAxisCalibration(
        xAxis = AxisReport(
            label = xLabel ?: ReportTextValue.notCalculated(),
            unit = xUnit ?: ReportTextValue.notCalculated(),
            visibleMinimum = xTicks.minByOrNull { it.value ?: Double.MAX_VALUE } ?: ReportDoubleValue.notCalculated(),
            visibleMaximum = xTicks.maxByOrNull { it.value ?: -Double.MAX_VALUE } ?: ReportDoubleValue.notCalculated(),
            majorTicks = xTicks.sortedBy { it.value ?: Double.MAX_VALUE },
        ),
        yAxis = AxisReport(
            label = yLabel ?: ReportTextValue.notCalculated(),
            unit = yUnit ?: ReportTextValue.notCalculated(),
            visibleMinimum = yTicks.minByOrNull { it.value ?: Double.MAX_VALUE } ?: ReportDoubleValue.notCalculated(),
            visibleMaximum = yTicks.maxByOrNull { it.value ?: -Double.MAX_VALUE } ?: ReportDoubleValue.notCalculated(),
            majorTicks = yTicks.sortedBy { it.value ?: Double.MAX_VALUE },
        ),
        calibrationConfidence = confidence,
        calibrationCandidates = calibrationCandidates,
    )
}

private fun buildAxisCalibrationCandidate(
    axis: AxisSide,
    values: List<Float>,
    unit: String?,
    source: ReportValueSource,
    confidence: Double?,
    elements: List<OcrTextElement>,
    bounds: PixelRect?,
): AxisCalibrationCandidate? {
    val normalizedValues = values
        .filter { !it.isNaN() && !it.isInfinite() }
        .distinct()
        .sorted()

    if (normalizedValues.isEmpty()) return null

    val candidatePoints = normalizedValues.map { value ->
        val element = elements
            .filter { it.numericValue.matchesAxisValue(value) }
            .maxByOrNull { it.axisNumericScore(bounds, axis) }
        AxisCalibrationCandidatePoint(
            value = value.toDouble(),
            pixel = element?.axisPixel(bounds, axis),
            text = element?.text?.cleanText(),
            confidence = element?.confidence.toReportConfidence(),
        )
    }
    val localizedPixelCount = candidatePoints
        .mapNotNull { it.pixel }
        .distinctBy { (it * 2.0).toInt() }
        .size
    val reasons = buildList {
        if (normalizedValues.size < 2) {
            add("Fewer than two distinct numeric axis readings were available.")
        }
        if (bounds == null) {
            add("Detected graph bounds were unavailable; candidate pixels could not be made graph-relative.")
        }
        if (normalizedValues.size >= 2 && localizedPixelCount < 2) {
            add("Fewer than two OCR readings had localized pixel positions for this axis.")
        }
    }

    return AxisCalibrationCandidate(
        candidateId = when (axis) {
            AxisSide.X -> "ocr-x-axis"
            AxisSide.Y -> "ocr-y-axis"
        },
        axis = axis.toReportAxisName(),
        source = source,
        status = if (reasons.isEmpty()) {
            AxisCalibrationCandidateStatus.CANDIDATE
        } else {
            AxisCalibrationCandidateStatus.INSUFFICIENT_DATA
        },
        unit = unit,
        confidence = confidence,
        points = candidatePoints,
        rejectionReasons = reasons,
    )
}

private fun AxisSide.toReportAxisName(): ReportAxisName =
    when (this) {
        AxisSide.X -> ReportAxisName.X
        AxisSide.Y -> ReportAxisName.Y
    }

private fun OcrTextElement.axisPixel(
    bounds: PixelRect?,
    axis: AxisSide,
): Double? {
    val graphBounds = bounds ?: return null
    return when (axis) {
        AxisSide.X -> (x + width / 2f - graphBounds.x).toDouble()
        AxisSide.Y -> (y + height / 2f - graphBounds.y).toDouble()
    }
}

private fun OcrTextElement.axisNumericScore(
    bounds: PixelRect?,
    axis: AxisSide,
): Int {
    val graphBounds = bounds ?: return 1
    val centerX = x + width / 2f
    val centerY = y + height / 2f
    val graphRight = graphBounds.x + graphBounds.width
    val graphBottom = graphBounds.y + graphBounds.height
    var score = 1

    when (axis) {
        AxisSide.X -> {
            if (centerY >= graphBottom - graphBounds.height * 0.08f) score += 4
            if (centerX >= graphBounds.x - graphBounds.width * 0.10f &&
                centerX <= graphRight + graphBounds.width * 0.10f
            ) {
                score += 3
            }
        }
        AxisSide.Y -> {
            if (centerX <= graphBounds.x + graphBounds.width * 0.12f) score += 4
            if (centerY >= graphBounds.y - graphBounds.height * 0.10f &&
                centerY <= graphBottom + graphBounds.height * 0.10f
            ) {
                score += 3
            }
        }
    }

    return score
}

private fun Float?.matchesAxisValue(value: Float): Boolean {
    val numericValue = this ?: return false
    val tolerance = maxOf(0.01f, abs(value) * 0.001f)
    return abs(numericValue - value) <= tolerance
}

private fun List<OcrTextElement>.filterForGraph(bounds: PixelRect?): List<OcrTextElement> {
    val graphBounds = bounds ?: return this
    val left = graphBounds.x - graphBounds.width * 0.35f
    val right = graphBounds.x + graphBounds.width + graphBounds.width * 0.15f
    val top = graphBounds.y - graphBounds.height * 0.30f
    val bottom = graphBounds.y + graphBounds.height + graphBounds.height * 0.35f

    return filter { element ->
        val centerX = element.x + element.width / 2f
        val centerY = element.y + element.height / 2f
        centerX in left..right && centerY in top..bottom
    }
}

private fun List<OcrTextElement>.bestTitleCandidate(bounds: PixelRect?): OcrTextElement? =
    mapNotNull { element ->
        val score = element.titleScore(bounds)
        if (score > 0) element to score else null
    }
        .maxWithOrNull(
            compareBy<Pair<OcrTextElement, Int>> { it.second }
                .thenBy { it.first.text.length },
        )
        ?.first

private fun OcrTextElement.titleScore(bounds: PixelRect?): Int {
    val text = text.cleanText()
    if (text.length < 6 || text.isPlainNumeric()) return 0
    val lower = text.lowercase()
    var score = 0
    if (lower.contains("ion") || lower.contains("m/z") || lower.contains("mz")) score += 5
    if (lower.contains(".d") || lower.contains(".ms") || lower.contains("data")) score += 4
    if (text.extractIonValue() != null) score += 2

    if (bounds != null) {
        val centerY = y + height / 2f
        val topBand = bounds.y + bounds.height * 0.28f
        if (centerY <= topBand) score += 2
    }

    return score
}

private fun List<OcrTextElement>.bestAxisLabel(
    bounds: PixelRect?,
    axis: AxisSide,
): OcrTextElement? =
    mapNotNull { element ->
        val score = element.axisLabelScore(bounds, axis)
        if (score > 0) element to score else null
    }
        .maxWithOrNull(compareBy<Pair<OcrTextElement, Int>> { it.second }.thenBy { it.first.confidence })
        ?.first

private fun OcrTextElement.axisLabelScore(bounds: PixelRect?, axis: AxisSide): Int {
    val text = text.cleanText()
    if (text.isBlank() || text.isPlainNumeric()) return 0
    val lower = text.lowercase()
    var score = when (axis) {
        AxisSide.X -> when {
            lower.contains("time") || lower.contains("retention") || lower.contains("min") -> 4
            lower.contains("rt") -> 2
            else -> 0
        }
        AxisSide.Y -> when {
            lower.contains("abundance") || lower.contains("intensity") -> 4
            lower.contains("mau") || lower.contains("mv") || lower.contains("a.u") -> 3
            else -> 0
        }
    }
    if (score == 0 || bounds == null) return score

    val centerX = x + width / 2f
    val centerY = y + height / 2f
    val graphBottom = bounds.y + bounds.height
    val graphLeft = bounds.x
    score += when (axis) {
        AxisSide.X -> if (centerY >= graphBottom - bounds.height * 0.10f) 2 else 0
        AxisSide.Y -> if (centerX <= graphLeft + bounds.width * 0.12f) 2 else 0
    }
    return score
}

private fun AxisOcrResult.axisValueSource(confirmedValues: List<Float>?): ReportValueSource =
    if (!confirmedValues.isNullOrEmpty() && (status == OcrStatus.ACCEPTED || status == OcrStatus.CORRECTED)) {
        ReportValueSource.USER
    } else {
        ReportValueSource.OCR
    }

private fun List<Float>.toDetectedTicks(
    unit: String?,
    source: ReportValueSource,
): List<ReportDoubleValue> =
    filter { !it.isNaN() && !it.isInfinite() }
        .distinct()
        .map { value ->
            ReportDoubleValue(
                value = value.toDouble(),
                unit = unit,
                status = ReportValueStatus.DETECTED,
                confidence = null,
                source = source,
            )
        }

private fun String?.toAxisUnitValue(
    label: String?,
    axis: AxisSide,
    confidence: Float?,
): ReportTextValue? {
    val detectedUnit = normalizeAxisUnit()
    if (detectedUnit != null) {
        return detectedText(detectedUnit, confidence.toReportConfidence(), ReportValueSource.OCR)
    }
    return inferAxisUnitFromLabel(label, axis)?.let {
        inferredText(it, 0.50, ReportValueSource.LOCAL_KNOWLEDGE)
    }
}

private fun String?.normalizeAxisUnit(): String? {
    val lower = this?.lowercase().orEmpty()
    return when {
        lower.contains("min") || lower.contains("мин") -> "min"
        lower.contains("sec") || lower.contains("сек") -> "sec"
        lower.contains("mau") -> "mAU"
        lower.contains("mv") -> "mV"
        lower.contains("a.u") || lower == "au" -> "a.u."
        lower.contains("count") -> "counts"
        else -> null
    }
}

private fun inferAxisUnitFromLabel(
    label: String?,
    axis: AxisSide,
): String? =
    when (axis) {
        AxisSide.X -> if (label?.lowercase()?.contains("time") == true) "min" else null
        AxisSide.Y -> if (label?.lowercase()?.contains("abundance") == true) "counts" else null
    }

private fun String.normalizeXAxisLabel(): String? {
    val lower = lowercase()
    return when {
        lower.contains("retention") -> "Retention time"
        lower.contains("time") -> "Time"
        lower.contains("rt") -> "RT"
        else -> cleanAxisLabel().takeIf { it.isNotBlank() }
    }
}

private fun String.normalizeYAxisLabel(): String? {
    val lower = lowercase()
    return when {
        lower.contains("abundance") -> "Abundance"
        lower.contains("intensity") -> "Intensity"
        else -> cleanAxisLabel().takeIf { it.isNotBlank() }
    }
}

private fun String.cleanAxisLabel(): String =
    cleanText()
        .replace(Regex("""[-=]+>"""), "")
        .replace(":", "")
        .trim()

private fun String.extractIonValue(): String? {
    val match = ionRegex.find(this) ?: return null
    return match.groupValues.getOrNull(1)?.normalizeDecimalText()
}

private fun String.extractIonRange(): String? {
    val match = ionRangeRegex.find(this) ?: return null
    val start = match.groupValues.getOrNull(1)?.normalizeDecimalText() ?: return null
    val end = match.groupValues.getOrNull(2)?.normalizeDecimalText() ?: return null
    return "$start to $end"
}

private fun String.extractSampleLabel(): String? {
    val pathMatch = samplePathRegex.find(this)?.value?.cleanText()
    val afterRange = indexOf("):").takeIf { it >= 0 }?.let { substring(it + 2).cleanText() }
    val afterColon = substringAfter(':', "").cleanText()
    return listOfNotNull(afterRange, afterColon, pathMatch)
        .firstOrNull { it.isNotBlank() && !it.isPlainNumeric() }
        ?.trim(' ', ';', ',')
}

private fun String.extractSampleName(): String? {
    val firstPathSegment = replace('\\', '/')
        .substringBefore('/')
        .trim()
    val withoutDataSuffix = firstPathSegment
        .removeSuffixIgnoreCase(".D")
        .removeSuffixIgnoreCase(".MS")
    return withoutDataSuffix.takeIf { it.isNotBlank() && !it.isPlainNumeric() }
}

private fun String.removeSuffixIgnoreCase(suffix: String): String =
    if (endsWith(suffix, ignoreCase = true)) {
        dropLast(suffix.length)
    } else {
        this
    }

private fun String.normalizeDecimalText(): String =
    trim()
        .replace(',', '.')
        .trim(' ', '(', ')', '[', ']', ':', ';')

private fun String.cleanText(): String =
    replace('\n', ' ')
        .replace('\r', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()

private fun String.normalizedTextKey(): String =
    cleanText().lowercase()

private fun String.isPlainNumeric(): Boolean =
    replace(",", ".").trim().toDoubleOrNull() != null

private fun detectedText(
    value: String,
    confidence: Double?,
    source: ReportValueSource,
): ReportTextValue =
    ReportTextValue(
        value = value,
        status = ReportValueStatus.DETECTED,
        confidence = confidence,
        source = source,
    )

private fun inferredText(
    value: String,
    confidence: Double?,
    source: ReportValueSource,
): ReportTextValue =
    ReportTextValue(
        value = value,
        status = ReportValueStatus.INFERRED,
        confidence = confidence,
        source = source,
    )

private fun Float?.toReportConfidence(): Double? {
    val value = this ?: return null
    if (value.isNaN() || value.isInfinite() || value < 0f) return null
    val normalized = if (value > 1f) value / 100f else value
    return normalized.toDouble().coerceIn(0.0, 1.0)
}

private val ionRegex = Regex(
    pattern = """(?:ion|m/z|mz)\s*[:=]?\s*([0-9]+(?:[.,][0-9]+)?)""",
    option = RegexOption.IGNORE_CASE,
)

private val ionRangeRegex = Regex(
    pattern = """([0-9]+(?:[.,][0-9]+)?)\s*(?:to|-|\u2013)\s*([0-9]+(?:[.,][0-9]+)?)""",
    option = RegexOption.IGNORE_CASE,
)

private val samplePathRegex = Regex(
    pattern = """[A-Za-z0-9 _.-]+\.D[\\/][A-Za-z0-9 _.-]+\.ms""",
    option = RegexOption.IGNORE_CASE,
)
