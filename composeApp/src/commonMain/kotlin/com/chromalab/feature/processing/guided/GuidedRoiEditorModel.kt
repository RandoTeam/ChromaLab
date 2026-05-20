package com.chromalab.feature.processing.guided

import com.chromalab.feature.processing.graph.GraphRegion
import kotlinx.serialization.Serializable

private const val MIN_ROI_DIMENSION_PX = 1
private const val NEAR_EDGE_MARGIN_RATIO = 0.03f
private const val FULL_PANEL_EQUALITY_TOLERANCE_PX = 2

@Serializable
enum class GuidedRoiEditorStage {
    GRAPH_PANEL,
    PLOT_AREA,
}

@Serializable
enum class GuidedRoiEditorStatus {
    SUGGESTED,
    USER_CONFIRMED,
    REVIEW,
    INVALID,
}

@Serializable
enum class GuidedRoiValidationSeverity {
    INFO,
    WARNING,
    ERROR,
}

@Serializable
data class GuidedRoiValidationIssue(
    val code: String,
    val severity: GuidedRoiValidationSeverity,
    val message: String,
)

@Serializable
data class GuidedRoiValidationResult(
    val issues: List<GuidedRoiValidationIssue> = emptyList(),
) {
    val hasErrors: Boolean
        get() = issues.any { it.severity == GuidedRoiValidationSeverity.ERROR }

    val hasWarnings: Boolean
        get() = issues.any { it.severity == GuidedRoiValidationSeverity.WARNING }

    val warningCodes: List<String>
        get() = issues
            .filter { it.severity == GuidedRoiValidationSeverity.WARNING }
            .map { it.code }

    fun gateStatus(): GuidedWorkflowGateStatus =
        when {
            hasErrors -> GuidedWorkflowGateStatus.INVALID
            hasWarnings -> GuidedWorkflowGateStatus.REVIEW_REQUIRED
            else -> GuidedWorkflowGateStatus.USER_CONFIRMED
        }

    fun editorStatus(): GuidedRoiEditorStatus =
        when {
            hasErrors -> GuidedRoiEditorStatus.INVALID
            hasWarnings -> GuidedRoiEditorStatus.REVIEW
            else -> GuidedRoiEditorStatus.USER_CONFIRMED
        }
}

@Serializable
data class GuidedRoiEditorSnapshot(
    val imageWidth: Int,
    val imageHeight: Int,
    val activeStage: GuidedRoiEditorStage = GuidedRoiEditorStage.GRAPH_PANEL,
    val graphPanelSuggestion: GraphRegion? = null,
    val plotAreaSuggestion: GraphRegion? = null,
    val graphPanelBounds: GraphRegion? = graphPanelSuggestion,
    val plotAreaBounds: GraphRegion? = plotAreaSuggestion,
    val graphPanelEdited: Boolean = false,
    val plotAreaEdited: Boolean = false,
    val graphPanelStatus: GuidedRoiEditorStatus = if (graphPanelSuggestion != null) {
        GuidedRoiEditorStatus.SUGGESTED
    } else {
        GuidedRoiEditorStatus.INVALID
    },
    val plotAreaStatus: GuidedRoiEditorStatus = if (plotAreaSuggestion != null) {
        GuidedRoiEditorStatus.SUGGESTED
    } else {
        GuidedRoiEditorStatus.INVALID
    },
) {
    val graphPanelValidation: GuidedRoiValidationResult
        get() = GuidedRoiEditorReducer.validateGraphPanel(graphPanelBounds, imageWidth, imageHeight)

    val plotAreaValidation: GuidedRoiValidationResult
        get() = GuidedRoiEditorReducer.validatePlotArea(plotAreaBounds, graphPanelBounds)

    val canConfirmGraphPanel: Boolean
        get() = !graphPanelValidation.hasErrors

    val canConfirmPlotArea: Boolean
        get() = !plotAreaValidation.hasErrors
}

object GuidedRoiEditorReducer {
    fun initialSnapshot(
        imageWidth: Int,
        imageHeight: Int,
        graphPanelSuggestion: GraphRegion?,
        plotAreaSuggestion: GraphRegion?,
    ): GuidedRoiEditorSnapshot {
        val clampedGraphPanel = graphPanelSuggestion?.coerceInsideImage(imageWidth, imageHeight)
        val clampedPlotArea = plotAreaSuggestion
            ?.coerceInsideImage(imageWidth, imageHeight)
            ?.let { plotArea ->
                if (clampedGraphPanel != null) plotArea.coerceInside(clampedGraphPanel) else plotArea
            }

        return GuidedRoiEditorSnapshot(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            graphPanelSuggestion = clampedGraphPanel,
            plotAreaSuggestion = clampedPlotArea,
            graphPanelBounds = clampedGraphPanel,
            plotAreaBounds = clampedPlotArea,
            graphPanelStatus = statusFor(validateGraphPanel(clampedGraphPanel, imageWidth, imageHeight), clampedGraphPanel),
            plotAreaStatus = statusFor(validatePlotArea(clampedPlotArea, clampedGraphPanel), clampedPlotArea),
        )
    }

    fun updateGraphPanel(
        snapshot: GuidedRoiEditorSnapshot,
        bounds: GraphRegion,
    ): GuidedRoiEditorSnapshot {
        val graphPanel = bounds.coerceInsideImage(snapshot.imageWidth, snapshot.imageHeight)
        val plotArea = snapshot.plotAreaBounds?.coerceInside(graphPanel)
        val graphValidation = validateGraphPanel(graphPanel, snapshot.imageWidth, snapshot.imageHeight)
        val plotValidation = validatePlotArea(plotArea, graphPanel)

        return snapshot.copy(
            activeStage = GuidedRoiEditorStage.GRAPH_PANEL,
            graphPanelBounds = graphPanel,
            plotAreaBounds = plotArea,
            graphPanelEdited = true,
            graphPanelStatus = graphValidation.editorStatus(),
            plotAreaStatus = statusFor(plotValidation, plotArea),
        )
    }

    fun updatePlotArea(
        snapshot: GuidedRoiEditorSnapshot,
        bounds: GraphRegion,
    ): GuidedRoiEditorSnapshot {
        val graphPanel = snapshot.graphPanelBounds
        val plotArea = if (graphPanel != null) {
            bounds.coerceInside(graphPanel)
        } else {
            bounds.coerceInsideImage(snapshot.imageWidth, snapshot.imageHeight)
        }
        val plotValidation = validatePlotArea(plotArea, graphPanel)

        return snapshot.copy(
            activeStage = GuidedRoiEditorStage.PLOT_AREA,
            plotAreaBounds = plotArea,
            plotAreaEdited = true,
            plotAreaStatus = plotValidation.editorStatus(),
        )
    }

    fun resetGraphPanel(snapshot: GuidedRoiEditorSnapshot): GuidedRoiEditorSnapshot {
        val graphPanel = snapshot.graphPanelSuggestion
        val plotArea = snapshot.plotAreaBounds?.let { current ->
            if (graphPanel != null) current.coerceInside(graphPanel) else current
        }
        val graphValidation = validateGraphPanel(graphPanel, snapshot.imageWidth, snapshot.imageHeight)
        val plotValidation = validatePlotArea(plotArea, graphPanel)

        return snapshot.copy(
            activeStage = GuidedRoiEditorStage.GRAPH_PANEL,
            graphPanelBounds = graphPanel,
            plotAreaBounds = plotArea,
            graphPanelEdited = false,
            graphPanelStatus = statusFor(graphValidation, graphPanel, suggested = true),
            plotAreaStatus = statusFor(plotValidation, plotArea),
        )
    }

    fun resetPlotArea(snapshot: GuidedRoiEditorSnapshot): GuidedRoiEditorSnapshot {
        val plotArea = snapshot.plotAreaSuggestion?.let { suggestion ->
            snapshot.graphPanelBounds?.let { suggestion.coerceInside(it) } ?: suggestion
        }
        val plotValidation = validatePlotArea(plotArea, snapshot.graphPanelBounds)

        return snapshot.copy(
            activeStage = GuidedRoiEditorStage.PLOT_AREA,
            plotAreaBounds = plotArea,
            plotAreaEdited = false,
            plotAreaStatus = statusFor(plotValidation, plotArea, suggested = true),
        )
    }

    fun confirmGraphPanel(
        state: GuidedDigitizationState,
        snapshot: GuidedRoiEditorSnapshot,
        userProvenance: GuidedUserProvenance,
        timestampEpochMillis: Long,
        overlayArtifactPath: String? = null,
    ): GuidedDigitizationState {
        val graphPanel = requireNotNull(snapshot.graphPanelBounds) {
            "GraphPanel confirmation requires bounds"
        }
        val validation = validateGraphPanel(graphPanel, snapshot.imageWidth, snapshot.imageHeight)
        require(!validation.hasErrors) {
            "GraphPanel confirmation has blocking validation errors: ${validation.issues.map { it.code }}"
        }

        val image = requireNotNull(state.image) {
            "GraphPanel confirmation requires guided image reference"
        }
        val source = roiSource(snapshot.graphPanelEdited, snapshot.graphPanelSuggestion)
        val gateStatus = validation.gateStatus()
        val confirmed = UserConfirmedGraphPanel(
            bounds = graphPanel.copy(label = "graphPanel"),
            source = source,
            confirmationStatus = UserConfirmationStatus.CONFIRMED,
            timestampEpochMillis = timestampEpochMillis,
            userProvenance = userProvenance,
            relatedImageId = image.imageId,
            relatedImagePath = image.normalizedImagePath ?: image.originalImagePath,
            overlayArtifactPath = overlayArtifactPath,
            validationWarnings = validation.warningCodes,
            gateStatus = gateStatus,
        )
        val evidence = RoiConfirmationEvidence(
            relatedImageId = image.imageId,
            relatedImagePath = image.normalizedImagePath ?: image.originalImagePath,
            overlayArtifactPath = overlayArtifactPath,
            warnings = validation.warningCodes,
        )

        return state.copy(
            currentStep = GuidedWorkflowStep.GRAPH_PANEL_CONFIRMED,
            stepStatuses = state.stepStatuses + mapOf(
                GuidedWorkflowStep.GRAPH_PANEL_SUGGESTED to GuidedStepStatus.SUGGESTED,
                GuidedWorkflowStep.GRAPH_PANEL_CONFIRMED to if (gateStatus == GuidedWorkflowGateStatus.REVIEW_REQUIRED) {
                    GuidedStepStatus.REVIEW_REQUIRED
                } else {
                    GuidedStepStatus.CONFIRMED
                },
                GuidedWorkflowStep.PLOT_AREA_SUGGESTED to GuidedStepStatus.SUGGESTED,
            ),
            graphPanelConfirmation = GraphPanelConfirmation(
                suggestedBounds = snapshot.graphPanelSuggestion,
                confirmedGraphPanel = confirmed,
                evidence = evidence,
                confirmationStatus = UserConfirmationStatus.CONFIRMED,
                gateStatus = gateStatus,
                warnings = validation.warningCodes,
            ),
            updatedAtEpochMillis = timestampEpochMillis,
            auditTrail = state.auditTrail + GuidedWorkflowAuditEntry(
                timestampEpochMillis = timestampEpochMillis,
                step = GuidedWorkflowStep.GRAPH_PANEL_CONFIRMED,
                action = "graph_panel_confirmed:${source.name.lowercase()}",
                actor = "user",
                details = validation.warningCodes.takeIf { it.isNotEmpty() }?.joinToString(","),
            ),
        )
    }

    fun confirmPlotArea(
        state: GuidedDigitizationState,
        snapshot: GuidedRoiEditorSnapshot,
        userProvenance: GuidedUserProvenance,
        timestampEpochMillis: Long,
        overlayArtifactPath: String? = null,
    ): GuidedDigitizationState {
        val graphPanel = requireNotNull(snapshot.graphPanelBounds) {
            "PlotArea confirmation requires graphPanel bounds"
        }
        val plotArea = requireNotNull(snapshot.plotAreaBounds) {
            "PlotArea confirmation requires bounds"
        }
        val validation = validatePlotArea(plotArea, graphPanel)
        require(!validation.hasErrors) {
            "PlotArea confirmation has blocking validation errors: ${validation.issues.map { it.code }}"
        }

        val image = requireNotNull(state.image) {
            "PlotArea confirmation requires guided image reference"
        }
        val source = roiSource(snapshot.plotAreaEdited, snapshot.plotAreaSuggestion)
        val gateStatus = validation.gateStatus()
        val confirmed = UserConfirmedPlotArea(
            bounds = plotArea.copy(label = "plotArea"),
            parentGraphPanelBounds = graphPanel.copy(label = "graphPanel"),
            source = source,
            confirmationStatus = UserConfirmationStatus.CONFIRMED,
            timestampEpochMillis = timestampEpochMillis,
            userProvenance = userProvenance,
            relatedImageId = image.imageId,
            relatedImagePath = image.normalizedImagePath ?: image.originalImagePath,
            overlayArtifactPath = overlayArtifactPath,
            validationWarnings = validation.warningCodes,
            gateStatus = gateStatus,
        )
        val evidence = RoiConfirmationEvidence(
            relatedImageId = image.imageId,
            relatedImagePath = image.normalizedImagePath ?: image.originalImagePath,
            overlayArtifactPath = overlayArtifactPath,
            warnings = validation.warningCodes,
        )

        return state.copy(
            currentStep = GuidedWorkflowStep.PLOT_AREA_CONFIRMED,
            stepStatuses = state.stepStatuses + mapOf(
                GuidedWorkflowStep.PLOT_AREA_SUGGESTED to GuidedStepStatus.SUGGESTED,
                GuidedWorkflowStep.PLOT_AREA_CONFIRMED to if (gateStatus == GuidedWorkflowGateStatus.REVIEW_REQUIRED) {
                    GuidedStepStatus.REVIEW_REQUIRED
                } else {
                    GuidedStepStatus.CONFIRMED
                },
            ),
            plotAreaConfirmation = PlotAreaConfirmation(
                suggestedBounds = snapshot.plotAreaSuggestion,
                confirmedPlotArea = confirmed,
                evidence = evidence,
                confirmationStatus = UserConfirmationStatus.CONFIRMED,
                gateStatus = gateStatus,
                warnings = validation.warningCodes,
            ),
            updatedAtEpochMillis = timestampEpochMillis,
            auditTrail = state.auditTrail + GuidedWorkflowAuditEntry(
                timestampEpochMillis = timestampEpochMillis,
                step = GuidedWorkflowStep.PLOT_AREA_CONFIRMED,
                action = "plot_area_confirmed:${source.name.lowercase()}",
                actor = "user",
                details = validation.warningCodes.takeIf { it.isNotEmpty() }?.joinToString(","),
            ),
        )
    }

    fun validateGraphPanel(
        graphPanel: GraphRegion?,
        imageWidth: Int,
        imageHeight: Int,
    ): GuidedRoiValidationResult {
        val issues = mutableListOf<GuidedRoiValidationIssue>()
        if (graphPanel == null) {
            return GuidedRoiValidationResult(
                listOf(
                    error("graph_panel.missing", "Graph panel bounds are missing."),
                ),
            )
        }
        if (graphPanel.width < MIN_ROI_DIMENSION_PX || graphPanel.height < MIN_ROI_DIMENSION_PX) {
            issues += error("graph_panel.zero_area", "Graph panel must have non-zero width and height.")
        }
        if (graphPanel.x < 0 || graphPanel.y < 0 || graphPanel.right > imageWidth || graphPanel.bottom > imageHeight) {
            issues += error("graph_panel.outside_image", "Graph panel must stay inside the normalized image.")
        }
        return GuidedRoiValidationResult(issues)
    }

    fun validatePlotArea(
        plotArea: GraphRegion?,
        graphPanel: GraphRegion?,
    ): GuidedRoiValidationResult {
        val issues = mutableListOf<GuidedRoiValidationIssue>()
        if (plotArea == null) {
            return GuidedRoiValidationResult(
                listOf(
                    error("plot_area.missing", "Plot area bounds are missing."),
                ),
            )
        }
        if (graphPanel == null) {
            issues += error("plot_area.graph_panel_missing", "Plot area requires a confirmed graph panel.")
        }
        if (plotArea.width < MIN_ROI_DIMENSION_PX || plotArea.height < MIN_ROI_DIMENSION_PX) {
            issues += error("plot_area.zero_area", "Plot area must have non-zero width and height.")
        }
        if (graphPanel != null && !plotArea.isInside(graphPanel)) {
            issues += error("plot_area.outside_graph_panel", "Plot area must stay inside the graph panel.")
        }
        if (graphPanel != null && plotArea.nearlyEquals(graphPanel)) {
            issues += warning("plot_area.equals_graph_panel", "Plot area equals the whole graph panel and may include labels.")
        }
        if (graphPanel != null && plotArea.isNearGraphPanelEdge(graphPanel)) {
            issues += warning("plot_area.near_graph_panel_edge", "Plot area is close to a graph panel edge; tick labels or axes may be cut.")
        }
        return GuidedRoiValidationResult(issues)
    }

    private fun statusFor(
        validation: GuidedRoiValidationResult,
        bounds: GraphRegion?,
        suggested: Boolean = false,
    ): GuidedRoiEditorStatus =
        when {
            bounds == null || validation.hasErrors -> GuidedRoiEditorStatus.INVALID
            validation.hasWarnings -> GuidedRoiEditorStatus.REVIEW
            suggested -> GuidedRoiEditorStatus.SUGGESTED
            else -> GuidedRoiEditorStatus.USER_CONFIRMED
        }

    private fun roiSource(
        edited: Boolean,
        suggestion: GraphRegion?,
    ): RoiEditSource =
        when {
            suggestion == null -> RoiEditSource.MANUAL
            edited -> RoiEditSource.USER_EDITED_AUTO_SUGGESTION
            else -> RoiEditSource.USER_CONFIRMED
        }

    private fun error(code: String, message: String): GuidedRoiValidationIssue =
        GuidedRoiValidationIssue(code, GuidedRoiValidationSeverity.ERROR, message)

    private fun warning(code: String, message: String): GuidedRoiValidationIssue =
        GuidedRoiValidationIssue(code, GuidedRoiValidationSeverity.WARNING, message)
}

fun GraphRegion.isInside(parent: GraphRegion): Boolean =
    x >= parent.x &&
        y >= parent.y &&
        right <= parent.right &&
        bottom <= parent.bottom

fun GraphRegion.coerceInsideImage(imageWidth: Int, imageHeight: Int): GraphRegion {
    val safeImageWidth = imageWidth.coerceAtLeast(MIN_ROI_DIMENSION_PX)
    val safeImageHeight = imageHeight.coerceAtLeast(MIN_ROI_DIMENSION_PX)
    val safeWidth = width.coerceAtLeast(MIN_ROI_DIMENSION_PX).coerceAtMost(safeImageWidth)
    val safeHeight = height.coerceAtLeast(MIN_ROI_DIMENSION_PX).coerceAtMost(safeImageHeight)
    val safeX = x.coerceIn(0, safeImageWidth - safeWidth)
    val safeY = y.coerceIn(0, safeImageHeight - safeHeight)
    return copy(x = safeX, y = safeY, width = safeWidth, height = safeHeight)
}

fun GraphRegion.coerceInside(parent: GraphRegion): GraphRegion {
    val safeWidth = width.coerceAtLeast(MIN_ROI_DIMENSION_PX).coerceAtMost(parent.width.coerceAtLeast(MIN_ROI_DIMENSION_PX))
    val safeHeight = height.coerceAtLeast(MIN_ROI_DIMENSION_PX).coerceAtMost(parent.height.coerceAtLeast(MIN_ROI_DIMENSION_PX))
    val safeX = x.coerceIn(parent.x, parent.right - safeWidth)
    val safeY = y.coerceIn(parent.y, parent.bottom - safeHeight)
    return copy(x = safeX, y = safeY, width = safeWidth, height = safeHeight)
}

private fun GraphRegion.nearlyEquals(other: GraphRegion): Boolean =
    kotlin.math.abs(x - other.x) <= FULL_PANEL_EQUALITY_TOLERANCE_PX &&
        kotlin.math.abs(y - other.y) <= FULL_PANEL_EQUALITY_TOLERANCE_PX &&
        kotlin.math.abs(width - other.width) <= FULL_PANEL_EQUALITY_TOLERANCE_PX &&
        kotlin.math.abs(height - other.height) <= FULL_PANEL_EQUALITY_TOLERANCE_PX

private fun GraphRegion.isNearGraphPanelEdge(graphPanel: GraphRegion): Boolean {
    val margin = (minOf(graphPanel.width, graphPanel.height) * NEAR_EDGE_MARGIN_RATIO)
        .toInt()
        .coerceAtLeast(4)
    return x - graphPanel.x <= margin ||
        y - graphPanel.y <= margin ||
        graphPanel.right - right <= margin ||
        graphPanel.bottom - bottom <= margin
}
