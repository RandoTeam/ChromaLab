package com.chromalab.feature.processing.guided

import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryPoint
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.reports.EvidenceGateStatus
import com.chromalab.feature.reports.GateEvidence
import com.chromalab.feature.reports.ReportGateStatus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GuidedCalibrationEditorModelTest {

    @Test
    fun cannotConfirmCalibrationWithoutConfirmedPlotArea() {
        val state = baseState(GuidedDigitizationMode.GUIDED_PRODUCTION)
        val snapshot = validSnapshot()

        assertFailsWith<IllegalArgumentException> {
            GuidedCalibrationEditorReducer.confirmCalibration(
                state = state,
                snapshot = snapshot,
                userProvenance = user,
                timestampEpochMillis = 20L,
            )
        }
    }

    @Test
    fun cannotConfirmWithFewerThanTwoXAnchors() {
        val snapshot = validSnapshot().copy(
            anchors = yAnchors(3) + xAnchors(1),
        )

        val evaluation = snapshot.evaluation

        assertEquals(CalibrationEditorStatus.INVALID, evaluation.editorStatus)
        assertTrue(evaluation.errorCodes.contains("calibration.x.not_enough_anchors"))
        assertFailsWith<IllegalArgumentException> {
            GuidedCalibrationEditorReducer.confirmCalibration(
                state = confirmedRoiState(GuidedDigitizationMode.GUIDED_PRODUCTION),
                snapshot = snapshot,
                userProvenance = user,
                timestampEpochMillis = 21L,
            )
        }
    }

    @Test
    fun cannotConfirmWithFewerThanTwoYAnchors() {
        val snapshot = validSnapshot().copy(
            anchors = xAnchors(3) + yAnchors(1),
        )

        val evaluation = snapshot.evaluation

        assertEquals(CalibrationEditorStatus.INVALID, evaluation.editorStatus)
        assertTrue(evaluation.errorCodes.contains("calibration.y.not_enough_anchors"))
    }

    @Test
    fun cannotConfirmWithNonNumericValues() {
        val snapshot = validSnapshot().copy(
            anchors = xAnchors(2) + yAnchors(2).mapIndexed { index, anchor ->
                if (index == 0) anchor.copy(value = Double.NaN) else anchor
            },
        )

        val evaluation = snapshot.evaluation

        assertEquals(CalibrationEditorStatus.INVALID, evaluation.editorStatus)
        assertTrue(evaluation.errorCodes.contains("calibration.y.value_non_finite"))
    }

    @Test
    fun twoPointXYCalibrationProducesExpectedSlopeAndIntercept() {
        val snapshot = validSnapshot().copy(
            anchors = listOf(
                anchor("x-1", CalibrationAxis.X, 20f, 100f, 0.0),
                anchor("x-2", CalibrationAxis.X, 100f, 100f, 8.0),
                anchor("y-1", CalibrationAxis.Y, 20f, 20f, 100.0),
                anchor("y-2", CalibrationAxis.Y, 20f, 100f, 0.0),
            ),
        )

        val evaluation = snapshot.evaluation

        assertEquals(0.1, assertNotNull(evaluation.xFit.slope), absoluteTolerance = 0.000001)
        assertEquals(-2.0, assertNotNull(evaluation.xFit.intercept), absoluteTolerance = 0.000001)
        assertEquals(-1.25, assertNotNull(evaluation.yFit.slope), absoluteTolerance = 0.000001)
        assertEquals(125.0, assertNotNull(evaluation.yFit.intercept), absoluteTolerance = 0.000001)
        assertEquals(CalibrationEditorStatus.REVIEW, evaluation.editorStatus)
        assertTrue(evaluation.warningCodes.contains("calibration.x.two_anchor_review"))
        assertTrue(evaluation.warningCodes.contains("calibration.y.two_anchor_review"))
    }

    @Test
    fun threePointCalibrationComputesResiduals() {
        val evaluation = validSnapshot().evaluation

        assertEquals(CalibrationFitStatus.VALID, evaluation.xFit.status)
        assertEquals(CalibrationFitStatus.VALID, evaluation.yFit.status)
        assertEquals(3, evaluation.xFit.residualReport.residuals.size)
        assertEquals(3, evaluation.yFit.residualReport.residuals.size)
        assertEquals(0.0, assertNotNull(evaluation.xFit.residualReport.rmsePx), absoluteTolerance = 0.000001)
        assertEquals(0.0, assertNotNull(evaluation.yFit.residualReport.rmsePx), absoluteTolerance = 0.000001)
        assertEquals(CalibrationEditorStatus.VALID, evaluation.editorStatus)
    }

    @Test
    fun outlierHighResidualProducesReviewOrInvalid() {
        val snapshot = validSnapshot().copy(
            anchors = listOf(
                anchor("x-1", CalibrationAxis.X, 20f, 100f, 0.0),
                anchor("x-2", CalibrationAxis.X, 100f, 100f, 8.0),
                anchor("x-3", CalibrationAxis.X, 180f, 100f, 40.0),
            ) + yAnchors(3),
        )

        val evaluation = snapshot.evaluation

        assertTrue(
            evaluation.xFit.status == CalibrationFitStatus.REVIEW ||
                evaluation.xFit.status == CalibrationFitStatus.INVALID,
        )
        assertTrue(evaluation.editorStatus == CalibrationEditorStatus.REVIEW || evaluation.editorStatus == CalibrationEditorStatus.INVALID)
    }

    @Test
    fun movingAnchorUpdatesFit() {
        val snapshot = validSnapshot()
        val before = assertNotNull(snapshot.evaluation.xFit.slope)

        val moved = GuidedCalibrationEditorReducer.moveAnchor(
            snapshot,
            anchorId = "x-3",
            pixel = GeometryPoint(140f, 100f),
        )
        val after = assertNotNull(moved.evaluation.xFit.slope)

        assertTrue(before != after)
    }

    @Test
    fun removingAnchorUpdatesStatus() {
        val snapshot = validSnapshot()
        val reduced = GuidedCalibrationEditorReducer
            .removeAnchor(snapshot, "y-3")
            .let { GuidedCalibrationEditorReducer.removeAnchor(it, "y-2") }

        assertEquals(CalibrationEditorStatus.INVALID, reduced.evaluation.editorStatus)
        assertTrue(reduced.evaluation.errorCodes.contains("calibration.y.not_enough_anchors"))
    }

    @Test
    fun resetClearsAnchors() {
        val reset = GuidedCalibrationEditorReducer.resetAnchors(validSnapshot())

        assertTrue(reset.anchors.isEmpty())
        assertEquals(CalibrationEditorStatus.INVALID, reset.evaluation.editorStatus)
    }

    @Test
    fun autoDiagnosticCannotClaimUserConfirmedCalibration() {
        val state = GuidedCalibrationEditorReducer.confirmCalibration(
            state = confirmedRoiState(GuidedDigitizationMode.AUTO_DIAGNOSTIC),
            snapshot = validSnapshot(),
            userProvenance = user,
            timestampEpochMillis = 30L,
        )

        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(ReportGateStatus.DIAGNOSTIC_ONLY, gate.status)
        assertEquals(EvidenceGateStatus.MISSING, gate.evidence.xCalibrationStatus)
        assertEquals(EvidenceGateStatus.NOT_APPLICABLE, gate.evidence.userConfirmationStatus)
    }

    @Test
    fun guidedProductionGateAcceptsUserConfirmedValidCalibration() {
        val state = GuidedCalibrationEditorReducer.confirmCalibration(
            state = confirmedRoiState(GuidedDigitizationMode.GUIDED_PRODUCTION),
            snapshot = validSnapshot(),
            userProvenance = user,
            timestampEpochMillis = 31L,
            overlayArtifactPath = "calibration-overlay.png",
        )

        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.xCalibrationStatus)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.yCalibrationStatus)
        assertTrue(gate.blockingReasons.contains("trace.missing"))
        assertEquals("calibration-overlay.png", state.calibration?.overlayArtifactPath)
    }

    @Test
    fun serializationRoundTripPreservesAnchorsAndResiduals() {
        val state = GuidedCalibrationEditorReducer.confirmCalibration(
            state = confirmedRoiState(GuidedDigitizationMode.GUIDED_PRODUCTION),
            snapshot = validSnapshot(),
            userProvenance = user,
            timestampEpochMillis = 32L,
        )
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        val decoded = json.decodeFromString<GuidedDigitizationState>(
            json.encodeToString(state),
        )

        assertEquals(6, decoded.calibration?.calibrationSet?.anchors?.size)
        assertEquals(2, decoded.calibration?.calibrationSet?.residualReports?.size)
        assertEquals(GuidedWorkflowStep.CALIBRATION_VALIDATED, decoded.currentStep)
    }

    private fun validSnapshot(): CalibrationAnchorEditorSnapshot =
        GuidedCalibrationEditorReducer.initialSnapshot(
            imageWidth = 400,
            imageHeight = 240,
            graphPanelBounds = graphPanel,
            plotAreaBounds = plotArea,
            anchors = xAnchors(3) + yAnchors(3),
        )

    private fun xAnchors(count: Int): List<ManualCalibrationAnchor> =
        (0 until count).map { index ->
            anchor(
                id = "x-${index + 1}",
                axis = CalibrationAxis.X,
                x = 20f + index * 80f,
                y = 100f,
                value = index * 8.0,
            )
        }

    private fun yAnchors(count: Int): List<ManualCalibrationAnchor> =
        (0 until count).map { index ->
            anchor(
                id = "y-${index + 1}",
                axis = CalibrationAxis.Y,
                x = 20f,
                y = 20f + index * 40f,
                value = 100.0 - index * 50.0,
            )
        }

    private fun anchor(
        id: String,
        axis: CalibrationAxis,
        x: Float,
        y: Float,
        value: Double,
    ): ManualCalibrationAnchor =
        ManualCalibrationAnchor(
            anchorId = id,
            axis = axis,
            pixel = GeometryPoint(x, y),
            value = value,
            unitLabel = if (axis == CalibrationAxis.X) "min" else "a.u.",
            source = CalibrationAnchorSource.MANUAL,
            timestampEpochMillis = 10L,
            userProvenance = user,
            relatedImageId = "image-1",
        )

    private fun confirmedRoiState(mode: GuidedDigitizationMode): GuidedDigitizationState =
        baseState(mode).copy(
            graphPanelConfirmation = GraphPanelConfirmation(
                confirmedGraphPanel = UserConfirmedGraphPanel(
                    bounds = graphPanel,
                    source = RoiEditSource.USER_CONFIRMED,
                    confirmationStatus = UserConfirmationStatus.CONFIRMED,
                    timestampEpochMillis = 11L,
                    userProvenance = user,
                    relatedImageId = "image-1",
                    gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
                ),
                confirmationStatus = UserConfirmationStatus.CONFIRMED,
                gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
            ),
            plotAreaConfirmation = PlotAreaConfirmation(
                confirmedPlotArea = UserConfirmedPlotArea(
                    bounds = plotArea,
                    parentGraphPanelBounds = graphPanel,
                    source = RoiEditSource.USER_CONFIRMED,
                    confirmationStatus = UserConfirmationStatus.CONFIRMED,
                    timestampEpochMillis = 12L,
                    userProvenance = user,
                    relatedImageId = "image-1",
                    gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
                ),
                confirmationStatus = UserConfirmationStatus.CONFIRMED,
                gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
            ),
        )

    private fun baseState(mode: GuidedDigitizationMode): GuidedDigitizationState =
        GuidedDigitizationState(
            stateId = "guided-calibration-state",
            mode = mode,
            image = GuidedImageReference(
                imageId = "image-1",
                originalImagePath = "original.png",
                normalizedImagePath = "normalized.png",
                width = 400,
                height = 240,
            ),
            autoDiagnosticEvidence = GateEvidence(
                evidencePackageStatus = EvidenceGateStatus.VALID,
                sourceProvenanceStatus = EvidenceGateStatus.VALID,
            ),
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )

    private companion object {
        val graphPanel = GraphRegion(0, 0, 220, 140, "graphPanel")
        val plotArea = GraphRegion(20, 20, 180, 100, "plotArea")
        val user = GuidedUserProvenance(
            userIdHash = "user-hash",
            sessionId = "session-1",
            deviceIdHash = "device-hash",
            appVersion = "test",
        )
    }
}
