package com.chromalab.feature.processing.guided

import com.chromalab.feature.processing.geometry.GeometryPoint
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.reports.EvidenceGateStatus
import com.chromalab.feature.reports.GateEvidence
import com.chromalab.feature.reports.PeakBoundaryEvidence
import com.chromalab.feature.reports.PeakEvidence
import com.chromalab.feature.reports.PeakEvidenceStatus
import com.chromalab.feature.reports.PeakGateStatus
import com.chromalab.feature.reports.PeakMetricEvidence
import com.chromalab.feature.reports.PeakMetricEvidenceStatus
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
        assertEquals(ProcessingMode.AUTONOMOUS_PRODUCTION, GuidedDigitizationMode.AUTONOMOUS_PRODUCTION.toProcessingMode())
        assertEquals(ProcessingMode.AUTO_DIAGNOSTIC, GuidedDigitizationMode.AUTO_DIAGNOSTIC.toProcessingMode())
        assertEquals(ProcessingMode.ASSISTED_REVIEW, GuidedDigitizationMode.ASSISTED_REVIEW.toProcessingMode())
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
        assertEquals(GuidedDigitizationMode.ASSISTED_REVIEW, decoded.mode)
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
    fun assistedReviewStateMapsToReleaseReadyWithExplicitUserConfirmedEvidence() {
        val gate = GuidedReportGateMapper.evaluate(
            releaseReadyGuidedState().copy(mode = GuidedDigitizationMode.ASSISTED_REVIEW),
        )

        assertEquals(ReportGateStatus.RELEASE_READY, gate.status)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.traceStatus)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.userConfirmationStatus)
    }

    @Test
    fun guidedProductionCompatibilityAliasStillMapsToAssistedReviewBehavior() {
        val gate = GuidedReportGateMapper.evaluate(
            releaseReadyGuidedState().copy(mode = GuidedDigitizationMode.GUIDED_PRODUCTION),
        )

        assertEquals(ReportGateStatus.RELEASE_READY, gate.status)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.traceStatus)
    }

    @Test
    fun autonomousProductionCanSatisfyTraceGateWithAutoValidEvidence() {
        val state = GuidedTraceOverlayReducer.acceptAutonomousTrace(
            state = releaseReadyAutonomousState().copy(peaks = autoValidPeakSet()),
            snapshot = validTraceSnapshot(),
            timestampEpochMillis = 30L,
        )

        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(ReportGateStatus.RELEASE_READY, gate.status)
        assertEquals(EvidenceGateStatus.VALID, gate.evidence.traceStatus)
        assertEquals(EvidenceGateStatus.NOT_APPLICABLE, gate.evidence.userConfirmationStatus)
    }

    @Test
    fun autonomousProductionCanSatisfyPeakGateWithAutoValidEvidence() {
        val gate = GuidedReportGateMapper.evaluate(
            releaseReadyAutonomousState().copy(
                trace = autoValidTrace(),
                peaks = autoValidPeakSet(),
            ),
        )

        assertEquals(ReportGateStatus.RELEASE_READY, gate.status)
        assertEquals(EvidenceGateStatus.VALID, gate.evidence.peakReviewStatus)
        assertEquals(EvidenceGateStatus.NOT_APPLICABLE, gate.evidence.userConfirmationStatus)
    }

    @Test
    fun assistedReviewPeakConfirmationIsExplicitUserEvidence() {
        val gate = GuidedReportGateMapper.evaluate(releaseReadyGuidedState())

        assertEquals(ReportGateStatus.RELEASE_READY, gate.status)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.peakReviewStatus)
        assertEquals(EvidenceGateStatus.USER_CONFIRMED, gate.evidence.userConfirmationStatus)
    }

    @Test
    fun userRejectedPeakSetBlocksAssistedRelease() {
        val state = releaseReadyGuidedState().copy(
            peaks = UserConfirmedPeakSet(
                peakSetId = "peaks-rejected",
                decisions = listOf(
                    UserPeakEditDecision(
                        decisionId = "decision-reject",
                        action = PeakEditAction.REMOVE,
                        peakId = "peak-1",
                        timestampEpochMillis = 15L,
                        userProvenance = user,
                    ),
                ),
                rejectedPeakIds = listOf("peak-1"),
                reviewStatus = PeakReviewStatus.USER_REJECTED,
                timestampEpochMillis = 15L,
                userProvenance = user,
                gateStatus = PeakReviewGateStatus.INVALID,
            ),
        )

        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(ReportGateStatus.DIAGNOSTIC_ONLY, gate.status)
        assertEquals(EvidenceGateStatus.INVALID, gate.evidence.peakReviewStatus)
        assertTrue(gate.blockingReasons.contains("peak_review.invalid"))
    }

    @Test
    fun autoDiagnosticCannotUseAutoValidTraceAsReleaseEvidence() {
        val state = releaseReadyAutonomousState().copy(
            mode = GuidedDigitizationMode.AUTO_DIAGNOSTIC,
            trace = autoValidTrace(),
            autoDiagnosticEvidence = GateEvidence(
                graphPanelStatus = EvidenceGateStatus.VALID,
                plotAreaStatus = EvidenceGateStatus.VALID,
                xCalibrationStatus = EvidenceGateStatus.VALID,
                yCalibrationStatus = EvidenceGateStatus.VALID,
                traceStatus = EvidenceGateStatus.MISSING,
                peakReviewStatus = EvidenceGateStatus.VALID,
                evidencePackageStatus = EvidenceGateStatus.VALID,
                sourceProvenanceStatus = EvidenceGateStatus.VALID,
            ),
        )

        val gate = GuidedReportGateMapper.evaluate(state)

        assertEquals(ReportGateStatus.DIAGNOSTIC_ONLY, gate.status)
        assertEquals(EvidenceGateStatus.MISSING, gate.evidence.traceStatus)
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
            mode = GuidedDigitizationMode.ASSISTED_REVIEW,
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
                peakEvidence = listOf(autoValidPeakEvidence().copy(status = PeakEvidenceStatus.USER_CONFIRMED)),
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

    private fun releaseReadyAutonomousState(): GuidedDigitizationState =
        GuidedDigitizationState(
            stateId = "autonomous-state-1",
            mode = GuidedDigitizationMode.AUTONOMOUS_PRODUCTION,
            image = GuidedImageReference(
                imageId = "image-1",
                originalImagePath = "original.png",
                normalizedImagePath = "normalized.png",
            ),
            autoDiagnosticEvidence = GateEvidence(
                graphPanelStatus = EvidenceGateStatus.VALID,
                plotAreaStatus = EvidenceGateStatus.VALID,
                xCalibrationStatus = EvidenceGateStatus.VALID,
                yCalibrationStatus = EvidenceGateStatus.VALID,
                traceStatus = EvidenceGateStatus.MISSING,
                evidencePackageStatus = EvidenceGateStatus.VALID,
                sourceProvenanceStatus = EvidenceGateStatus.VALID,
            ),
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )

    private fun validTraceSnapshot(): TraceOverlayEditorSnapshot =
        GuidedTraceOverlayReducer.initialSnapshot(
            imageWidth = 200,
            imageHeight = 120,
            graphPanelBounds = graphPanel,
            plotAreaBounds = plotArea,
            sourceTraceId = "trace-auto",
            tracePoints = (0 until 80).map { index ->
                TraceOverlayPoint(
                    x = 20f + index * 1.5f,
                    y = 60f + (index % 4) * 0.25f,
                    confidence = 0.96f,
                )
            },
            qualitySummary = TraceQualitySummary(
                pointCount = 80,
                columnCoverageRatio = 0.86,
                maxGapColumns = 2,
                componentCount = 1,
                branchPointCount = 0,
                selectedComponentCoverage = 0.86,
                textContaminationScore = 0.02,
                baselineTouchRatio = 0.03,
                frameTouchRatio = 0.02,
                traceConfidence = 0.96,
                confidence = 0.96,
            ),
            calibratedTraceRequired = false,
            overlayArtifactPath = "trace-overlay.png",
            maskArtifactPath = "mask.png",
            centerlineArtifactPath = "centerline.png",
        )

    private fun autoValidTrace(): UserConfirmedTrace =
        UserConfirmedTrace(
            sourceTraceId = "trace-auto",
            confirmationStatus = UserConfirmationStatus.NOT_REQUIRED,
            editDecisions = listOf(TraceEditDecision.ACCEPT_AUTO),
            evidence = TraceConfirmationEvidence(
                sourceTraceId = "trace-auto",
                overlayArtifactPath = "trace-overlay.png",
                centerlineArtifactPath = "centerline.png",
                qualityStatus = TraceQualityStatus.VALID,
                qualitySummary = TraceQualitySummary(pointCount = 80, columnCoverageRatio = 0.86, confidence = 0.96),
                source = TraceOverlaySource.AUTO_EXTRACTED,
                tracePoints = validTraceSnapshot().tracePoints,
                plotAreaBounds = plotArea,
            ),
            timestampEpochMillis = 30L,
            userProvenance = GuidedUserProvenance(sessionId = "autonomous"),
            gateStatus = TraceGateStatus.AUTO_VALID,
            source = TraceOverlaySource.AUTO_EXTRACTED,
            traceConfirmationStatus = TraceConfirmationStatus.AUTO_SUGGESTED,
        )

    private fun autoValidPeakSet(): UserConfirmedPeakSet =
        UserConfirmedPeakSet(
            peakSetId = "auto-peaks-1",
            reportablePeakIds = listOf("peak-1"),
            peakEvidence = listOf(autoValidPeakEvidence()),
            reviewStatus = PeakReviewStatus.AUTO_VALID,
            timestampEpochMillis = 31L,
            userProvenance = GuidedUserProvenance(sessionId = "autonomous"),
            gateStatus = PeakReviewGateStatus.AUTO_VALID,
        )

    private fun autoValidPeakEvidence(): PeakEvidence =
        PeakEvidence(
            evidenceId = "peak-evidence-1",
            peakId = "peak-1",
            peakNumber = 1,
            status = PeakEvidenceStatus.AUTO_VALID,
            gateStatus = PeakGateStatus.VALID,
            retentionTime = PeakMetricEvidence.calculated(5.0, "min"),
            apexPointIndex = 42,
            localMaximumEvidence = true,
            height = PeakMetricEvidence.calculated(120.0, "a.u."),
            area = PeakMetricEvidence.calculated(300.0),
            boundaryEvidence = PeakBoundaryEvidence(
                startRetentionTime = PeakMetricEvidence.calculated(4.8, "min"),
                endRetentionTime = PeakMetricEvidence.calculated(5.2, "min"),
                status = PeakMetricEvidenceStatus.CALCULATED,
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
