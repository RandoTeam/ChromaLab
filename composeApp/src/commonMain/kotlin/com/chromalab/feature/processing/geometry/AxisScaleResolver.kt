package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.calibration.AxisCalibrationFitter
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrElementSourceKind
import com.chromalab.feature.processing.ocr.OcrTextElement
import kotlin.math.abs

class AxisScaleResolver(
    private val calibrationFitter: AxisCalibrationFitter = AxisCalibrationFitter(),
) {
    fun resolve(
        panelRegion: GraphRegion,
        plotRegion: GraphRegion?,
        axisGeometry: AxisGeometry?,
        tickGeometry: TickGeometry?,
        tickOcrResult: TickOcrResult?,
        axisLabelOcrResult: AxisOcrResult?,
    ): AxisScaleResolutionResult {
        val plot = plotRegion ?: return invalidResult(
            subreasons = listOf(AxisScaleFailureSubreason.AXIS_FRAME_INCONSISTENT),
            warning = "axis_scale.plot_frame_missing",
        )

        val rejected = mutableListOf<AxisScaleAnchor>()
        val xAnchors = mutableListOf<AxisScaleAnchor>()
        val yAnchors = mutableListOf<AxisScaleAnchor>()

        tickOcrResult?.acceptedItems.orEmpty().forEach { item ->
            val value = item.parsedNumericValue ?: return@forEach
            val pixel = item.tickPixelPosition ?: return@forEach
            val semanticReason = item.rawText.scaleRejectionReason()
            if (semanticReason != null) {
                rejected += AxisScaleAnchor(
                    axis = item.axis,
                    pixelCoordinate = pixel,
                    numericValue = value,
                    evidenceType = AxisScaleEvidenceType.SEMANTIC_TEXT_REJECTED,
                    confidence = item.confidence,
                    rawText = item.rawText,
                    cropPath = item.localCropPath,
                    rejectionReason = semanticReason,
                )
                return@forEach
            }
            val relativePixel = when (item.axis) {
                GeometryAxis.X -> pixel - plot.x
                GeometryAxis.Y -> pixel - plot.y
            }.coerceAtLeast(0f)
            val anchor = AxisScaleAnchor(
                axis = item.axis,
                pixelCoordinate = relativePixel,
                numericValue = value,
                evidenceType = AxisScaleEvidenceType.EXPLICIT_TICK_MARK,
                confidence = item.confidence,
                rawText = item.rawText,
                cropPath = item.localCropPath,
                projectionSource = "deterministic_tick",
            )
            when (item.axis) {
                GeometryAxis.X -> xAnchors += anchor
                GeometryAxis.Y -> yAnchors += anchor
            }
        }

        val labelOcr = axisLabelOcrResult
        labelOcr?.rawElements.orEmpty().forEach { element ->
            val value = element.numericValue?.toDouble()
            if (value == null) {
                return@forEach
            }
            val semanticReason = element.semanticScaleRejectionReason()
            if (semanticReason != null) {
                rejected += element.rejectedAnchor(GeometryAxis.X, semanticReason, AxisScaleEvidenceType.SEMANTIC_TEXT_REJECTED)
                return@forEach
            }
            if (element.sourceKind == OcrElementSourceKind.VLM_AXIS_EXTRACTION) {
                rejected += element.rejectedAnchor(
                    axis = element.preferredAxis(labelOcr, plot, panelRegion) ?: GeometryAxis.X,
                    reason = "axis_scale.vlm_position_rejected_for_geometry",
                    evidenceType = AxisScaleEvidenceType.SEMANTIC_TEXT_REJECTED,
                )
                return@forEach
            }

            val axis = element.preferredAxis(labelOcr, plot, panelRegion)
            if (axis == null) {
                rejected += element.rejectedAnchor(GeometryAxis.X, "axis_scale.numeric_label_box_outside_axis_bands", AxisScaleEvidenceType.OCR_VALUE_ONLY_REJECTED)
                return@forEach
            }

            val anchor = element.toLabelAnchor(
                axis = axis,
                value = value,
                plot = plot,
                tickGeometry = tickGeometry,
            )
            when (axis) {
                GeometryAxis.X -> xAnchors += anchor
                GeometryAxis.Y -> yAnchors += anchor
            }
        }

        val xDistinct = xAnchors.bestPerValue()
        val yDistinct = yAnchors.bestPerValue()
        val rawXFit = calibrationFitter.fit(
            axis = GeometryAxis.X,
            anchors = xDistinct.map { it.toCalibrationAnchor() },
            axisLengthPx = plot.width.toFloat(),
            geometryCleanliness = axisGeometry?.axisConfidence ?: 0f,
        )
        val rawYFit = calibrationFitter.fit(
            axis = GeometryAxis.Y,
            anchors = yDistinct.map { it.toCalibrationAnchor() },
            axisLengthPx = plot.height.toFloat(),
            geometryCleanliness = axisGeometry?.axisConfidence ?: 0f,
        )
        val xFit = rawXFit.downgradeLabelOnlyFitIfNeeded(xDistinct)
        val yFit = rawYFit.downgradeLabelOnlyFitIfNeeded(yDistinct)

        val subreasons = buildList {
            val numericLabels = axisLabelOcrResult?.rawElements.orEmpty().count { it.numericValue != null }
            if (numericLabels == 0) add(AxisScaleFailureSubreason.NUMERIC_LABELS_MISSING)
            if (numericLabels > 0 && xDistinct.isEmpty() && yDistinct.isEmpty()) {
                add(AxisScaleFailureSubreason.LABEL_BOXES_FOUND_NO_GEOMETRY)
            }
            if (!xDistinct.isMonotonicByPixelAndValue() || !yDistinct.isMonotonicByPixelAndValue()) {
                add(AxisScaleFailureSubreason.LABEL_SEQUENCE_NON_MONOTONIC)
            }
            val tickCount = tickGeometry?.xTicks.orEmpty().size + tickGeometry?.yTicks.orEmpty().size
            if (tickCount < 2 && (xDistinct.size >= 2 || yDistinct.size >= 2)) {
                add(AxisScaleFailureSubreason.TICK_MARKS_MISSING_BUT_LABELS_AVAILABLE)
            }
            if (xDistinct.size < 2 || yDistinct.size < 2) add(AxisScaleFailureSubreason.INSUFFICIENT_SCALE_ANCHORS)
            if (xFit.warnings.any { it.contains("residual", ignoreCase = true) } ||
                yFit.warnings.any { it.contains("residual", ignoreCase = true) }
            ) {
                add(AxisScaleFailureSubreason.SCALE_FIT_HIGH_RESIDUAL)
            }
            if (axisGeometry?.xAxisLinePx == null || axisGeometry.yAxisLinePx == null) {
                add(AxisScaleFailureSubreason.AXIS_FRAME_INCONSISTENT)
            }
            if (rejected.any { it.evidenceType == AxisScaleEvidenceType.SEMANTIC_TEXT_REJECTED }) {
                add(AxisScaleFailureSubreason.TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL)
            }
        }.distinct()

        val status = when {
            xFit.status == CalibrationFitStatus.VALID && yFit.status == CalibrationFitStatus.VALID -> CalibrationFitStatus.VALID
            xFit.status != CalibrationFitStatus.INVALID && yFit.status != CalibrationFitStatus.INVALID -> CalibrationFitStatus.REVIEW
            else -> CalibrationFitStatus.INVALID
        }

        return AxisScaleResolutionResult(
            status = status,
            xFit = xFit,
            yFit = yFit,
            xAnchors = xDistinct,
            yAnchors = yDistinct,
            rejectedAnchors = rejected.distinctBy { "${it.axis}:${it.rawText}:${it.rejectionReason}" },
            subreasons = subreasons,
            warnings = buildList {
                addAll(axisLabelOcrResult?.warnings.orEmpty())
                addAll(tickOcrResult?.warnings.orEmpty())
                if (xDistinct.any { it.evidenceType == AxisScaleEvidenceType.OCR_LABEL_BOX } ||
                    yDistinct.any { it.evidenceType == AxisScaleEvidenceType.OCR_LABEL_BOX }
                ) {
                    add("axis_scale.ocr_label_boxes_used")
                }
                if (subreasons.contains(AxisScaleFailureSubreason.TICK_MARKS_MISSING_BUT_LABELS_AVAILABLE)) {
                    add("axis_scale.tick_marks_missing_but_label_boxes_available")
                }
            }.distinct(),
        )
    }

    private fun invalidResult(
        subreasons: List<AxisScaleFailureSubreason>,
        warning: String,
    ): AxisScaleResolutionResult =
        AxisScaleResolutionResult(
            status = CalibrationFitStatus.INVALID,
            subreasons = subreasons,
            warnings = listOf(warning),
        )

    private fun OcrTextElement.preferredAxis(
        result: AxisOcrResult?,
        plot: GraphRegion,
        panel: GraphRegion,
    ): GeometryAxis? {
        val value = numericValue ?: return null
        val cx = this.centerX
        val cy = this.centerY
        val inXBand = cy >= plot.bottom - plot.height * 0.04f &&
            cy <= panel.bottom + plot.height * 0.24f &&
            cx >= plot.x - plot.width * 0.05f &&
            cx <= plot.right + plot.width * 0.05f
        val inYBand = cx <= plot.x + plot.width * 0.25f &&
            cx >= panel.x - plot.width * 0.22f &&
            cy >= plot.y - plot.height * 0.06f &&
            cy <= plot.bottom + plot.height * 0.06f

        val inSuggestedX = result?.suggestedXValues.orEmpty().containsValue(value)
        val inSuggestedY = result?.suggestedYValues.orEmpty().containsValue(value)
        return when {
            inXBand && !inYBand -> GeometryAxis.X
            inYBand && !inXBand -> GeometryAxis.Y
            inXBand && inSuggestedX && !inSuggestedY -> GeometryAxis.X
            inYBand && inSuggestedY && !inSuggestedX -> GeometryAxis.Y
            inXBand && !inSuggestedY -> GeometryAxis.X
            inYBand && !inSuggestedX -> GeometryAxis.Y
            else -> null
        }
    }

    private fun OcrTextElement.toLabelAnchor(
        axis: GeometryAxis,
        value: Double,
        plot: GraphRegion,
        tickGeometry: TickGeometry?,
    ): AxisScaleAnchor {
        val rawCoordinate = when (axis) {
            GeometryAxis.X -> centerX
            GeometryAxis.Y -> centerY
        }
        val ticks = when (axis) {
            GeometryAxis.X -> tickGeometry?.xTicks.orEmpty().map { it.pixelCoordinate }
            GeometryAxis.Y -> tickGeometry?.yTicks.orEmpty().map { it.pixelCoordinate }
        }
        val tolerance = when (axis) {
            GeometryAxis.X -> plot.width * 0.035f
            GeometryAxis.Y -> plot.height * 0.035f
        }.coerceAtLeast(6f)
        val projectedTick = ticks
            .minByOrNull { abs(it - rawCoordinate) }
            ?.takeIf { abs(it - rawCoordinate) <= tolerance }
        val useTickProjection = projectedTick != null
        val absolutePixel = projectedTick ?: rawCoordinate
        val relativePixel = when (axis) {
            GeometryAxis.X -> absolutePixel - plot.x
            GeometryAxis.Y -> absolutePixel - plot.y
        }.coerceAtLeast(0f)
        return AxisScaleAnchor(
            axis = axis,
            pixelCoordinate = relativePixel,
            numericValue = value,
            evidenceType = if (useTickProjection) AxisScaleEvidenceType.LABEL_PROJECTION else AxisScaleEvidenceType.OCR_LABEL_BOX,
            confidence = confidence,
            rawText = text,
            projectionSource = if (useTickProjection) "nearest_deterministic_tick" else "ocr_label_box_center",
        )
    }

    private fun OcrTextElement.rejectedAnchor(
        axis: GeometryAxis,
        reason: String,
        evidenceType: AxisScaleEvidenceType,
    ): AxisScaleAnchor =
        AxisScaleAnchor(
            axis = axis,
            pixelCoordinate = when (axis) {
                GeometryAxis.X -> centerX
                GeometryAxis.Y -> centerY
            },
            numericValue = numericValue?.toDouble(),
            evidenceType = evidenceType,
            confidence = confidence,
            rawText = text,
            rejectionReason = reason,
        )

    private fun OcrTextElement.semanticScaleRejectionReason(): String? {
        val lower = text.lowercase()
        return lower.scaleRejectionReason()
    }

    private fun String.scaleRejectionReason(): String? {
        val lower = lowercase()
        return when {
            "m/z" in lower || "ion" in lower || lower.startsWith("sim") || "scan" in lower ||
                " to " in lower || "):" in lower ->
                "axis_scale.title_ion_or_method_text_rejected"
            else -> null
        }
    }

    private fun AxisScaleAnchor.toCalibrationAnchor(): CalibrationAnchorEvidence =
        CalibrationAnchorEvidence(
            axis = axis,
            tickPixelPosition = pixelCoordinate,
            value = numericValue ?: 0.0,
            rawText = rawText,
            localCropPath = cropPath,
            confidence = confidence,
            rejectionReason = rejectionReason,
            evidenceType = evidenceType,
            evidenceSource = evidenceType.name.lowercase(),
            projectionSource = projectionSource,
        )

    private fun AxisCalibrationFit.downgradeLabelOnlyFitIfNeeded(
        anchors: List<AxisScaleAnchor>,
    ): AxisCalibrationFit {
        if (status != CalibrationFitStatus.VALID) return this
        val hasGeometrySupport = anchors.any {
            it.evidenceType == AxisScaleEvidenceType.EXPLICIT_TICK_MARK ||
                it.evidenceType == AxisScaleEvidenceType.LABEL_PROJECTION ||
                it.evidenceType == AxisScaleEvidenceType.GRID_LINE
        }
        return if (hasGeometrySupport) {
            this
        } else {
            copy(
                status = CalibrationFitStatus.REVIEW,
                warnings = warnings + "axis_scale.label_box_only_review",
            )
        }
    }

    private fun List<AxisScaleAnchor>.bestPerValue(): List<AxisScaleAnchor> =
        filter { it.numericValue != null && it.rejectionReason == null }
            .groupBy { "${it.axis}:${it.numericValue}" }
            .values
            .map { anchors -> anchors.maxBy { it.confidence } }
            .sortedBy { it.pixelCoordinate }

    private fun List<AxisScaleAnchor>.isMonotonicByPixelAndValue(): Boolean {
        val values = filter { it.numericValue != null }
            .sortedBy { it.pixelCoordinate }
            .mapNotNull { it.numericValue }
        if (values.size < 3) return true
        val increasing = values.zipWithNext().all { (a, b) -> b > a }
        val decreasing = values.zipWithNext().all { (a, b) -> b < a }
        return increasing || decreasing
    }

    private fun List<Float>.containsValue(value: Float): Boolean =
        any { abs(it - value) < 0.0001f }

    private val OcrTextElement.centerX: Float
        get() = x + width / 2f

    private val OcrTextElement.centerY: Float
        get() = y + height / 2f
}
