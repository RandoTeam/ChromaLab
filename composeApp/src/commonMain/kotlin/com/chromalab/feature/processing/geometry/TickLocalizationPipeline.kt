package com.chromalab.feature.processing.geometry

object TickLocalizationPipeline {
    fun evaluate(
        plotAreaBounds: PlotAreaBounds?,
        axisGeometry: AxisGeometry?,
        tickGeometry: TickGeometry?,
        tickOcrResult: TickOcrResult?,
        xCalibrationFit: AxisCalibrationFit?,
        yCalibrationFit: AxisCalibrationFit?,
    ): TickLocalizationResult {
        val xTicks = tickGeometry?.xTicks.orEmpty()
        val yTicks = tickGeometry?.yTicks.orEmpty()
        val items = tickOcrResult?.items.orEmpty()
        val acceptedX = items.filter { it.status == TickOcrItemStatus.ACCEPTED && it.axis == GeometryAxis.X }
        val acceptedY = items.filter { it.status == TickOcrItemStatus.ACCEPTED && it.axis == GeometryAxis.Y }
        val rejectedX = items.filter { it.status != TickOcrItemStatus.ACCEPTED && it.axis == GeometryAxis.X }
        val rejectedY = items.filter { it.status != TickOcrItemStatus.ACCEPTED && it.axis == GeometryAxis.Y }
        val subreasons = buildList {
            if (plotAreaBounds == null) add(TickLocalizationFailureSubreason.PLOT_FRAME_MISSING)
            if (axisGeometry?.xAxisLinePx == null || axisGeometry.yAxisLinePx == null) {
                add(TickLocalizationFailureSubreason.AXIS_LINE_MISSING)
            }
            if (xTicks.size < 2 || yTicks.size < 2) add(TickLocalizationFailureSubreason.TICK_MARKS_MISSING)
            if (tickOcrResult == null || items.none { it.parsedNumericValue != null }) {
                add(TickLocalizationFailureSubreason.OCR_NO_NUMERIC_TEXT)
            }
            if (items.any { it.rejectionReason?.contains("numeric_value_without_deterministic_tick_position") == true }) {
                add(TickLocalizationFailureSubreason.OCR_NUMERIC_NO_TICK_PIXEL)
            }
            if (items.any { it.rejectionReason?.contains("title", ignoreCase = true) == true || it.rawText.contains("m/z", ignoreCase = true) || it.rawText.contains("ion", ignoreCase = true) }) {
                add(TickLocalizationFailureSubreason.TITLE_OR_ION_TEXT_REJECTED)
            }
            if (!acceptedX.isMonotonicByPixelAndValue() || !acceptedY.isMonotonicByPixelAndValue()) {
                add(TickLocalizationFailureSubreason.NON_MONOTONIC_TICK_VALUES)
            }
            if ((xCalibrationFit?.status ?: CalibrationFitStatus.INVALID) == CalibrationFitStatus.INVALID &&
                (xCalibrationFit?.acceptedAnchors?.size ?: acceptedX.size) < 2
            ) {
                add(TickLocalizationFailureSubreason.INSUFFICIENT_X_ANCHORS)
            }
            if ((yCalibrationFit?.status ?: CalibrationFitStatus.INVALID) == CalibrationFitStatus.INVALID &&
                (yCalibrationFit?.acceptedAnchors?.size ?: acceptedY.size) < 2
            ) {
                add(TickLocalizationFailureSubreason.INSUFFICIENT_Y_ANCHORS)
            }
            val warnings = tickGeometry?.warnings.orEmpty() +
                tickOcrResult?.warnings.orEmpty() +
                xCalibrationFit?.warnings.orEmpty() +
                yCalibrationFit?.warnings.orEmpty()
            if (warnings.any { it.contains("no_numeric_text") }) add(TickLocalizationFailureSubreason.OCR_NO_NUMERIC_TEXT)
            if (warnings.any { it.contains("residual", ignoreCase = true) || it.contains("outlier", ignoreCase = true) }) {
                add(TickLocalizationFailureSubreason.HIGH_RESIDUALS)
            }
            if (warnings.any { it.contains("label_band", ignoreCase = true) }) {
                add(TickLocalizationFailureSubreason.LABEL_BAND_MISSING)
            }
        }.distinct()

        val status = when {
            xCalibrationFit?.status == CalibrationFitStatus.VALID && yCalibrationFit?.status == CalibrationFitStatus.VALID ->
                CalibrationFitStatus.VALID
            xCalibrationFit?.status == CalibrationFitStatus.REVIEW || yCalibrationFit?.status == CalibrationFitStatus.REVIEW ->
                CalibrationFitStatus.REVIEW
            else -> CalibrationFitStatus.INVALID
        }

        return TickLocalizationResult(
            status = status,
            subreasons = subreasons,
            xTickCandidateCount = xTicks.size,
            yTickCandidateCount = yTicks.size,
            xAcceptedAnchorCount = acceptedX.size,
            yAcceptedAnchorCount = acceptedY.size,
            xRejectedAnchorCount = rejectedX.size,
            yRejectedAnchorCount = rejectedY.size,
            warnings = (
                tickGeometry?.warnings.orEmpty() +
                    tickOcrResult?.warnings.orEmpty() +
                    xCalibrationFit?.warnings.orEmpty() +
                    yCalibrationFit?.warnings.orEmpty()
                ).distinct(),
        )
    }

    private fun List<TickOcrItem>.isMonotonicByPixelAndValue(): Boolean {
        val positioned = filter { it.tickPixelPosition != null && it.parsedNumericValue != null }
            .sortedBy { it.tickPixelPosition }
        if (positioned.size < 3) return true
        val values = positioned.mapNotNull { it.parsedNumericValue }
        val increasing = values.zipWithNext().all { (a, b) -> b > a }
        val decreasing = values.zipWithNext().all { (a, b) -> b < a }
        return increasing || decreasing
    }
}
