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
import com.chromalab.feature.reports.PixelToUnitTransform
import com.chromalab.feature.reports.ReportAxisCalibration
import com.chromalab.feature.reports.ReportAxisName
import com.chromalab.feature.reports.ReportDoubleValue
import com.chromalab.feature.reports.ReportSeverity
import com.chromalab.feature.reports.ReportTextValue
import com.chromalab.feature.reports.ReportValueSource
import com.chromalab.feature.reports.ReportValueStatus
import com.chromalab.feature.reports.ReportWarning
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
    val axisAlignmentWarnings = detectAxisAlignmentWarnings(
        xValues = xTickValues,
        yValues = yTickValues,
        elements = graphElements,
        bounds = detectedGraphBounds,
    )
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
        bounds = detectedGraphBounds,
        axisAlignmentWarnings = axisAlignmentWarnings,
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
    bounds: PixelRect?,
    axisAlignmentWarnings: List<ReportWarning>,
): ReportAxisCalibration? {
    val transformSelection = buildPixelToUnitTransform(calibrationCandidates, bounds, confidence)
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
        calibrationConfidence = transformSelection?.confidence,
        calibrationCandidates = calibrationCandidates,
        pixelToUnitTransform = transformSelection?.transform,
        warnings = buildAxisCalibrationWarnings(
            xTicks = xTicks,
            yTicks = yTicks,
            calibrationCandidates = calibrationCandidates,
            transformSelection = transformSelection,
            axisOcrConfidence = confidence,
            axisAlignmentWarnings = axisAlignmentWarnings,
        ),
    )
}

private fun buildAxisCalibrationWarnings(
    xTicks: List<ReportDoubleValue>,
    yTicks: List<ReportDoubleValue>,
    calibrationCandidates: List<AxisCalibrationCandidate>,
    transformSelection: AxisTransformSelection?,
    axisOcrConfidence: Double?,
    axisAlignmentWarnings: List<ReportWarning>,
): List<ReportWarning> =
    buildList {
        if (axisOcrConfidence == null) {
            add(axisWarning("axis.ocr_confidence_missing", "Axis OCR confidence is missing."))
        } else if (axisOcrConfidence < WEAK_AXIS_OCR_CONFIDENCE_THRESHOLD) {
            add(
                axisWarning(
                    code = "axis.ocr_confidence_weak",
                    message = "Axis OCR confidence is weak (${axisOcrConfidence.renderPercentForWarning()}).",
                ),
            )
        }

        addAll(buildSingleAxisWarnings(ReportAxisName.X, xTicks, calibrationCandidates))
        addAll(buildSingleAxisWarnings(ReportAxisName.Y, yTicks, calibrationCandidates))
        addAll(axisAlignmentWarnings)

        if (transformSelection == null) {
            add(
                axisWarning(
                    code = "axis.transform_missing",
                    message = "No validated X/Y pixel-to-unit transform is available; release-quality calibrated calculations must not be claimed.",
                    severity = ReportSeverity.SERIOUS,
                ),
            )
        }
    }.distinctBy { warning ->
        listOf(warning.code, warning.message).joinToString("|")
    }

private fun buildSingleAxisWarnings(
    axis: ReportAxisName,
    ticks: List<ReportDoubleValue>,
    calibrationCandidates: List<AxisCalibrationCandidate>,
): List<ReportWarning> =
    buildList {
        val candidate = calibrationCandidates.firstOrNull { it.axis == axis }
        if (ticks.size < MIN_AXIS_TICK_COUNT) {
            add(
                axisWarning(
                    code = "axis.${axis.codeName}.ticks_missing",
                    message = "${axis.displayName} has fewer than two detected tick labels.",
                    severity = ReportSeverity.SERIOUS,
                ),
            )
        }
        if (candidate == null) {
            add(
                axisWarning(
                    code = "axis.${axis.codeName}.candidate_missing",
                    message = "No ${axis.displayName.lowercase()} calibration candidate was produced.",
                    severity = ReportSeverity.SERIOUS,
                ),
            )
            return@buildList
        }

        val localizedTickCount = candidate.points.count { it.pixel != null }
        if (localizedTickCount < MIN_AXIS_TICK_COUNT) {
            add(
                axisWarning(
                    code = "axis.${axis.codeName}.localized_ticks_missing",
                    message = "${axis.displayName} has fewer than two localized tick pixels.",
                    severity = ReportSeverity.SERIOUS,
                ),
            )
        }

        when (candidate.status) {
            AxisCalibrationCandidateStatus.INSUFFICIENT_DATA -> add(
                axisWarning(
                    code = "axis.${axis.codeName}.calibration_insufficient",
                    message = "${axis.displayName} calibration candidate has insufficient data: ${candidate.rejectionReasons.joinToWarningMessage()}",
                    severity = ReportSeverity.SERIOUS,
                ),
            )
            AxisCalibrationCandidateStatus.REJECTED -> add(
                axisWarning(
                    code = "axis.${axis.codeName}.geometry_inconsistent",
                    message = "${axis.displayName} calibration geometry is inconsistent: ${candidate.rejectionReasons.joinToWarningMessage()}",
                    severity = ReportSeverity.SERIOUS,
                ),
            )
            AxisCalibrationCandidateStatus.CANDIDATE -> add(
                axisWarning(
                    code = "axis.${axis.codeName}.calibration_unvalidated",
                    message = "${axis.displayName} calibration candidate was not validated.",
                    severity = ReportSeverity.SERIOUS,
                ),
            )
            AxisCalibrationCandidateStatus.VALIDATED -> Unit
        }

        val tickConfidence = candidate.points
            .mapNotNull { it.confidence }
            .takeIf { it.isNotEmpty() }
            ?.average()
        if (tickConfidence != null && tickConfidence < WEAK_TICK_OCR_CONFIDENCE_THRESHOLD) {
            add(
                axisWarning(
                    code = "axis.${axis.codeName}.tick_ocr_confidence_weak",
                    message = "${axis.displayName} tick OCR confidence is weak (${tickConfidence.renderPercentForWarning()}).",
                ),
            )
        }
    }

private fun buildPixelToUnitTransform(
    candidates: List<AxisCalibrationCandidate>,
    bounds: PixelRect?,
    fallbackConfidence: Double?,
): AxisTransformSelection? {
    val graphBounds = bounds ?: return null
    val xFit = candidates.selectedAxisFit(ReportAxisName.X, graphBounds.width.toDouble()) ?: return null
    val yFit = candidates.selectedAxisFit(ReportAxisName.Y, graphBounds.height.toDouble()) ?: return null

    return AxisTransformSelection(
        transform = PixelToUnitTransform(
            xScale = xFit.scale,
            xOffset = xFit.offset,
            yScale = yFit.scale,
            yOffset = yFit.offset,
            method = "ocr-validated-linear-axis-fit",
        ),
        confidence = listOf(
            xFit.confidence(fallbackConfidence),
            yFit.confidence(fallbackConfidence),
        ).average().coerceIn(0.0, 1.0),
    )
}

private fun List<AxisCalibrationCandidate>.selectedAxisFit(
    axis: ReportAxisName,
    axisLength: Double,
): AxisLinearFit? =
    firstOrNull { candidate ->
        candidate.axis == axis && candidate.status == AxisCalibrationCandidateStatus.VALIDATED
    }
        ?.toAxisLinearFit(axisLength)

private fun AxisCalibrationCandidate.toAxisLinearFit(axisLength: Double): AxisLinearFit? {
    val localizedPoints = points
        .mapNotNull { point -> point.pixel?.let { pixel -> LocalizedAxisPoint(point.value, pixel) } }
        .distinctBy { point -> (point.pixel * 2.0).toInt() }
        .sortedBy { it.pixel }
    if (localizedPoints.size < 2) return null

    val meanPixel = localizedPoints.map { it.pixel }.average()
    val meanValue = localizedPoints.map { it.value }.average()
    val pixelVariance = localizedPoints.sumOf { point ->
        val delta = point.pixel - meanPixel
        delta * delta
    }
    if (pixelVariance <= 0.0) return null

    val covariance = localizedPoints.sumOf { point ->
        (point.pixel - meanPixel) * (point.value - meanValue)
    }
    val scale = covariance / pixelVariance
    val offset = meanValue - scale * meanPixel
    val fittedValues = localizedPoints.map { point -> scale * point.pixel + offset }
    val residualSum = localizedPoints.zip(fittedValues).sumOf { (point, fittedValue) ->
        val delta = point.value - fittedValue
        delta * delta
    }
    val totalSum = localizedPoints.sumOf { point ->
        val delta = point.value - meanValue
        delta * delta
    }
    val rSquared = if (totalSum <= 0.0) {
        1.0
    } else {
        (1.0 - residualSum / totalSum).coerceIn(0.0, 1.0)
    }
    val pixelSpan = localizedPoints.maxOf { it.pixel } - localizedPoints.minOf { it.pixel }
    val pointConfidence = localizedPoints
        .mapNotNull { localized ->
            points.firstOrNull { point -> point.value == localized.value && point.pixel == localized.pixel }?.confidence
        }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?: confidence

    return AxisLinearFit(
        scale = scale,
        offset = offset,
        rSquared = rSquared,
        pixelCoverage = if (axisLength > 0.0) pixelSpan / axisLength else 0.0,
        pointConfidence = pointConfidence,
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
    val structuralReasons = buildList {
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
    val geometryReasons = if (structuralReasons.isEmpty()) {
        validateAxisCandidateGeometry(axis, candidatePoints, bounds)
    } else {
        emptyList()
    }
    val reasons = structuralReasons + geometryReasons

    return AxisCalibrationCandidate(
        candidateId = when (axis) {
            AxisSide.X -> "ocr-x-axis"
            AxisSide.Y -> "ocr-y-axis"
        },
        axis = axis.toReportAxisName(),
        source = source,
        status = when {
            structuralReasons.isNotEmpty() -> {
                AxisCalibrationCandidateStatus.INSUFFICIENT_DATA
            }
            geometryReasons.isNotEmpty() -> {
                AxisCalibrationCandidateStatus.REJECTED
            }
            else -> {
                AxisCalibrationCandidateStatus.VALIDATED
            }
        },
        unit = unit,
        confidence = confidence,
        points = candidatePoints,
        rejectionReasons = reasons,
    )
}

private fun validateAxisCandidateGeometry(
    axis: AxisSide,
    points: List<AxisCalibrationCandidatePoint>,
    bounds: PixelRect?,
): List<String> {
    val graphBounds = bounds ?: return emptyList()
    val localized = points
        .mapNotNull { point -> point.pixel?.let { pixel -> LocalizedAxisPoint(point.value, pixel) } }
        .sortedBy { it.pixel }
    if (localized.size < 2) return emptyList()

    val axisLimit = when (axis) {
        AxisSide.X -> graphBounds.width.toDouble()
        AxisSide.Y -> graphBounds.height.toDouble()
    }
    val tolerance = axisLimit * 0.05

    return buildList {
        if (localized.any { point -> point.pixel < -tolerance || point.pixel > axisLimit + tolerance }) {
            add("At least one localized tick pixel is outside the visible graph range.")
        }

        val values = localized.map { it.value }
        val minValue = values.minOrNull()
        val maxValue = values.maxOrNull()
        if (minValue == null || maxValue == null || maxValue <= minValue) {
            add("Axis readings do not define a positive visible value range.")
        }

        val valueDeltasByPixel = localized.zipWithNext { left, right -> right.value - left.value }
        val isMonotonic = when (axis) {
            AxisSide.X -> valueDeltasByPixel.all { it > 0.0 }
            AxisSide.Y -> valueDeltasByPixel.all { it < 0.0 }
        }
        if (!isMonotonic) {
            add(
                when (axis) {
                    AxisSide.X -> "X-axis readings are not monotonic increasing from left to right."
                    AxisSide.Y -> "Y-axis readings are not monotonic decreasing from top to bottom."
                },
            )
        }

        val slopes = localized.zipWithNext { left, right ->
            val pixelDelta = right.pixel - left.pixel
            if (pixelDelta <= 0.0) {
                null
            } else {
                abs(right.value - left.value) / pixelDelta
            }
        }.filterNotNull()

        if (slopes.size >= 2) {
            val medianSlope = slopes.sorted()[slopes.size / 2]
            if (medianSlope <= 0.0) {
                add("Axis tick spacing cannot be evaluated because the median slope is zero.")
            } else {
                val maxRelativeDeviation = slopes.maxOf { slope -> abs(slope - medianSlope) / medianSlope }
                if (maxRelativeDeviation > 0.40) {
                    add("Axis tick spacing is inconsistent with a linear calibration candidate.")
                }
            }
        }
    }
}

private fun detectAxisAlignmentWarnings(
    xValues: List<Float>,
    yValues: List<Float>,
    elements: List<OcrTextElement>,
    bounds: PixelRect?,
): List<ReportWarning> {
    val graphBounds = bounds ?: return emptyList()
    return listOfNotNull(
        detectSingleAxisAlignmentWarning(AxisSide.X, xValues, elements, graphBounds),
        detectSingleAxisAlignmentWarning(AxisSide.Y, yValues, elements, graphBounds),
    )
}

private fun detectSingleAxisAlignmentWarning(
    axis: AxisSide,
    values: List<Float>,
    elements: List<OcrTextElement>,
    bounds: PixelRect,
): ReportWarning? {
    val matchedElements = values
        .filter { !it.isNaN() && !it.isInfinite() }
        .distinct()
        .mapNotNull { value ->
            elements
                .filter { it.numericValue.matchesAxisValue(value) }
                .maxByOrNull { it.axisNumericScore(bounds, axis) }
        }
        .distinctBy { element ->
            listOf(
                element.text.normalizedTextKey(),
                (element.x / 4f).toInt(),
                (element.y / 4f).toInt(),
            ).joinToString("|")
        }
    if (matchedElements.size < MIN_TILT_ALIGNMENT_TICK_COUNT) return null

    val orthogonalPositions = matchedElements.map { element ->
        when (axis) {
            AxisSide.X -> element.y + element.height / 2f
            AxisSide.Y -> element.x + element.width / 2f
        }.toDouble()
    }
    val spread = (orthogonalPositions.maxOrNull() ?: return null) -
        (orthogonalPositions.minOrNull() ?: return null)
    val threshold = when (axis) {
        AxisSide.X -> maxOf(MIN_TILT_ALIGNMENT_SPREAD_PX, bounds.height * TILT_ALIGNMENT_SPREAD_FRACTION)
        AxisSide.Y -> maxOf(MIN_TILT_ALIGNMENT_SPREAD_PX, bounds.width * TILT_ALIGNMENT_SPREAD_FRACTION)
    }
    if (spread <= threshold) return null

    return axisWarning(
        code = "axis.${axis.toReportAxisName().codeName}.alignment_tilt_suspected",
        message = "${axis.toReportAxisName().displayName} tick labels are not aligned; image tilt or perspective correction may be incomplete.",
    )
}

private data class LocalizedAxisPoint(
    val value: Double,
    val pixel: Double,
)

private data class AxisTransformSelection(
    val transform: PixelToUnitTransform,
    val confidence: Double,
)

private data class AxisLinearFit(
    val scale: Double,
    val offset: Double,
    val rSquared: Double,
    val pixelCoverage: Double,
    val pointConfidence: Double?,
) {
    fun confidence(fallbackConfidence: Double?): Double {
        val sourceConfidence = (pointConfidence ?: fallbackConfidence ?: 0.50).coerceIn(0.0, 1.0)
        val coverageConfidence = pixelCoverage.coerceIn(0.0, 1.0)
        return (
            rSquared.coerceIn(0.0, 1.0) * 0.55 +
                coverageConfidence * 0.30 +
                sourceConfidence * 0.15
            ).coerceIn(0.0, 1.0)
    }
}

private fun AxisSide.toReportAxisName(): ReportAxisName =
    when (this) {
        AxisSide.X -> ReportAxisName.X
        AxisSide.Y -> ReportAxisName.Y
    }

private val ReportAxisName.codeName: String
    get() = name.lowercase()

private val ReportAxisName.displayName: String
    get() = when (this) {
        ReportAxisName.X -> "X-axis"
        ReportAxisName.Y -> "Y-axis"
    }

private fun axisWarning(
    code: String,
    message: String,
    severity: ReportSeverity = ReportSeverity.WARNING,
): ReportWarning =
    ReportWarning(
        code = code,
        message = message,
        severity = severity,
        stage = "axis_calibration",
    )

private fun List<String>.joinToWarningMessage(): String =
    takeIf { it.isNotEmpty() }?.joinToString("; ") ?: "no detailed reason recorded."

private fun Double.renderPercentForWarning(): String =
    "${(coerceIn(0.0, 1.0) * 100.0).formatWarningNumber()}%"

private fun Double.formatWarningNumber(): String =
    if (abs(this) >= 10.0) "%.1f".format(this) else "%.2f".format(this)

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

private const val MIN_AXIS_TICK_COUNT = 2
private const val MIN_TILT_ALIGNMENT_TICK_COUNT = 3
private const val MIN_TILT_ALIGNMENT_SPREAD_PX = 8.0
private const val TILT_ALIGNMENT_SPREAD_FRACTION = 0.04
private const val WEAK_AXIS_OCR_CONFIDENCE_THRESHOLD = 0.70
private const val WEAK_TICK_OCR_CONFIDENCE_THRESHOLD = 0.70
