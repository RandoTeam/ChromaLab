package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrTextElement
import kotlin.math.abs

internal object TickOcrMatcher {
    fun toTickOcrResult(
        ocr: AxisOcrResult?,
        panelRegion: GraphRegion?,
        ticks: TickGeometry,
        cropArtifacts: List<TickOcrCropArtifact> = emptyList(),
    ): TickOcrResult {
        if (ocr == null) {
            return TickOcrResult(
                warnings = listOf("tick_ocr.not_available"),
                timestamp = System.currentTimeMillis(),
            )
        }
        val panel = panelRegion ?: return TickOcrResult(
            warnings = listOf("tick_ocr.panel_missing"),
            timestamp = ocr.timestamp,
        )
        val xTolerance = (panel.width * 0.035f).coerceAtLeast(8f)
        val yTolerance = (panel.height * 0.035f).coerceAtLeast(8f)
        val matcherWarnings = mutableListOf<String>()
        val items = ocr.rawElements.mapNotNull { element ->
            val value = element.numericValue ?: return@mapNotNull null
            val sourcedAxis = element.sourceGeometryAxis()
            val item = if (sourcedAxis != null) {
                element.toSourcedTickOcrItemOrNull(
                    axis = sourcedAxis,
                    ticks = ticks.positionsFor(sourcedAxis),
                    sourceTolerance = 1.5f,
                    cropArtifacts = cropArtifacts,
                )
            } else if (element.sourceAxis != null) {
                matcherWarnings += "tick_ocr.invalid_source_axis:${element.sourceAxis}"
                null
            } else {
                val xItem = element.toBandMatchedTickOcrItemOrNull(
                    axis = GeometryAxis.X,
                    ticks = ticks.xTicks.map { it.pixelCoordinate },
                    tolerance = xTolerance,
                    coordinate = element.centerX,
                    expectedBand = element.centerY >= panel.y + panel.height * 0.45f,
                    cropArtifacts = cropArtifacts,
                )
                val yItem = element.toBandMatchedTickOcrItemOrNull(
                    axis = GeometryAxis.Y,
                    ticks = ticks.yTicks.map { it.pixelCoordinate },
                    tolerance = yTolerance,
                    coordinate = element.centerY,
                    expectedBand = element.centerX <= panel.x + panel.width * 0.42f,
                    cropArtifacts = cropArtifacts,
                )
                listOfNotNull(xItem, yItem).maxWithOrNull(
                    compareBy<TickOcrItem> { if (it.status == TickOcrItemStatus.ACCEPTED) 1 else 0 }
                        .thenByDescending { it.confidence },
                )
            }
            item?.copy(parsedNumericValue = value.toDouble())
        }
        val semanticOnlyCount = items.count { it.status == TickOcrItemStatus.SEMANTIC_ONLY }
        return TickOcrResult(
            items = items,
            warnings = buildList {
                addAll(ocr.warnings)
                addAll(matcherWarnings)
                if (ticks.xTicks.size < 2) add("tick_geometry.x_positions_insufficient")
                if (ticks.yTicks.size < 2) add("tick_geometry.y_positions_insufficient")
                if ((ticks.xTicks.isNotEmpty() || ticks.yTicks.isNotEmpty()) && cropArtifacts.isEmpty()) {
                    add("tick_ocr.local_crops_missing")
                }
                if (semanticOnlyCount > 0) add("tick_ocr.semantic_only:$semanticOnlyCount")
            }.distinct(),
            timestamp = ocr.timestamp,
        )
    }

    private fun OcrTextElement.toSourcedTickOcrItemOrNull(
        axis: GeometryAxis,
        ticks: List<Float>,
        sourceTolerance: Float,
        cropArtifacts: List<TickOcrCropArtifact>,
    ): TickOcrItem? {
        val sourceTick = sourceTickPixelPosition ?: return TickOcrItem(
            axis = axis,
            rawText = text,
            parsedNumericValue = numericValue?.toDouble(),
            ocrEngine = TickOcrEngine.BOTH,
            confidence = confidence,
            status = TickOcrItemStatus.SEMANTIC_ONLY,
            rejectionReason = "tick_ocr.numeric_value_without_deterministic_tick_position",
        )
        val nearest = ticks.minByOrNull { abs(it - sourceTick) }
        val distance = nearest?.let { abs(it - sourceTick) }
        val accepted = nearest != null && distance != null && distance <= sourceTolerance
        return TickOcrItem(
            axis = axis,
            tickPixelPosition = nearest?.takeIf { accepted },
            localCropPath = sourceCropPath ?: nearest
                ?.takeIf { accepted }
                ?.let { cropArtifacts.pathFor(axis, it) },
            rawText = text,
            parsedNumericValue = numericValue?.toDouble(),
            ocrEngine = TickOcrEngine.BOTH,
            confidence = confidence,
            status = if (accepted) TickOcrItemStatus.ACCEPTED else TickOcrItemStatus.SEMANTIC_ONLY,
            rejectionReason = if (accepted) {
                null
            } else {
                "tick_ocr.source_tick_without_matching_deterministic_position"
            },
        )
    }

    private fun OcrTextElement.toBandMatchedTickOcrItemOrNull(
        axis: GeometryAxis,
        ticks: List<Float>,
        tolerance: Float,
        coordinate: Float,
        expectedBand: Boolean,
        cropArtifacts: List<TickOcrCropArtifact>,
    ): TickOcrItem? {
        if (!expectedBand) return null
        val nearest = ticks.minByOrNull { abs(it - coordinate) }
        val distance = nearest?.let { abs(it - coordinate) }
        val status = when {
            nearest == null -> TickOcrItemStatus.SEMANTIC_ONLY
            distance != null && distance <= tolerance -> TickOcrItemStatus.ACCEPTED
            else -> TickOcrItemStatus.SEMANTIC_ONLY
        }
        return TickOcrItem(
            axis = axis,
            tickPixelPosition = nearest?.takeIf { status == TickOcrItemStatus.ACCEPTED },
            localCropPath = nearest
                ?.takeIf { status == TickOcrItemStatus.ACCEPTED }
                ?.let { cropArtifacts.pathFor(axis, it) },
            rawText = text,
            parsedNumericValue = numericValue?.toDouble(),
            ocrEngine = TickOcrEngine.BOTH,
            confidence = confidence,
            status = status,
            rejectionReason = if (status == TickOcrItemStatus.SEMANTIC_ONLY) {
                "tick_ocr.numeric_value_without_deterministic_tick_position"
            } else {
                null
            },
        )
    }

    private fun TickGeometry.positionsFor(axis: GeometryAxis): List<Float> =
        when (axis) {
            GeometryAxis.X -> xTicks.map { it.pixelCoordinate }
            GeometryAxis.Y -> yTicks.map { it.pixelCoordinate }
        }

    private fun OcrTextElement.sourceGeometryAxis(): GeometryAxis? =
        sourceAxis?.uppercase()?.let { axis ->
            runCatching { GeometryAxis.valueOf(axis) }.getOrNull()
        }

    private fun List<TickOcrCropArtifact>.pathFor(axis: GeometryAxis, tickPixelPosition: Float): String? =
        firstOrNull {
            it.axis == axis && abs(it.tickPixelPosition - tickPixelPosition) <= 0.5f
        }?.path

    private val OcrTextElement.centerX: Float
        get() = x + width / 2f

    private val OcrTextElement.centerY: Float
        get() = y + height / 2f
}
