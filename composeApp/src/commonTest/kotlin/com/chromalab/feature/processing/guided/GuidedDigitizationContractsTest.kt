package com.chromalab.feature.processing.guided

import com.chromalab.feature.processing.geometry.GeometryPoint
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.reports.EvidenceGateStatus
import com.chromalab.feature.reports.GateEvidence
import com.chromalab.feature.reports.ProcessingMode
import com.chromalab.feature.reports.ReportGateStatus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuidedDigitizationContractsTest {

    @Test
    fun guidedModesMapToReportProcessingModes() {
        assertEquals(ProcessingMode.AUTO_DIAGNOSTIC, GuidedDigitizationMode.AUTO_DIAGNOSTIC.toProcessingMode())
        assertEquals(ProcessingMode.GUIDED_PRODUCTION, GuidedDigitizationMode.GUIDED_PRODUCTION.toProcessingMode())
        assertEquals(ProcessingMode.MANUAL_ADVANCED, GuidedDigitizationMode.MANUAL_ADVANCED.toProcessingMode())
    }

    @Test
    fun stateMachineAllowsOnlySequentialForwardTransitionsOrDiagnosticTerminal() {
        val state = baseState()

        assertTrue(
            GuidedDigitizationStateMachine.canTransitionTo(
                state,
                GuidedWorkflowStep.GRAPH_PANEL_SUGGESTED,
            ),
        )
        assertFalse(
            GuidedDigitizationStateMachine.canTransitionTo(
                state,
                GuidedWorkflowStep.PLOT_AREA_CONFIRMED,
            ),
        )
        assertTrue(
            GuidedDigitizationStateMachine.canTransitionTo(
                state,
                GuidedWorkflowStep.DIAGNOSTIC_ONLY,
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            GuidedDigitizationStateMachine.transitionTo(
                state = state,
                targetStep = GuidedWorkflowStep.PLOT_AREA_CONFIRMED,
                timestampEpochMillis = 2L,
            )
        }
    }

    @Test
    fun guidedStateSerializationRoundTripPreservesConfirmations() {
        val state = releaseReadyGuidedState()
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        val encoded = json.encodeToString(state)
        val decoded = json.decodeFromString<GuidedDigitizationState>(encoded)

        assertEquals(state.stateId, decoded.stateId)
        assertEquals(GuidedDigitizationMode.GUIDED_PRODUCTION, decoded.mode)
        assertEquals(UserConfirmationStatus.CONFIRMED, decoded.graphPanelConfirmation?.confirmationStatus)
        assertEquals(3, decoded.calibration?.calibrationSet?.acceptedAnchors(CalibrationAxis.X)?.size)
        assertEquals(3, decoded.calibration?.calibrationSet?.acceptedAnchors(CalibrationAxis.Y)?.size)
    }

    @Test
    fun userConfirmedGuidedStateMapsToReleaseReadyWhenAllRequiredGatesExist() {
        val gate = GuidedReportGateMapper.evaluate(releaseReadyGuidedState())

        assertEquals(ReportGateStatus.RELEASE_READY, gate.status)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.graphPanelStatus)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.plotAreaStatus)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.xCalibrationStatus)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.yCalibrationStatus)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.traceStatus)
        assertTrue(gate.blockingReasons.isEmpty())
    }

    @Test
    fun missingGuidedConfirmationBlocksReleaseReady() {
        val state = releaseReadyGuidedState().copy(trace = null)
        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(ReportGateStatus.DIAGNOSTIC_ONLY, gate.status)
        assertTrue(gate.blockingReasons.contains("trace.missing"))
    }

    @Test
    fun autoDiagnosticModeCannotUseUserConfirmationsAsReleaseEvidence() {
        val state = releaseReadyGuidedState().copy(
            mode = GuidedDigitizationMode.AUTO_DIAGNOSTIC,
            autoDiagnosticEvidence = GateEvidence(
                graphPanelStatus = EvidenceGateStatus.MISSING,
                plotAreaStatus = EvidenceGateStatus.MISSING,
                xCalibrationStatus = EvidenceGateStatus.MISSING,
                yCalibrationStatus = EvidenceGateStatus.MISSING,
                traceStatus = EvidenceGateStatus.MISSING,
                evidencePackageStatus = EvidenceGateStatus.VALID,
                sourceProvenanceStatus = EvidenceGateStatus.VALID,
            ),
        )
        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(ReportGateStatus.DIAGNOSTIC_ONLY, gate.status)
        assertEquals(EvidenceGateStatus.MISSING, gate.evidence.graphPanelStatus)
        assertEquals(EvidenceGateStatus.NOT_APPLICABLE, gate.evidence.userConfirmationStatus)
    }

    @Test
    fun calibrationRequiresAtLeastTwoAcceptedAnchorsPerAxis() {
        val calibration = confirmedCalibration(
            xAnchorCount = 2,
            yAnchorCount = 1,
            gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
        )
        val state = releaseReadyGuidedState().copy(calibration = calibration)
        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(ReportGateStatus.DIAGNOSTIC_ONLY, gate.status)
        assertTrue(gate.blockingReasons.contains("y_calibration.invalid"))
    }

    @Test
    fun twoAnchorCalibrationIsReviewGradeUntilRobustFitReady() {
        val state = releaseReadyGuidedState().copy(
            calibration = confirmedCalibration(
                xAnchorCount = 2,
                yAnchorCount = 2,
                gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
            ),
        )
        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(ReportGateStatus.REVIEW_ONLY, gate.status)
        assertTrue(gate.reviewReasons.contains("x_calibration.review"))
        assertTrue(gate.reviewReasons.contains("y_calibration.review"))
    }

    private fun baseState(): GuidedDigitizationState =
        GuidedDigitizationState(
            stateId = "guided-state-1",
            mode = GuidedDigitizationMode.GUIDED_PRODUCTION,
            image = GuidedImageReference(
                imageId = "image-1",
                originalImagePath = "original.png",
                normalizedImagePath = "normalized.png",
            ),
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )

    private fun releaseReadyGuidedState(): GuidedDigitizationState =
        baseState().copy(
            graphPanelConfirmation = GraphPanelConfirmation(
                suggestedBounds = graphPanel,
                confirmedGraphPanel = UserConfirmedGraphPanel(
                    bounds = graphPanel,
                    source = RoiEditSource.USER_DRAG,
                    confirmationStatus = UserConfirmationStatus.CONFIRMED,
                    timestampEpochMillis = 10L,
                    userProvenance = user,
                    relatedImageId = "image-1",
                    relatedImagePath = "normalized.png",
                    overlayArtifactPath = "graph-panel-overlay.png",
                    gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
                ),
                evidence = RoiConfirmationEvidence(
                    relatedImageId = "image-1",
                    relatedImagePath = "normalized.png",
                    overlayArtifactPath = "graph-panel-overlay.png",
                ),
                confirmationStatus = UserConfirmationStatus.CONFIRMED,
                gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
            ),
            plotAreaConfirmation = PlotAreaConfirmation(
                suggestedBounds = plotArea,
                confirmedPlotArea = UserConfirmedPlotArea(
                    bounds = plotArea,
                    parentGraphPanelBounds = graphPanel,
                    source = RoiEditSource.USER_DRAG,
                    confirmationStatus = UserConfirmationStatus.CONFIRMED,
                    timestampEpochMillis = 11L,
                    userProvenance = user,
                    relatedImageId = "image-1",
                    relatedImagePath = "normalized.png",
                    overlayArtifactPath = "plot-area-overlay.png",
                    gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
                ),
                evidence = RoiConfirmationEvidence(
                    relatedImageId = "image-1",
                    relatedImagePath = "normalized.png",
                    overlayArtifactPath = "plot-area-overlay.png",
                ),
                confirmationStatus = UserConfirmationStatus.CONFIRMED,
                gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
            ),
            calibration = confirmedCalibration(
                xAnchorCount = 3,
                yAnchorCount = 3,
                gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
            ),
            trace = UserConfirmedTrace(
                sourceTraceId = "trace-1",
                confirmationStatus = UserConfirmationStatus.CONFIRMED,
                editDecisions = listOf(TraceEditDecision.ACCEPT_AUTO),
                evidence = TraceConfirmationEvidence(
                    sourceTraceId = "trace-1",
                    overlayArtifactPath = "trace-overlay.png",
                    centerlineArtifactPath = "centerline.png",
                    qualityStatus = TraceQualityStatus.VALID,
                    qualitySummary = TraceQualitySummary(
                        pointCount = 200,
                        columnCoverageRatio = 0.9,
                        confidence = 0.95,
                    ),
                ),
                timestampEpochMillis = 13L,
                userProvenance = user,
                gateStatus = TraceGateStatus.USER_CONFIRMED,
            ),
            peaks = UserConfirmedPeakSet(
                peakSetId = "peaks-1",
                decisions = listOf(
                    UserPeakEditDecision(
                        decisionId = "decision-1",
                        action = PeakEditAction.ACCEPT_AUTO,
                        peakId = "peak-1",
                        timestampEpochMillis = 14L,
                        userProvenance = user,
                    ),
                ),
                reportablePeakIds = listOf("peak-1"),
                reviewStatus = PeakReviewStatus.USER_CONFIRMED,
                timestampEpochMillis = 14L,
                userProvenance = user,
                gateStatus = PeakReviewGateStatus.USER_CONFIRMED,
            ),
            autoDiagnosticEvidence = GateEvidence(
                evidencePackageStatus = EvidenceGateStatus.VALID,
                sourceProvenanceStatus = EvidenceGateStatus.VALID,
            ),
        )

    private fun confirmedCalibration(
        xAnchorCount: Int,
        yAnchorCount: Int,
        gateStatus: GuidedWorkflowGateStatus,
    ): UserConfirmedCalibration {
        val anchors = (0 until xAnchorCount).map { index ->
            anchor("x-$index", CalibrationAxis.X, index)
        } + (0 until yAnchorCount).map { index ->
            anchor("y-$index", CalibrationAxis.Y, index)
        }
        return UserConfirmedCalibration(
            calibrationSet = UserCalibrationSet(
                calibrationSetId = "calibration-1",
                anchors = anchors,
                xUnitLabel = "min",
                yUnitLabel = "a.u.",
                residualReports = listOf(
                    residualReport(CalibrationAxis.X, anchors.filter { it.axis == CalibrationAxis.X }),
                    residualReport(CalibrationAxis.Y, anchors.filter { it.axis == CalibrationAxis.Y }),
                ),
                timestampEpochMillis = 12L,
                userProvenance = user,
                gateStatus = gateStatus,
            ),
            confirmationStatus = UserConfirmationStatus.CONFIRMED,
            timestampEpochMillis = 12L,
            userProvenance = user,
            overlayArtifactPath = "calibration-overlay.png",
            gateStatus = gateStatus,
        )
    }

    private fun anchor(
        id: String,
        axis: CalibrationAxis,
        index: Int,
    ): ManualCalibrationAnchor =
        ManualCalibrationAnchor(
            anchorId = id,
            axis = axis,
            pixel = GeometryPoint(
                x = 10f + index * 20f,
                y = if (axis == CalibrationAxis.X) 90f else 80f - index * 20f,
            ),
            value = index.toDouble(),
            unitLabel = if (axis == CalibrationAxis.X) "min" else "a.u.",
            source = CalibrationAnchorSource.USER_CLICK,
            status = CalibrationAnchorStatus.ACCEPTED,
            timestampEpochMillis = 12L,
            userProvenance = user,
            relatedImageId = "image-1",
        )

    private fun residualReport(
        axis: CalibrationAxis,
        anchors: List<ManualCalibrationAnchor>,
    ): CalibrationResidualReport =
        CalibrationResidualReport(
            axis = axis,
            acceptedAnchorIds = anchors.map { it.anchorId },
            residuals = anchors.map {
                CalibrationResidual(it.anchorId, residualPx = 0.1, residualUnit = 0.01)
            },
            maxResidualPx = 0.1,
            rmsePx = 0.1,
            r2 = 0.999,
            monotonicityStatus = CalibrationMonotonicityStatus.VALID,
            gateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
        )

    private companion object {
        val user = GuidedUserProvenance(
            userIdHash = "user-hash",
            sessionId = "session-1",
            deviceIdHash = "device-hash",
            appVersion = "test",
        )
        val graphPanel = GraphRegion(0, 0, 200, 120, "graphPanel")
        val plotArea = GraphRegion(20, 20, 160, 80, "plotArea")
    }
}
