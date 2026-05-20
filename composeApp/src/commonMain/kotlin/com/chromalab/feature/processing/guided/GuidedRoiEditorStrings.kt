package com.chromalab.feature.processing.guided

import androidx.compose.runtime.Composable
import com.chromalab.core.common.AppLanguage
import com.chromalab.core.common.Strings

data class GuidedRoiEditorStrings(
    val title: String,
    val graphPanel: String,
    val plotArea: String,
    val suggested: String,
    val confirmed: String,
    val review: String,
    val invalid: String,
    val confirmGraphPanel: String,
    val confirmPlotArea: String,
    val reset: String,
    val graphPanelHint: String,
    val plotAreaHint: String,
    val nextCalibrationAnchors: String,
    val nextDisabledReason: String,
    val zoomHint: String,
    val moveSelection: String,
    val resizeSelection: String,
    val imageDescription: String,
    val back: String,
)

@Composable
fun guidedRoiEditorStrings(): GuidedRoiEditorStrings =
    if (Strings.language == AppLanguage.EN) {
        GuidedRoiEditorStrings(
            title = "Guided graph area",
            graphPanel = "Graph panel",
            plotArea = "Plot area",
            suggested = "Suggested",
            confirmed = "User confirmed",
            review = "Review",
            invalid = "Invalid",
            confirmGraphPanel = "Confirm graph panel",
            confirmPlotArea = "Confirm plot area",
            reset = "Reset",
            graphPanelHint = "Include title, ion/channel, axes, tick labels, and plot frame.",
            plotAreaHint = "Keep only the coordinate rectangle with trace, grid, and frame.",
            nextCalibrationAnchors = "Next: calibration anchors",
            nextDisabledReason = "Available in Phase 3 after plot area confirmation.",
            zoomHint = "Pinch to zoom. Drag the rectangle or handles to adjust bounds.",
            moveSelection = "Move selected rectangle",
            resizeSelection = "Resize selected rectangle",
            imageDescription = "Normalized chromatogram image",
            back = "Back",
        )
    } else {
        GuidedRoiEditorStrings(
            title = "\u041E\u0431\u043B\u0430\u0441\u0442\u0438 \u0433\u0440\u0430\u0444\u0438\u043A\u0430",
            graphPanel = "\u041F\u0430\u043D\u0435\u043B\u044C \u0433\u0440\u0430\u0444\u0438\u043A\u0430",
            plotArea = "\u041E\u0431\u043B\u0430\u0441\u0442\u044C \u043F\u043E\u0441\u0442\u0440\u043E\u0435\u043D\u0438\u044F",
            suggested = "\u041F\u0440\u0435\u0434\u043B\u043E\u0436\u0435\u043D\u043E",
            confirmed = "\u041F\u043E\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043D\u043E",
            review = "\u041D\u0443\u0436\u043D\u0430 \u043F\u0440\u043E\u0432\u0435\u0440\u043A\u0430",
            invalid = "\u041E\u0448\u0438\u0431\u043A\u0430",
            confirmGraphPanel = "\u041F\u043E\u0434\u0442\u0432\u0435\u0440\u0434\u0438\u0442\u044C \u043F\u0430\u043D\u0435\u043B\u044C",
            confirmPlotArea = "\u041F\u043E\u0434\u0442\u0432\u0435\u0440\u0434\u0438\u0442\u044C plotArea",
            reset = "\u0421\u0431\u0440\u043E\u0441",
            graphPanelHint = "\u0412\u043A\u043B\u044E\u0447\u0438\u0442\u0435 title, ion/channel, \u043E\u0441\u0438, tick labels \u0438 \u0440\u0430\u043C\u043A\u0443.",
            plotAreaHint = "\u041E\u0441\u0442\u0430\u0432\u044C\u0442\u0435 \u0442\u043E\u043B\u044C\u043A\u043E \u043A\u043E\u043E\u0440\u0434\u0438\u043D\u0430\u0442\u043D\u0443\u044E \u043E\u0431\u043B\u0430\u0441\u0442\u044C \u0441 \u0442\u0440\u0435\u0439\u0441\u043E\u043C, \u0441\u0435\u0442\u043A\u043E\u0439 \u0438 \u0440\u0430\u043C\u043A\u043E\u0439.",
            nextCalibrationAnchors = "\u0414\u0430\u043B\u0435\u0435: \u0442\u043E\u0447\u043A\u0438 \u043A\u0430\u043B\u0438\u0431\u0440\u043E\u0432\u043A\u0438",
            nextDisabledReason = "\u0411\u0443\u0434\u0435\u0442 \u0434\u043E\u0441\u0442\u0443\u043F\u043D\u043E \u0432 Phase 3 \u043F\u043E\u0441\u043B\u0435 \u043F\u043E\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043D\u0438\u044F plotArea.",
            zoomHint = "\u041C\u0430\u0441\u0448\u0442\u0430\u0431: pinch. \u0414\u0432\u0438\u0433\u0430\u0439\u0442\u0435 \u0440\u0430\u043C\u043A\u0443 \u0438\u043B\u0438 handles.",
            moveSelection = "\u041F\u0435\u0440\u0435\u043C\u0435\u0441\u0442\u0438\u0442\u044C \u0440\u0430\u043C\u043A\u0443",
            resizeSelection = "\u0418\u0437\u043C\u0435\u043D\u0438\u0442\u044C \u0440\u0430\u0437\u043C\u0435\u0440 \u0440\u0430\u043C\u043A\u0438",
            imageDescription = "\u041D\u043E\u0440\u043C\u0430\u043B\u0438\u0437\u043E\u0432\u0430\u043D\u043D\u043E\u0435 \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u0445\u0440\u043E\u043C\u0430\u0442\u043E\u0433\u0440\u0430\u043C\u043C\u044B",
            back = "\u041D\u0430\u0437\u0430\u0434",
        )
    }
