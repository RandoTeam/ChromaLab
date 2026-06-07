package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CalibrationStrategyEnsembleTest {
    private val ensemble = CalibrationStrategyEnsemble()
    private val plot = GraphRegion(100, 200, 400, 300)
    private val axes = AxisGeometry(axisConfidence = 0.9f)

    @Test
    fun selectsLegacyValidFitWhenAxisScaleResolverIsInvalid() {
        val tickOcr = TickOcrResult(
            items = listOf(
                tick(GeometryAxis.X, 140f, 0.0),
                tick(GeometryAxis.X, 260f, 10.0),
                tick(GeometryAxis.X, 380f, 20.0),
                tick(GeometryAxis.Y, 230f, 300.0),
                tick(GeometryAxis.Y, 350f, 150.0),
                tick(GeometryAxis.Y, 470f, 0.0),
            ),
            timestamp = 1L,
        )
        val invalidResolver = AxisScaleResolutionResult(
            status = CalibrationFitStatus.INVALID,
            subreasons = listOf(AxisScaleFailureSubreason.INSUFFICIENT_SCALE_ANCHORS),
        )

        val result = ensemble.arbitrate(
            plotRegion = plot,
            axisGeometry = axes,
            tickOcrResult = tickOcr,
            axisScaleResolution = invalidResolver,
        )

        assertEquals(CalibrationFitStatus.VALID, result.xFit.status)
        assertEquals(CalibrationFitStatus.VALID, result.yFit.status)
        assertEquals(CalibrationStrategyId.LEGACY_TICK_LOCALIZATION, result.selectedXStrategy)
        assertEquals(CalibrationStrategyId.LEGACY_TICK_LOCALIZATION, result.selectedYStrategy)
        assertTrue(result.selectionReasons.contains(CalibrationSelectionReason.LEGACY_REGRESSION_SHIELD))
    }

    @Test
    fun rejectsTitleIonMzLabelsFromLegacyStrategy() {
        val tickOcr = TickOcrResult(
            items = listOf(
                tick(GeometryAxis.X, 140f, 0.0, rawText = "0.00"),
                tick(GeometryAxis.X, 260f, 71.0, rawText = "Ion 71.00"),
                tick(GeometryAxis.X, 380f, 20.0, rawText = "20.00"),
                tick(GeometryAxis.Y, 230f, 300.0),
                tick(GeometryAxis.Y, 350f, 150.0),
                tick(GeometryAxis.Y, 470f, 0.0),
            ),
            timestamp = 1L,
        )

        val result = ensemble.arbitrate(
            plotRegion = plot,
            axisGeometry = axes,
            tickOcrResult = tickOcr,
            axisScaleResolution = AxisScaleResolutionResult(status = CalibrationFitStatus.INVALID),
        )

        assertTrue(result.xFit.acceptedAnchors.none { it.rawText == "Ion 71.00" })
        assertNotEquals(CalibrationFitStatus.VALID, result.xFit.status)
    }

    @Test
    fun keepsValidLegacyOverReviewResolver() {
        val legacy = TickOcrResult(
            items = listOf(
                tick(GeometryAxis.X, 140f, 0.0),
                tick(GeometryAxis.X, 260f, 10.0),
                tick(GeometryAxis.X, 380f, 20.0),
                tick(GeometryAxis.Y, 230f, 300.0),
                tick(GeometryAxis.Y, 350f, 150.0),
                tick(GeometryAxis.Y, 470f, 0.0),
            ),
            timestamp = 1L,
        )
        val noisyResolver = AxisScaleResolutionResult(
            status = CalibrationFitStatus.REVIEW,
            xFit = AxisCalibrationFit(axis = GeometryAxis.X, status = CalibrationFitStatus.REVIEW, confidence = 0.5f, rmsePx = 18.0, maxResidualPx = 25.0),
            yFit = AxisCalibrationFit(axis = GeometryAxis.Y, status = CalibrationFitStatus.REVIEW, confidence = 0.5f, rmsePx = 18.0, maxResidualPx = 25.0),
        )

        val result = ensemble.arbitrate(plot, axes, legacy, noisyResolver)

        assertEquals(CalibrationStrategyId.LEGACY_TICK_LOCALIZATION, result.selectedXStrategy)
        assertEquals(CalibrationStrategyId.LEGACY_TICK_LOCALIZATION, result.selectedYStrategy)
    }

    @Test
    fun choosesLowerResidualCandidateWhenStatusesMatch() {
        val noisyLegacy = TickOcrResult(
            items = listOf(
                tick(GeometryAxis.X, 140f, 0.0),
                tick(GeometryAxis.X, 260f, 10.0),
                tick(GeometryAxis.X, 380f, 80.0),
                tick(GeometryAxis.Y, 230f, 300.0),
                tick(GeometryAxis.Y, 350f, 150.0),
                tick(GeometryAxis.Y, 470f, -200.0),
            ),
            timestamp = 1L,
        )
        val cleanerResolver = AxisScaleResolutionResult(
            status = CalibrationFitStatus.REVIEW,
            xFit = reviewFit(GeometryAxis.X),
            yFit = reviewFit(GeometryAxis.Y),
        )

        val result = ensemble.arbitrate(plot, axes, noisyLegacy, cleanerResolver)

        assertEquals(CalibrationStrategyId.AXIS_SCALE_RESOLVER, result.selectedXStrategy)
        assertEquals(CalibrationStrategyId.AXIS_SCALE_RESOLVER, result.selectedYStrategy)
    }

    @Test
    fun frameEndpointFallbackDoesNotCreateReviewFromLabelBoxesAlone() {
        val labelOnlyResolver = AxisScaleResolutionResult(
            status = CalibrationFitStatus.REVIEW,
            xAnchors = listOf(
                scaleAnchor(GeometryAxis.X, 0f, 0.0),
                scaleAnchor(GeometryAxis.X, 200f, 10.0),
            ),
            yAnchors = listOf(
                scaleAnchor(GeometryAxis.Y, 0f, 100.0),
                scaleAnchor(GeometryAxis.Y, 200f, 0.0),
            ),
            xFit = AxisCalibrationFit(axis = GeometryAxis.X, status = CalibrationFitStatus.REVIEW, confidence = 0.4f),
            yFit = AxisCalibrationFit(axis = GeometryAxis.Y, status = CalibrationFitStatus.REVIEW, confidence = 0.4f),
        )

        val result = ensemble.arbitrate(
            plotRegion = plot,
            axisGeometry = AxisGeometry(
                axisConfidence = 0.8f,
                xAxisLinePx = AxisLine(100f, 500f, 500f, 500f),
                yAxisLinePx = AxisLine(100f, 200f, 100f, 500f),
            ),
            tickOcrResult = TickOcrResult(timestamp = 1L),
            axisScaleResolution = labelOnlyResolver,
        )

        val endpointResult = result.strategyResults.single {
            it.strategyId == CalibrationStrategyId.FRAME_ENDPOINT_REVIEW_FALLBACK
        }
        assertEquals(CalibrationFitStatus.INVALID, endpointResult.xCandidate.fit.status)
        assertEquals(CalibrationFitStatus.INVALID, endpointResult.yCandidate.fit.status)
        assertNotEquals(CalibrationStrategyId.FRAME_ENDPOINT_REVIEW_FALLBACK, result.selectedXStrategy)
        assertNotEquals(CalibrationStrategyId.FRAME_ENDPOINT_REVIEW_FALLBACK, result.selectedYStrategy)
    }

    @Test
    fun selectsAndroidRuntimeAnchorStrategyWhenSafeRowsAreOnlyUsableCalibrationEvidence() {
        val result = ensemble.arbitrate(
            plotRegion = plot,
            axisGeometry = axes,
            tickOcrResult = TickOcrResult(timestamp = 1L),
            axisScaleResolution = AxisScaleResolutionResult(status = CalibrationFitStatus.INVALID),
            runtimeOcrAnchorRows = runtimeRows(RuntimeOcrAnchorCoordinateFrame.PLOT_RELATIVE),
        )

        assertEquals(CalibrationStrategyId.ANDROID_RUNTIME_OCR_ANCHOR, result.selectedXStrategy)
        assertEquals(CalibrationStrategyId.ANDROID_RUNTIME_OCR_ANCHOR, result.selectedYStrategy)
        assertEquals(CalibrationFitStatus.VALID, result.xFit.status)
        assertEquals(CalibrationFitStatus.VALID, result.yFit.status)
    }

    @Test
    fun convertsAbsoluteRuntimeAnchorRowsToPlotRelativeCalibrationAnchors() {
        val result = ensemble.arbitrate(
            plotRegion = plot,
            axisGeometry = axes,
            tickOcrResult = TickOcrResult(timestamp = 1L),
            axisScaleResolution = AxisScaleResolutionResult(status = CalibrationFitStatus.INVALID),
            runtimeOcrAnchorRows = runtimeRows(RuntimeOcrAnchorCoordinateFrame.IMAGE_ABSOLUTE),
        )

        val androidResult = result.strategyResults.single {
            it.strategyId == CalibrationStrategyId.ANDROID_RUNTIME_OCR_ANCHOR
        }
        assertEquals(listOf(0f, 200f, 400f), androidResult.xCandidate.fit.acceptedAnchors.map { it.tickPixelPosition })
        assertEquals(listOf(0f, 150f, 300f), androidResult.yCandidate.fit.acceptedAnchors.map { it.tickPixelPosition })
        assertEquals(CalibrationStrategyId.ANDROID_RUNTIME_OCR_ANCHOR, result.selectedXStrategy)
    }

    @Test
    fun rejectsUnsafeRuntimeAnchorRowsBeforeCalibrationFit() {
        val unsafeRows = listOf(
            runtimeRow(GeometryAxis.X, 0f, 0.0, rawText = "m/z 71"),
            runtimeRow(GeometryAxis.X, 200f, 10.0, numericSource = "VLM_TEXT_ADVISORY_REJECTED"),
            runtimeRow(GeometryAxis.X, null, 20.0),
            runtimeRow(GeometryAxis.X, 400f, 30.0, geometrySource = AxisScaleEvidenceType.OCR_VALUE_ONLY_REJECTED),
            runtimeRow(GeometryAxis.Y, 0f, 300.0).copy(coordinateFrame = null),
        )

        val result = ensemble.arbitrate(
            plotRegion = plot,
            axisGeometry = axes,
            tickOcrResult = TickOcrResult(timestamp = 1L),
            axisScaleResolution = AxisScaleResolutionResult(status = CalibrationFitStatus.INVALID),
            runtimeOcrAnchorRows = unsafeRows,
        )
        val androidResult = result.strategyResults.single {
            it.strategyId == CalibrationStrategyId.ANDROID_RUNTIME_OCR_ANCHOR
        }

        assertEquals(CalibrationFitStatus.INVALID, androidResult.xCandidate.fit.status)
        assertTrue(androidResult.xCandidate.fit.warnings.any { it.contains("forbidden_text") })
        assertTrue(androidResult.xCandidate.fit.warnings.any { it.contains("vlm_numeric_source") })
        assertTrue(androidResult.xCandidate.fit.warnings.any { it.contains("pixel_missing") })
        assertTrue(androidResult.xCandidate.fit.warnings.any { it.contains("rejected_geometry_source") })
        assertTrue(androidResult.yCandidate.fit.warnings.any { it.contains("coordinate_frame_missing") })
    }

    @Test
    fun keepsLegacyFallbackWhenRuntimeAnchorStrategyIsInvalid() {
        val legacy = TickOcrResult(
            items = listOf(
                tick(GeometryAxis.X, 140f, 0.0),
                tick(GeometryAxis.X, 260f, 10.0),
                tick(GeometryAxis.X, 380f, 20.0),
                tick(GeometryAxis.Y, 230f, 300.0),
                tick(GeometryAxis.Y, 350f, 150.0),
                tick(GeometryAxis.Y, 470f, 0.0),
            ),
            timestamp = 1L,
        )

        val result = ensemble.arbitrate(
            plotRegion = plot,
            axisGeometry = axes,
            tickOcrResult = legacy,
            axisScaleResolution = AxisScaleResolutionResult(status = CalibrationFitStatus.INVALID),
            runtimeOcrAnchorRows = listOf(
                runtimeRow(GeometryAxis.X, 0f, 0.0, rawText = "Ion 71"),
                runtimeRow(GeometryAxis.Y, null, 300.0),
            ),
        )

        assertEquals(CalibrationStrategyId.LEGACY_TICK_LOCALIZATION, result.selectedXStrategy)
        assertEquals(CalibrationStrategyId.LEGACY_TICK_LOCALIZATION, result.selectedYStrategy)
        assertTrue(result.selectionReasons.contains(CalibrationSelectionReason.LEGACY_REGRESSION_SHIELD))
    }

    private fun tick(
        axis: GeometryAxis,
        absolutePixel: Float,
        value: Double,
        rawText: String = value.toInt().toString(),
    ): TickOcrItem =
        TickOcrItem(
            axis = axis,
            tickPixelPosition = absolutePixel,
            rawText = rawText,
            parsedNumericValue = value,
            confidence = 0.82f,
            status = TickOcrItemStatus.ACCEPTED,
        )

    private fun runtimeRows(frame: RuntimeOcrAnchorCoordinateFrame): List<RuntimeOcrAnchorBridgeRow> {
        fun xPixel(plotRelative: Float): Float =
            if (frame == RuntimeOcrAnchorCoordinateFrame.IMAGE_ABSOLUTE) plot.x + plotRelative else plotRelative

        fun yPixel(plotRelative: Float): Float =
            if (frame == RuntimeOcrAnchorCoordinateFrame.IMAGE_ABSOLUTE) plot.y + plotRelative else plotRelative

        return listOf(
            runtimeRow(GeometryAxis.X, xPixel(0f), 0.0, frame = frame),
            runtimeRow(GeometryAxis.X, xPixel(200f), 10.0, frame = frame),
            runtimeRow(GeometryAxis.X, xPixel(400f), 20.0, frame = frame),
            runtimeRow(GeometryAxis.Y, yPixel(0f), 300.0, frame = frame),
            runtimeRow(GeometryAxis.Y, yPixel(150f), 150.0, frame = frame),
            runtimeRow(GeometryAxis.Y, yPixel(300f), 0.0, frame = frame),
        )
    }

    private fun runtimeRow(
        axis: GeometryAxis,
        pixel: Float?,
        value: Double,
        rawText: String = value.toString(),
        frame: RuntimeOcrAnchorCoordinateFrame? = RuntimeOcrAnchorCoordinateFrame.PLOT_RELATIVE,
        numericSource: String = "LOCAL_TICK_CROP_OCR",
        geometrySource: AxisScaleEvidenceType? = AxisScaleEvidenceType.EXPLICIT_TICK_MARK,
    ): RuntimeOcrAnchorBridgeRow =
        RuntimeOcrAnchorBridgeRow(
            runtimeRowId = "runtime-ocr-anchor:test:${axis.name}:$rawText",
            graphId = "graph:1",
            graphIndex = 1,
            axis = axis,
            rawText = rawText,
            parsedNumericValue = value,
            pixelCoordinate = pixel,
            coordinateFrame = frame,
            sourceCropRef = "crop:tick.png",
            sourceCropPath = "tick.png",
            cropFileAvailable = true,
            confidence = 0.91f,
            geometrySource = geometrySource,
            numericSource = numericSource,
            status = TickOcrItemStatus.ACCEPTED,
        )

    private fun scaleAnchor(axis: GeometryAxis, pixel: Float, value: Double): AxisScaleAnchor =
        AxisScaleAnchor(
            axis = axis,
            pixelCoordinate = pixel,
            numericValue = value,
            evidenceType = AxisScaleEvidenceType.OCR_LABEL_BOX,
            confidence = 0.8f,
            rawText = value.toString(),
        )

    private fun reviewFit(axis: GeometryAxis): AxisCalibrationFit =
        AxisCalibrationFit(
            axis = axis,
            status = CalibrationFitStatus.REVIEW,
            acceptedAnchors = listOf(
                CalibrationAnchorEvidence(axis = axis, tickPixelPosition = 0f, value = 0.0),
                CalibrationAnchorEvidence(axis = axis, tickPixelPosition = 100f, value = 10.0),
                CalibrationAnchorEvidence(axis = axis, tickPixelPosition = 200f, value = 20.0),
            ),
            confidence = 0.68f,
            rmsePx = 0.1,
            maxResidualPx = 0.1,
        )
}
