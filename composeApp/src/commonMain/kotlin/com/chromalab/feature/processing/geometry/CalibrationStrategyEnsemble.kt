package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.calibration.AxisCalibrationFitter
import com.chromalab.feature.processing.graph.GraphRegion

class CalibrationStrategyEnsemble(
    private val calibrationFitter: AxisCalibrationFitter = AxisCalibrationFitter(),
) {
    fun arbitrate(
        plotRegion: GraphRegion?,
        axisGeometry: AxisGeometry?,
        tickOcrResult: TickOcrResult?,
        axisScaleResolution: AxisScaleResolutionResult?,
        runtimeOcrAnchorRows: List<RuntimeOcrAnchorBridgeRow> = emptyList(),
    ): CalibrationArbitrationResult {
        if (plotRegion == null) {
            val xInvalid = calibrationFitter.fit(GeometryAxis.X, emptyList())
            val yInvalid = calibrationFitter.fit(GeometryAxis.Y, emptyList())
            return CalibrationArbitrationResult(
                xFit = xInvalid,
                yFit = yInvalid,
                warnings = listOf("calibration_ensemble.plot_region_missing"),
            )
        }

        val results = listOf(
            legacyTickLocalization(plotRegion, axisGeometry, tickOcrResult),
            axisScaleResolver(axisScaleResolution),
            androidRuntimeOcrAnchor(plotRegion, axisGeometry, runtimeOcrAnchorRows),
            ocrLabelBoxDirectFit(plotRegion, axisGeometry, axisScaleResolution),
            gridFrameProjection(axisGeometry),
            regularSequenceFit(plotRegion, axisGeometry, axisScaleResolution),
            frameEndpointReviewFallback(plotRegion, axisGeometry, axisScaleResolution),
        )
        val xCandidates = results.map { it.xCandidate }
        val yCandidates = results.map { it.yCandidate }
        val selectedX = selectBest(GeometryAxis.X, xCandidates)
        val selectedY = selectBest(GeometryAxis.Y, yCandidates)

        return CalibrationArbitrationResult(
            xFit = selectedX.fit,
            yFit = selectedY.fit,
            selectedXStrategy = selectedX.strategyId,
            selectedYStrategy = selectedY.strategyId,
            strategyResults = results,
            selectionReasons = buildList {
                add(selectedX.selectionReason())
                add(selectedY.selectionReason())
                if (
                    selectedX.strategyId == CalibrationStrategyId.LEGACY_TICK_LOCALIZATION ||
                    selectedY.strategyId == CalibrationStrategyId.LEGACY_TICK_LOCALIZATION
                ) {
                    add(CalibrationSelectionReason.LEGACY_REGRESSION_SHIELD)
                }
            }.distinct(),
            warnings = buildList {
                add("calibration_ensemble.selected_x:${selectedX.strategyId.name}")
                add("calibration_ensemble.selected_y:${selectedY.strategyId.name}")
                addAll(results.flatMap { it.warnings })
            }.distinct(),
        )
    }

    private fun legacyTickLocalization(
        plotRegion: GraphRegion,
        axisGeometry: AxisGeometry?,
        tickOcrResult: TickOcrResult?,
    ): CalibrationStrategyResult {
        val xFit = legacyFit(GeometryAxis.X, plotRegion, axisGeometry, tickOcrResult)
        val yFit = legacyFit(GeometryAxis.Y, plotRegion, axisGeometry, tickOcrResult)
        return strategyResult(CalibrationStrategyId.LEGACY_TICK_LOCALIZATION, xFit, yFit)
    }

    private fun legacyFit(
        axis: GeometryAxis,
        plotRegion: GraphRegion,
        axisGeometry: AxisGeometry?,
        tickOcrResult: TickOcrResult?,
    ): AxisCalibrationFit {
        val anchors = tickOcrResult?.acceptedItems.orEmpty()
            .filter { it.axis == axis && it.parsedNumericValue != null && it.tickPixelPosition != null }
            .filterNot { it.rawText.isForbiddenScaleLabel() }
            .map {
                CalibrationAnchorEvidence(
                    axis = axis,
                    tickPixelPosition = when (axis) {
                        GeometryAxis.X -> it.tickPixelPosition!! - plotRegion.x
                        GeometryAxis.Y -> it.tickPixelPosition!! - plotRegion.y
                    }.coerceAtLeast(0f),
                    value = it.parsedNumericValue!!,
                    rawText = it.rawText,
                    localCropPath = it.localCropPath,
                    confidence = it.confidence,
                    evidenceType = AxisScaleEvidenceType.EXPLICIT_TICK_MARK,
                    evidenceSource = CalibrationStrategyId.LEGACY_TICK_LOCALIZATION.name.lowercase(),
                    projectionSource = "legacy_tick_ocr",
                )
            }
        return calibrationFitter.fit(
            axis = axis,
            anchors = anchors,
            axisLengthPx = when (axis) {
                GeometryAxis.X -> plotRegion.width.toFloat()
                GeometryAxis.Y -> plotRegion.height.toFloat()
            },
            geometryCleanliness = axisGeometry?.axisConfidence ?: 0f,
        )
    }

    private fun axisScaleResolver(axisScaleResolution: AxisScaleResolutionResult?): CalibrationStrategyResult {
        val xFit = axisScaleResolution?.xFit ?: calibrationFitter.fit(GeometryAxis.X, emptyList())
        val yFit = axisScaleResolution?.yFit ?: calibrationFitter.fit(GeometryAxis.Y, emptyList())
        return strategyResult(
            strategyId = CalibrationStrategyId.AXIS_SCALE_RESOLVER,
            xFit = xFit,
            yFit = yFit,
            warnings = axisScaleResolution?.warnings.orEmpty(),
        )
    }

    private fun ocrLabelBoxDirectFit(
        plotRegion: GraphRegion,
        axisGeometry: AxisGeometry?,
        axisScaleResolution: AxisScaleResolutionResult?,
    ): CalibrationStrategyResult {
        val xFit = labelBoxFit(
            axis = GeometryAxis.X,
            anchors = axisScaleResolution?.xAnchors.orEmpty(),
            plotRegion = plotRegion,
            axisGeometry = axisGeometry,
            evidenceSource = CalibrationStrategyId.OCR_LABEL_BOX_DIRECT_FIT,
            allowedEvidence = setOf(AxisScaleEvidenceType.OCR_LABEL_BOX, AxisScaleEvidenceType.LABEL_PROJECTION),
        )
        val yFit = labelBoxFit(
            axis = GeometryAxis.Y,
            anchors = axisScaleResolution?.yAnchors.orEmpty(),
            plotRegion = plotRegion,
            axisGeometry = axisGeometry,
            evidenceSource = CalibrationStrategyId.OCR_LABEL_BOX_DIRECT_FIT,
            allowedEvidence = setOf(AxisScaleEvidenceType.OCR_LABEL_BOX, AxisScaleEvidenceType.LABEL_PROJECTION),
        )
        return strategyResult(CalibrationStrategyId.OCR_LABEL_BOX_DIRECT_FIT, xFit, yFit)
    }

    private fun androidRuntimeOcrAnchor(
        plotRegion: GraphRegion,
        axisGeometry: AxisGeometry?,
        runtimeOcrAnchorRows: List<RuntimeOcrAnchorBridgeRow>,
    ): CalibrationStrategyResult {
        val xFit = runtimeOcrAnchorFit(GeometryAxis.X, plotRegion, axisGeometry, runtimeOcrAnchorRows)
        val yFit = runtimeOcrAnchorFit(GeometryAxis.Y, plotRegion, axisGeometry, runtimeOcrAnchorRows)
        return strategyResult(
            strategyId = CalibrationStrategyId.ANDROID_RUNTIME_OCR_ANCHOR,
            xFit = xFit,
            yFit = yFit,
            warnings = buildList {
                if (runtimeOcrAnchorRows.isEmpty()) {
                    add("calibration_ensemble.android_runtime_ocr_anchor_rows_missing")
                }
            },
        )
    }

    private fun runtimeOcrAnchorFit(
        axis: GeometryAxis,
        plotRegion: GraphRegion,
        axisGeometry: AxisGeometry?,
        rows: List<RuntimeOcrAnchorBridgeRow>,
    ): AxisCalibrationFit {
        val rejectedReasons = mutableListOf<String>()
        val anchors = rows
            .filter { it.axis == axis }
            .mapNotNull { row ->
                val anchor = row.toRuntimeCalibrationAnchor(axis, plotRegion, rejectedReasons)
                anchor
            }
            .distinctBy { "${it.tickPixelPosition}:${it.value}:${it.rawText}" }

        val rawFit = calibrationFitter.fit(
            axis = axis,
            anchors = anchors,
            axisLengthPx = when (axis) {
                GeometryAxis.X -> plotRegion.width.toFloat()
                GeometryAxis.Y -> plotRegion.height.toFloat()
            },
            geometryCleanliness = axisGeometry?.axisConfidence ?: 0f,
        )
        val hasGeometrySupport = anchors.any {
            it.evidenceType == AxisScaleEvidenceType.EXPLICIT_TICK_MARK ||
                it.evidenceType == AxisScaleEvidenceType.LABEL_PROJECTION ||
                it.evidenceType == AxisScaleEvidenceType.GRID_LINE
        }
        val strongFit = rawFit.status == CalibrationFitStatus.VALID &&
            anchors.size >= 3 &&
            anchors.runtimeAnchorsMonotonicByPixelAndValue() &&
            hasGeometrySupport &&
            rawFit.warnings.none { it.contains("residual", ignoreCase = true) }
        val runtimeWarnings = buildList {
            if (anchors.isEmpty()) add("calibration_ensemble.android_runtime_ocr_anchor_no_usable_rows")
            if (anchors.size in 1..1) add("calibration_ensemble.android_runtime_ocr_anchor_insufficient_anchors")
            if (anchors.size == 2) add("calibration_ensemble.android_runtime_ocr_anchor_two_anchor_review")
            if (!anchors.runtimeAnchorsMonotonicByPixelAndValue()) add("calibration_ensemble.android_runtime_ocr_anchor_non_monotonic")
            if (!hasGeometrySupport && anchors.isNotEmpty()) add("calibration_ensemble.android_runtime_ocr_anchor_geometry_support_weak")
            addAll(rejectedReasons.distinct())
        }
        return when {
            rawFit.status == CalibrationFitStatus.VALID && !strongFit ->
                rawFit.copy(
                    status = CalibrationFitStatus.REVIEW,
                    warnings = rawFit.warnings + runtimeWarnings +
                        "calibration_ensemble.android_runtime_ocr_anchor_review_only",
                )
            else -> rawFit.copy(warnings = rawFit.warnings + runtimeWarnings)
        }
    }

    private fun regularSequenceFit(
        plotRegion: GraphRegion,
        axisGeometry: AxisGeometry?,
        axisScaleResolution: AxisScaleResolutionResult?,
    ): CalibrationStrategyResult {
        val xAnchors = axisScaleResolution?.xAnchors.orEmpty()
        val yAnchors = axisScaleResolution?.yAnchors.orEmpty()
        val xFit = if (xAnchors.isMonotonicByPixelAndValue()) {
            labelBoxFit(
                axis = GeometryAxis.X,
                anchors = xAnchors,
                plotRegion = plotRegion,
                axisGeometry = axisGeometry,
                evidenceSource = CalibrationStrategyId.REGULAR_SEQUENCE_FIT,
                allowedEvidence = setOf(AxisScaleEvidenceType.OCR_LABEL_BOX, AxisScaleEvidenceType.LABEL_PROJECTION),
                forceEvidenceType = AxisScaleEvidenceType.REGULAR_SEQUENCE,
            )
        } else {
            invalidFit(GeometryAxis.X, "calibration_ensemble.regular_sequence_non_monotonic")
        }
        val yFit = if (yAnchors.isMonotonicByPixelAndValue()) {
            labelBoxFit(
                axis = GeometryAxis.Y,
                anchors = yAnchors,
                plotRegion = plotRegion,
                axisGeometry = axisGeometry,
                evidenceSource = CalibrationStrategyId.REGULAR_SEQUENCE_FIT,
                allowedEvidence = setOf(AxisScaleEvidenceType.OCR_LABEL_BOX, AxisScaleEvidenceType.LABEL_PROJECTION),
                forceEvidenceType = AxisScaleEvidenceType.REGULAR_SEQUENCE,
            )
        } else {
            invalidFit(GeometryAxis.Y, "calibration_ensemble.regular_sequence_non_monotonic")
        }
        return strategyResult(CalibrationStrategyId.REGULAR_SEQUENCE_FIT, xFit, yFit)
    }

    private fun frameEndpointReviewFallback(
        plotRegion: GraphRegion,
        axisGeometry: AxisGeometry?,
        axisScaleResolution: AxisScaleResolutionResult?,
    ): CalibrationStrategyResult {
        val xFit = endpointFallbackFit(
            axis = GeometryAxis.X,
            anchors = axisScaleResolution?.xAnchors.orEmpty(),
            axisLengthPx = plotRegion.width.toFloat(),
            frameAvailable = axisGeometry?.xAxisLinePx != null,
        )
        val yFit = endpointFallbackFit(
            axis = GeometryAxis.Y,
            anchors = axisScaleResolution?.yAnchors.orEmpty(),
            axisLengthPx = plotRegion.height.toFloat(),
            frameAvailable = axisGeometry?.yAxisLinePx != null,
        )
        return strategyResult(CalibrationStrategyId.FRAME_ENDPOINT_REVIEW_FALLBACK, xFit, yFit)
    }

    private fun endpointFallbackFit(
        axis: GeometryAxis,
        anchors: List<AxisScaleAnchor>,
        axisLengthPx: Float,
        frameAvailable: Boolean,
    ): AxisCalibrationFit {
        if (!frameAvailable) return invalidFit(axis, "calibration_ensemble.frame_missing")
        val usableAnchors = anchors
            .filter { it.numericValue != null && it.rejectionReason == null && !it.rawText.orEmpty().isForbiddenScaleLabel() }
        val hasProjectionSupport = usableAnchors.any {
            it.evidenceType == AxisScaleEvidenceType.LABEL_PROJECTION ||
                it.evidenceType == AxisScaleEvidenceType.GRID_LINE ||
                it.evidenceType == AxisScaleEvidenceType.EXPLICIT_TICK_MARK ||
                it.evidenceType == AxisScaleEvidenceType.PLOT_FRAME_EDGE ||
                it.evidenceType == AxisScaleEvidenceType.AXIS_ENDPOINT
        }
        if (!hasProjectionSupport) {
            return invalidFit(axis, "calibration_ensemble.frame_endpoint_projection_evidence_missing")
        }
        val values = usableAnchors
            .mapNotNull { it.numericValue }
            .distinct()
            .sorted()
        if (values.size < 2) return invalidFit(axis, "calibration_ensemble.frame_endpoint_values_missing")
        val endpointAnchors = listOf(
            CalibrationAnchorEvidence(
                axis = axis,
                tickPixelPosition = 0f,
                value = values.first(),
                confidence = 0.42f,
                evidenceType = AxisScaleEvidenceType.FRAME_ENDPOINT,
                evidenceSource = CalibrationStrategyId.FRAME_ENDPOINT_REVIEW_FALLBACK.name.lowercase(),
                projectionSource = "plot_frame_start",
            ),
            CalibrationAnchorEvidence(
                axis = axis,
                tickPixelPosition = axisLengthPx,
                value = values.last(),
                confidence = 0.42f,
                evidenceType = AxisScaleEvidenceType.FRAME_ENDPOINT,
                evidenceSource = CalibrationStrategyId.FRAME_ENDPOINT_REVIEW_FALLBACK.name.lowercase(),
                projectionSource = "plot_frame_end",
            ),
        )
        return invalidFit(axis, "calibration_ensemble.frame_endpoint_review_fallback_disabled")
    }

    private fun gridFrameProjection(axisGeometry: AxisGeometry?): CalibrationStrategyResult {
        val warning = if (axisGeometry?.detectedFrameGridLines.orEmpty().isEmpty()) {
            "calibration_ensemble.grid_frame_projection_no_grid_lines"
        } else {
            "calibration_ensemble.grid_frame_projection_label_pairing_missing"
        }
        return strategyResult(
            strategyId = CalibrationStrategyId.GRID_FRAME_PROJECTION,
            xFit = invalidFit(GeometryAxis.X, warning),
            yFit = invalidFit(GeometryAxis.Y, warning),
            warnings = listOf(warning),
        )
    }

    private fun labelBoxFit(
        axis: GeometryAxis,
        anchors: List<AxisScaleAnchor>,
        plotRegion: GraphRegion,
        axisGeometry: AxisGeometry?,
        evidenceSource: CalibrationStrategyId,
        allowedEvidence: Set<AxisScaleEvidenceType>,
        forceEvidenceType: AxisScaleEvidenceType? = null,
    ): AxisCalibrationFit {
        val calibrationAnchors = anchors
            .filter { it.axis == axis && it.numericValue != null && it.rejectionReason == null }
            .filter { it.evidenceType in allowedEvidence }
            .filterNot { it.rawText.orEmpty().isForbiddenScaleLabel() }
            .map {
                CalibrationAnchorEvidence(
                    axis = axis,
                    tickPixelPosition = it.pixelCoordinate,
                    value = it.numericValue!!,
                    rawText = it.rawText,
                    localCropPath = it.cropPath,
                    confidence = it.confidence,
                    evidenceType = forceEvidenceType ?: it.evidenceType,
                    evidenceSource = evidenceSource.name.lowercase(),
                    projectionSource = it.projectionSource,
                )
            }
        val fit = calibrationFitter.fit(
            axis = axis,
            anchors = calibrationAnchors,
            axisLengthPx = when (axis) {
                GeometryAxis.X -> plotRegion.width.toFloat()
                GeometryAxis.Y -> plotRegion.height.toFloat()
            },
            geometryCleanliness = axisGeometry?.axisConfidence ?: 0f,
        )
        val hasGeometrySupport = calibrationAnchors.any {
            it.evidenceType == AxisScaleEvidenceType.LABEL_PROJECTION ||
                it.evidenceType == AxisScaleEvidenceType.GRID_LINE
        }
        return if (fit.status == CalibrationFitStatus.VALID && !hasGeometrySupport) {
            fit.copy(
                status = CalibrationFitStatus.REVIEW,
                warnings = fit.warnings + "calibration_ensemble.label_box_only_review",
            )
        } else {
            fit
        }
    }

    private fun strategyResult(
        strategyId: CalibrationStrategyId,
        xFit: AxisCalibrationFit,
        yFit: AxisCalibrationFit,
        warnings: List<String> = emptyList(),
    ): CalibrationStrategyResult =
        CalibrationStrategyResult(
            strategyId = strategyId,
            xCandidate = xFit.toCandidate(strategyId),
            yCandidate = yFit.toCandidate(strategyId),
            warnings = warnings,
        )

    private fun AxisCalibrationFit.toCandidate(strategyId: CalibrationStrategyId): CalibrationCandidate =
        CalibrationCandidate(
            strategyId = strategyId,
            axis = axis,
            fit = this,
            score = score(strategyId),
            rejectionReasons = rejectionReasons(),
        )

    private fun selectBest(axis: GeometryAxis, candidates: List<CalibrationCandidate>): CalibrationCandidate =
        candidates
            .filter { it.axis == axis }
            .maxWithOrNull(
                compareBy<CalibrationCandidate> { it.fit.status.rank }
                    .thenBy { it.score }
                    .thenBy { it.fit.acceptedAnchors.size },
            )
            ?: invalidFit(axis, "calibration_ensemble.no_candidates").toCandidate(CalibrationStrategyId.AXIS_SCALE_RESOLVER)

    private fun AxisCalibrationFit.score(strategyId: CalibrationStrategyId): Float {
        val residualPenalty = ((rmsePx ?: 25.0) + (maxResidualPx ?: 25.0)).toFloat() * 4f
        return status.rank * 1000f +
            acceptedAnchors.size * 48f +
            confidence * 160f +
            strategyId.trustScore -
            residualPenalty
    }

    private fun AxisCalibrationFit.rejectionReasons(): List<CalibrationRejectionReason> =
        buildList {
            if (acceptedAnchors.size < 2) {
                add(CalibrationRejectionReason.INSUFFICIENT_ANCHORS)
                add(CalibrationRejectionReason.REJECTED_INSUFFICIENT_ANCHORS)
            }
            if (warnings.any { it.contains("residual", ignoreCase = true) }) {
                add(CalibrationRejectionReason.HIGH_RESIDUALS)
                add(CalibrationRejectionReason.REJECTED_INVALID_RESIDUAL)
            }
            if (warnings.any { it.contains("frame", ignoreCase = true) }) add(CalibrationRejectionReason.AXIS_FRAME_INCONSISTENT)
            if (acceptedAnchors.any { it.rawText.orEmpty().isForbiddenScaleLabel() }) {
                add(CalibrationRejectionReason.FORBIDDEN_TEXT_LABEL)
                add(CalibrationRejectionReason.REJECTED_FORBIDDEN_TEXT)
            }
            if (acceptedAnchors.any { it.tickPixelPosition.isNaN() }) add(CalibrationRejectionReason.REJECTED_NO_PIXEL_GEOMETRY)
            if (status == CalibrationFitStatus.INVALID && isEmpty()) add(CalibrationRejectionReason.STRATEGY_NOT_APPLICABLE)
        }

    private fun RuntimeOcrAnchorBridgeRow.toRuntimeCalibrationAnchor(
        axis: GeometryAxis,
        plotRegion: GraphRegion,
        rejectedReasons: MutableList<String>,
    ): CalibrationAnchorEvidence? {
        fun reject(reason: String): Nothing? {
            rejectedReasons += reason
            return null
        }
        if (status != TickOcrItemStatus.ACCEPTED) {
            return reject("calibration_ensemble.android_runtime_ocr_anchor_row_not_accepted")
        }
        val value = parsedNumericValue
            ?: return reject("calibration_ensemble.android_runtime_ocr_anchor_numeric_value_missing")
        val pixel = pixelCoordinate
            ?: return reject("calibration_ensemble.android_runtime_ocr_anchor_pixel_missing")
        if (rawText.isForbiddenScaleLabel()) {
            return reject("calibration_ensemble.android_runtime_ocr_anchor_forbidden_text")
        }
        if (numericSource.contains("VLM", ignoreCase = true)) {
            return reject("calibration_ensemble.android_runtime_ocr_anchor_vlm_numeric_source")
        }
        val source = geometrySource
            ?: return reject("calibration_ensemble.android_runtime_ocr_anchor_geometry_source_missing")
        if (source == AxisScaleEvidenceType.OCR_VALUE_ONLY_REJECTED ||
            source == AxisScaleEvidenceType.SEMANTIC_TEXT_REJECTED
        ) {
            return reject("calibration_ensemble.android_runtime_ocr_anchor_rejected_geometry_source")
        }
        val relativePixel = when (coordinateFrame) {
            RuntimeOcrAnchorCoordinateFrame.PLOT_RELATIVE -> pixel
            RuntimeOcrAnchorCoordinateFrame.IMAGE_ABSOLUTE -> when (axis) {
                GeometryAxis.X -> pixel - plotRegion.x
                GeometryAxis.Y -> pixel - plotRegion.y
            }
            null -> return reject("calibration_ensemble.android_runtime_ocr_anchor_coordinate_frame_missing")
        }
        val axisLength = when (axis) {
            GeometryAxis.X -> plotRegion.width.toFloat()
            GeometryAxis.Y -> plotRegion.height.toFloat()
        }
        if (relativePixel < 0f || relativePixel > axisLength) {
            return reject("calibration_ensemble.android_runtime_ocr_anchor_pixel_outside_plot")
        }
        return CalibrationAnchorEvidence(
            axis = axis,
            tickPixelPosition = relativePixel,
            value = value,
            rawText = rawText,
            localCropPath = sourceCropPath,
            confidence = confidence,
            evidenceType = source,
            evidenceSource = CalibrationStrategyId.ANDROID_RUNTIME_OCR_ANCHOR.name.lowercase(),
            projectionSource = projectionSource ?: coordinateFrame.name.lowercase(),
        )
    }

    private fun List<CalibrationAnchorEvidence>.runtimeAnchorsMonotonicByPixelAndValue(): Boolean {
        if (size < 3) return true
        val values = sortedBy { it.tickPixelPosition }.map { it.value }
        val increasing = values.zipWithNext().all { (a, b) -> b > a }
        val decreasing = values.zipWithNext().all { (a, b) -> b < a }
        return increasing || decreasing
    }

    private fun invalidFit(axis: GeometryAxis, warning: String): AxisCalibrationFit =
        AxisCalibrationFit(
            axis = axis,
            status = CalibrationFitStatus.INVALID,
            warnings = listOf(warning),
        )

    private fun CalibrationCandidate.selectionReason(): CalibrationSelectionReason =
        when (fit.status) {
            CalibrationFitStatus.VALID -> CalibrationSelectionReason.SELECTED_VALID_LOW_RESIDUAL
            CalibrationFitStatus.REVIEW -> when {
                strategyId == CalibrationStrategyId.LEGACY_TICK_LOCALIZATION -> CalibrationSelectionReason.SELECTED_REVIEW_LEGACY_FALLBACK
                fit.acceptedAnchors.size == 2 -> CalibrationSelectionReason.SELECTED_REVIEW_TWO_ANCHOR_FIT
                else -> CalibrationSelectionReason.SELECTED_REVIEW_BEST_AVAILABLE
            }
            CalibrationFitStatus.INVALID -> CalibrationSelectionReason.ONLY_AVAILABLE
        }

    private fun List<AxisScaleAnchor>.isMonotonicByPixelAndValue(): Boolean {
        val values = filter { it.numericValue != null && it.rejectionReason == null }
            .sortedBy { it.pixelCoordinate }
            .mapNotNull { it.numericValue }
        if (values.size < 3) return true
        val increasing = values.zipWithNext().all { (a, b) -> b > a }
        val decreasing = values.zipWithNext().all { (a, b) -> b < a }
        return increasing || decreasing
    }

    private fun String.isForbiddenScaleLabel(): Boolean {
        val lower = lowercase()
        return "m/z" in lower ||
            "ion" in lower ||
            lower.startsWith("sim") ||
            "scan" in lower ||
            " to " in lower ||
            "):" in lower
    }

    private val CalibrationFitStatus.rank: Int
        get() = when (this) {
            CalibrationFitStatus.VALID -> 3
            CalibrationFitStatus.REVIEW -> 2
            CalibrationFitStatus.INVALID -> 1
        }

    private val CalibrationStrategyId.trustScore: Float
        get() = when (this) {
            CalibrationStrategyId.LEGACY_TICK_LOCALIZATION -> 90f
            CalibrationStrategyId.AXIS_SCALE_RESOLVER -> 78f
            CalibrationStrategyId.ANDROID_RUNTIME_OCR_ANCHOR -> 72f
            CalibrationStrategyId.OCR_LABEL_BOX_DIRECT_FIT -> 62f
            CalibrationStrategyId.REGULAR_SEQUENCE_FIT -> 56f
            CalibrationStrategyId.GRID_FRAME_PROJECTION -> 48f
            CalibrationStrategyId.FRAME_ENDPOINT_REVIEW_FALLBACK -> 30f
        }
}
