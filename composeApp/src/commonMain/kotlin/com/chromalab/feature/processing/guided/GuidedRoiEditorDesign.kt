package com.chromalab.feature.processing.guided

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.chromalab.core.ui.theme.Amber80
import com.chromalab.core.ui.theme.Blue80
import com.chromalab.core.ui.theme.Error
import com.chromalab.core.ui.theme.Neutral4
import com.chromalab.core.ui.theme.Neutral10
import com.chromalab.core.ui.theme.Neutral95
import com.chromalab.core.ui.theme.Success
import com.chromalab.core.ui.theme.Warning

@Immutable
data class GuidedRoiEditorColors(
    val background: Color,
    val panelSurface: Color,
    val graphPanel: Color,
    val plotArea: Color,
    val confirmed: Color,
    val warning: Color,
    val invalid: Color,
    val handle: Color,
    val outsideScrim: Color,
    val guideLine: Color,
    val labelText: Color,
)

@Composable
fun guidedRoiEditorColors(): GuidedRoiEditorColors =
    GuidedRoiEditorColors(
        background = Neutral4,
        panelSurface = Neutral10.copy(alpha = 0.94f),
        graphPanel = Blue80,
        plotArea = Amber80,
        confirmed = Success,
        warning = Warning,
        invalid = Error,
        handle = Neutral95,
        outsideScrim = Color.Black.copy(alpha = 0.46f),
        guideLine = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f),
        labelText = Neutral95,
    )

fun GuidedRoiEditorStatus.statusColor(colors: GuidedRoiEditorColors): Color =
    when (this) {
        GuidedRoiEditorStatus.SUGGESTED -> colors.graphPanel
        GuidedRoiEditorStatus.USER_CONFIRMED -> colors.confirmed
        GuidedRoiEditorStatus.REVIEW -> colors.warning
        GuidedRoiEditorStatus.INVALID -> colors.invalid
    }
