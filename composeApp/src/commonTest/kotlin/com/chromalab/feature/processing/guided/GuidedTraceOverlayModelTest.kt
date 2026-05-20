package com.chromalab.feature.processing.guided

import com.chromalab.feature.processing.geometry.GeometryPoint
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.reports.EvidenceGateStatus
import com.chromalab.feature.reports.GateEvidence
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GuidedTraceOverlayModelTest {

    @Test
    fun cannotConfirmTraceWithoutConfirmedPlotArea() {
        val state = baseState(GuidedDigitizationMode.GUIDED_PRODUCTION).copy(calibration = confirmedCalibration())

        assertFailsWith<IllegalArgumentException> {
            GuidedTraceOverlayReducer.acceptTrace(
                state = state,
                snapshot = validSnapshot(),
                userProvenance = user,
                timestampEpochMillis = 20L,
                reviewGrade = false,
            )
        }
    }

    @Test
    fun cannotConfirmValidTraceWithNoPoints() {
        val snapshot = validSnapshot().copy(tracePoints = emptyList(), qualitySummary = TraceQualitySummary())

        val evaluation = snapshot.evaluation

        assertEquals(TraceQualityStatus.INVALID, evaluation.qualityStatus)
        assertTrue(evaluation.errorCodes.contains("trace.points_missing"))
        assertFailsWith<IllegalArgumentException> {
            GuidedTraceOverlayReducer.acceptTrace(
                state = confirmedGeometryState(GuidedDigitizationMode.GUIDED_PRODUCTION),
                snapshot = snapshot,
                userProvenance = user,
                timestampEpochMillis = 21L,
                reviewGrade = false,
            )
        }
    }

    @Test
    fun traceOutsidePlotAreaIsInvalid() {
        val snapshot = validSnapshot().copy(
            tracePoints = validTracePoints() + TraceOverlayPoint(350f, 90f),
        )

        val evaluation = snapshot.evaluation

        assertEquals(TraceQualityStatus.INVALID, evaluation.qualityStatus)
        assertTrue(evaluation.errorCodes.contains("trace.points_outside_plot_area"))
    }

    @Test
    fun sparseFragmentedTraceBecomesReviewOrInvalid() {
        val snapshot = validSnapshot().copy(
            tracePoints = (0 until 16).map { index ->
                TraceOverlayPoint(20f + index * 4f, 80f, 0.6f)
            },
            qualitySummary = TraceQualitySummary(
                pointCount = 16,
                columnCoverageRatio = 0.12,
                maxGapColumns = 18,
                componentCount = 5,
                branchPointCount = 2,
                textContaminationScore = 0.1,
                frameTouchRatio = 0.1,
                traceConfidence = 0.6,
            ),
        )

        val evaluation = snapshot.evaluation

        assertTrue(evaluation.qualityStatus == TraceQualityStatus.REVIEW || evaluation.qualityStatus == TraceQualityStatus.INVALID)
        assertTrue(
            evaluation.warningCodes.contains("trace.column_coverage_review") ||
                evaluation.warningCodes.contains("trace.fragmented_review") ||
                evaluation.errorCodes.isNotEmpty(),
        )
    }

    @Test
    fun userCanAcceptTraceAsReviewGrade() {
        val snapshot = validSnapshot().copy(
            qualitySummary = validQuality().copy(columnCoverageRatio = 0.20),
        )

        val state = GuidedTraceOverlayReducer.acceptTrace(
            state = confirmedGeometryState(GuidedDigitizationMode.GUIDED_PRODUCTION),
            snapshot = snapshot,
            userProvenance = user,
            timestampEpochMillis = 22L,
            reviewGrade = true,
        )

        assertEquals(TraceGateStatus.REVIEW_REQUIRED, state.trace?.gateStatus)
        assertEquals(TraceConfirmationStatus.USER_CONFIRMED_REVIEW, state.trace?.traceConfirmationStatus)
        assertEquals(TraceOverlaySource.USER_REVIEW_CONFIRMED, state.trace?.source)
    }

    @Test
    fun userCanRejectTrace() {
        val state = GuidedTraceOverlayReducer.rejectTrace(
            state = confirmedGeometryState(GuidedDigitizationMode.GUIDED_PRODUCTION),
            snapshot = validSnapshot(),
            userProvenance = user,
            timestampEpochMillis = 23L,
            reason = "wrong_trace",
        )

        assertEquals(UserConfirmationStatus.REJECTED, state.trace?.confirmationStatus)
        assertEquals(TraceGateStatus.INVALID, state.trace?.gateStatus)
        assertEquals("wrong_trace", state.trace?.evidence?.rejectionReason)
    }

    @Test
    fun resetRestoresAutoCandidate() {
        val snapshot = validSnapshot().copy(
            tracePoints = emptyList(),
        )

        val reset = GuidedTraceOverlayReducer.resetToSuggestion(snapshot)

        assertEquals(validTracePoints().size, reset.tracePoints.size)
        assertEquals(TraceOverlaySource.AUTO_EXTRACTED, reset.source)
    }

    @Test
    fun autoDiagnosticTraceCannotCountAsUserConfirmed() {
        val state = GuidedTraceOverlayReducer.acceptTrace(
            state = confirmedGeometryState(GuidedDigitizationMode.AUTO_DIAGNOSTIC),
            snapshot = validSnapshot(),
            userProvenance = user,
            timestampEpochMillis = 24L,
            reviewGrade = false,
        )

        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(EvidenceGateStatus.MISSING, gate.evidence.traceStatus)
        assertEquals(EvidenceGateStatus.NOT_APPLICABLE, gate.evidence.userConfirmationStatus)
    }

    @Test
    fun guidedProductionGateAcceptsUserConfirmedValidTrace() {
        val state = GuidedTraceOverlayReducer.acceptTrace(
            state = confirmedGeometryState(GuidedDigitizationMode.GUIDED_PRODUCTION),
            snapshot = validSnapshot(),
            userProvenance = user,
            timestampEpochMillis = 25L,
            reviewGrade = false,
            overlayArtifactPath = "trace-overlay.png",
        )

        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.traceStatus)
        assertEquals("trace-overlay.png", state.trace?.evidence?.overlayArtifactPath)
        assertEquals("calibration-1", state.trace?.evidence?.calibrationSetId)
    }

    @Test
    fun serializationRoundTripPreservesTracePointsMetricsAndDecision() {
        val state = GuidedTraceOverlayReducer.acceptTrace(
            state = confirmedGeometryState(GuidedDigitizationMode.GUIDED_PRODUCTION),
            snapshot = validSnapshot(),
            userProvenance = user,
            timestampEpochMillis = 26L,
            reviewGrade = false,
        )
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        val decoded = json.decodeFromString<GuidedDigitizationState>(
            json.encodeToString(state),
        )

        assertEquals(validTracePoints().size, decoded.trace?.evidence?.tracePoints?.size)
        assertEquals(TraceConfirmationStatus.USER_CONFIRMED_VALID, decoded.trace?.traceConfirmationStatus)
        assertEquals(0.82, decoded.trace?.evidence?.qualitySummary?.columnCoverageRatio)
    }

    private fun validSnapshot(): TraceOverlayEditorSnapshot =
        GuidedTraceOverlayReducer.initialSnapshot(
            imageWidth = 400,
            imageHeight = 240,
            graphPanelBounds = graphPanel,
            plotAreaBounds = plotArea,
            sourceTraceId = "trace-1",
            tracePoints = validTracePoints(),
            qualitySummary = validQuality(),
            calibrationSetId = "calibration-1",
            overlayArtifactPath = "trace-overlay.png",
            maskArtifactPath = "trace-mask.png",
            centerlineArtifactPath = "centerline.png",
        )

    private fun validTracePoints(): List<TraceOverlayPoint> =
        (0 until 80).map { index ->
            TraceOverlayPoint(
                x = 20f + index * 2f,
                y = 80f + (index % 8) * 0.5f,
                confidence = 0.95f,
            )
        }

    private fun validQuality(): TraceQualitySummary =
        TraceQualitySummary(
            pointCount = 80,
            columnCoverageRatio = 0.82,
            maxGapColumns = 2,
            componentCount = 1,
            branchPointCount = 0,
            selectedComponentCoverage = 0.82,
            textContaminationScore = 0.05,
            baselineTouchRatio = 0.04,
            frameTouchRatio = 0.03,
            traceConfidence = 0.95,
            confidence = 0.95,
        )

    private fun confirmedGeometryState(mode: GuidedDigitizationMode): GuidedDigitizationState =
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
            calibration = confirmedCalibration(),
        )

    private fun confirmedCalibration(): UserConfirmedCalibration {
        val anchors = listOf(
            calibrationAnchor("x-1", CalibrationAxis.X, 20f, 120f, 0.0),
            calibrationAnchor("x-2", CalibrationAxis.X, 100f, 120f, 8.0),
            calibrationAnchor("x-3", CalibrationAxis.X, 180f, 120f, 16.0),
            calibrationAnchor("y-1", CalibrationAxis.Y, 20f, 20f, 100.0),
            calibrationAnchor("y-2", CalibrationAxis.Y, 20f, 70f, 50.0),
            calibrationAnchor("y-3", CalibrationAxis.Y, 20f, 120f, 0.0),
        )
        return UserConfirmedCalibration(
            calibrationSet = UserCalibrationSet(
                calibrationSetId = "calibration-1",
                anchors = anchors,
                source = CalibrationAnchorSource.USER_CONFIRMED,
                xUnitLabel = "min",
                yUnitLabel = "a.u.",
                residualReports = listOf(
                    CalibrationResidualReport(
                        axis = CalibrationAxis.X,
                        acceptedAnchorIds = anchors.filter { it.axis == CalibrationAxis.X }.map { it.anchorId },
                        gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
                    ),
                    CalibrationResidualReport(
                        axis = CalibrationAxis.Y,
                        acceptedAnchorIds = anchors.filter { it.axis == CalibrationAxis.Y }.map { it.anchorId },
                        gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
                    ),
                ),
                timestampEpochMillis = 13L,
                userProvenance = user,
                gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
            ),
            confirmationStatus = UserConfirmationStatus.CONFIRMED,
            timestampEpochMillis = 13L,
            userProvenance = user,
            gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
        )
    }

    private fun calibrationAnchor(
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
            source = CalibrationAnchorSource.USER_CONFIRMED,
            status = CalibrationAnchorStatus.ACCEPTED,
            timestampEpochMillis = 13L,
            userProvenance = user,
            relatedImageId = "image-1",
        )

    private fun baseState(mode: GuidedDigitizationMode): GuidedDigitizationState =
        GuidedDigitizationState(
            stateId = "guided-trace-state",
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
