package com.chromalab.feature.processing.guided

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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuidedRoiEditorModelTest {

    @Test
    fun graphPanelBoundsUpdateIsClampedToImage() {
        val snapshot = baseSnapshot()

        val updated = GuidedRoiEditorReducer.updateGraphPanel(
            snapshot,
            GraphRegion(-10, -20, 500, 260, "edited"),
        )

        assertEquals(0, updated.graphPanelBounds?.x)
        assertEquals(0, updated.graphPanelBounds?.y)
        assertEquals(400, updated.graphPanelBounds?.width)
        assertEquals(240, updated.graphPanelBounds?.height)
        assertTrue(updated.graphPanelEdited)
    }

    @Test
    fun plotAreaBoundsUpdateStaysInsideGraphPanel() {
        val snapshot = baseSnapshot()

        val updated = GuidedRoiEditorReducer.updatePlotArea(
            snapshot,
            GraphRegion(0, 0, 500, 500, "plot"),
        )

        assertEquals(snapshot.graphPanelBounds?.x, updated.plotAreaBounds?.x)
        assertEquals(snapshot.graphPanelBounds?.y, updated.plotAreaBounds?.y)
        assertEquals(snapshot.graphPanelBounds?.width, updated.plotAreaBounds?.width)
        assertEquals(snapshot.graphPanelBounds?.height, updated.plotAreaBounds?.height)
        assertTrue(updated.plotAreaEdited)
        assertEquals(GuidedRoiEditorStatus.REVIEW, updated.plotAreaStatus)
    }

    @Test
    fun plotAreaInsideGraphPanelValidationPasses() {
        val validation = GuidedRoiEditorReducer.validatePlotArea(
            GraphRegion(40, 40, 260, 120, "plot"),
            GraphRegion(20, 20, 320, 180, "graph"),
        )

        assertFalse(validation.hasErrors)
    }

    @Test
    fun zeroAreaBoundsAreInvalid() {
        val graphValidation = GuidedRoiEditorReducer.validateGraphPanel(
            GraphRegion(0, 0, 0, 10, "graph"),
            imageWidth = 400,
            imageHeight = 240,
        )
        val plotValidation = GuidedRoiEditorReducer.validatePlotArea(
            GraphRegion(20, 20, 40, 0, "plot"),
            GraphRegion(0, 0, 200, 120, "graph"),
        )

        assertTrue(graphValidation.hasErrors)
        assertTrue(plotValidation.hasErrors)
        assertTrue(graphValidation.issues.any { it.code == "graph_panel.zero_area" })
        assertTrue(plotValidation.issues.any { it.code == "plot_area.zero_area" })
    }

    @Test
    fun resetToSuggestionRestoresSuggestedBounds() {
        val edited = GuidedRoiEditorReducer
            .updateGraphPanel(baseSnapshot(), GraphRegion(10, 10, 100, 90, "edited"))

        val reset = GuidedRoiEditorReducer.resetGraphPanel(edited)

        assertEquals(graphPanelSuggestion, reset.graphPanelBounds)
        assertFalse(reset.graphPanelEdited)
        assertEquals(GuidedRoiEditorStatus.SUGGESTED, reset.graphPanelStatus)
    }

    @Test
    fun graphPanelConfirmationRecordsUserEditedAutoSuggestion() {
        val edited = GuidedRoiEditorReducer.updateGraphPanel(
            baseSnapshot(),
            GraphRegion(24, 22, 316, 178, "edited"),
        )

        val state = GuidedRoiEditorReducer.confirmGraphPanel(
            state = baseState(GuidedDigitizationMode.GUIDED_PRODUCTION),
            snapshot = edited,
            userProvenance = user,
            timestampEpochMillis = 20L,
            overlayArtifactPath = "roi-overlay.png",
        )

        val confirmation = state.graphPanelConfirmation
        assertEquals(GuidedWorkflowStep.GRAPH_PANEL_CONFIRMED, state.currentStep)
        assertEquals(UserConfirmationStatus.CONFIRMED, confirmation?.confirmationStatus)
        assertEquals(RoiEditSource.USER_EDITED_AUTO_SUGGESTION, confirmation?.confirmedGraphPanel?.source)
        assertEquals("roi-overlay.png", confirmation?.evidence?.overlayArtifactPath)
    }

    @Test
    fun plotAreaConfirmationStoresReviewWhenPlotEqualsGraphPanel() {
        val snapshot = baseSnapshot().copy(
            plotAreaBounds = graphPanelSuggestion,
            activeStage = GuidedRoiEditorStage.PLOT_AREA,
        )

        val state = GuidedRoiEditorReducer.confirmPlotArea(
            state = baseState(GuidedDigitizationMode.GUIDED_PRODUCTION),
            snapshot = snapshot,
            userProvenance = user,
            timestampEpochMillis = 30L,
        )

        assertEquals(GuidedWorkflowStep.PLOT_AREA_CONFIRMED, state.currentStep)
        assertEquals(GuidedWorkflowGateStatus.REVIEW_REQUIRED, state.plotAreaConfirmation?.gateStatus)
        assertTrue(
            state.plotAreaConfirmation?.warnings?.contains("plot_area.equals_graph_panel") == true,
        )
    }

    @Test
    fun autoDiagnosticConfirmationsDoNotBecomeReleaseEvidence() {
        val snapshot = baseSnapshot()
        val stateWithGraph = GuidedRoiEditorReducer.confirmGraphPanel(
            state = baseState(GuidedDigitizationMode.AUTO_DIAGNOSTIC),
            snapshot = snapshot,
            userProvenance = user,
            timestampEpochMillis = 40L,
        )
        val stateWithPlot = GuidedRoiEditorReducer.confirmPlotArea(
            state = stateWithGraph,
            snapshot = snapshot,
            userProvenance = user,
            timestampEpochMillis = 41L,
        )

        val gate = GuidedReportGateMapper.evaluate(stateWithPlot)

        assertEquals(ReportGateStatus.DIAGNOSTIC_ONLY, gate.status)
        assertEquals(EvidenceGateStatus.MISSING, gate.evidence.graphPanelStatus)
        assertEquals(EvidenceGateStatus.NOT_APPLICABLE, gate.evidence.userConfirmationStatus)
    }

    @Test
    fun editorSnapshotSerializesForStateRestoration() {
        val snapshot = GuidedRoiEditorReducer.updatePlotArea(
            baseSnapshot(),
            GraphRegion(50, 50, 200, 90, "plot"),
        )
        val json = Json { encodeDefaults = true }

        val decoded = json.decodeFromString<GuidedRoiEditorSnapshot>(
            json.encodeToString(snapshot),
        )

        assertEquals(snapshot.imageWidth, decoded.imageWidth)
        assertEquals(snapshot.plotAreaBounds, decoded.plotAreaBounds)
        assertTrue(decoded.plotAreaEdited)
    }

    @Test
    fun confirmationRejectsInvalidPlotArea() {
        val snapshot = baseSnapshot().copy(plotAreaBounds = null)

        assertFailsWith<IllegalArgumentException> {
            GuidedRoiEditorReducer.confirmPlotArea(
                state = baseState(GuidedDigitizationMode.GUIDED_PRODUCTION),
                snapshot = snapshot,
                userProvenance = user,
                timestampEpochMillis = 50L,
            )
        }
    }

    private fun baseSnapshot(): GuidedRoiEditorSnapshot =
        GuidedRoiEditorReducer.initialSnapshot(
            imageWidth = 400,
            imageHeight = 240,
            graphPanelSuggestion = graphPanelSuggestion,
            plotAreaSuggestion = plotAreaSuggestion,
        )

    private fun baseState(mode: GuidedDigitizationMode): GuidedDigitizationState =
        GuidedDigitizationState(
            stateId = "guided-roi-state",
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
        val graphPanelSuggestion = GraphRegion(20, 20, 320, 180, "graphPanel")
        val plotAreaSuggestion = GraphRegion(55, 55, 240, 100, "plotArea")
        val user = GuidedUserProvenance(
            userIdHash = "user-hash",
            sessionId = "session-1",
            deviceIdHash = "device-hash",
            appVersion = "test",
        )
    }
}
