package com.chromalab.feature.processing.geometry

object RuntimeOcrAnchorBridgeBuilder {
    fun build(
        graphIndex: Int,
        tickOcrResult: TickOcrResult?,
        axisScaleResolution: AxisScaleResolutionResult?,
    ): List<RuntimeOcrAnchorBridgeRow> {
        val scaleRows = axisScaleResolution
            ?.let { resolution ->
                resolution.xAnchors.map { it.toBridgeRow(graphIndex, TickOcrItemStatus.ACCEPTED) } +
                    resolution.yAnchors.map { it.toBridgeRow(graphIndex, TickOcrItemStatus.ACCEPTED) } +
                    resolution.rejectedAnchors.map { it.toBridgeRow(graphIndex, TickOcrItemStatus.REJECTED) }
            }
            .orEmpty()

        val scaleKeys = scaleRows.map { it.anchorKey() }.toSet()
        val tickRows = tickOcrResult
            ?.items
            .orEmpty()
            .map { it.toBridgeRow(graphIndex) }
            .filterNot { it.anchorKey() in scaleKeys }

        return retargetRows(
            rows = (scaleRows + tickRows).distinctBy { it.anchorKey() },
            graphIndex = graphIndex,
        )
    }

    fun retargetRows(
        rows: List<RuntimeOcrAnchorBridgeRow>,
        graphIndex: Int,
    ): List<RuntimeOcrAnchorBridgeRow> =
        rows.mapIndexed { index, row ->
            row.copy(
                runtimeRowId = "runtime-ocr-anchor:$graphIndex:${index + 1}",
                graphId = "graph:$graphIndex",
                graphIndex = graphIndex,
            )
        }

    private fun AxisScaleAnchor.toBridgeRow(
        graphIndex: Int,
        requestedStatus: TickOcrItemStatus,
    ): RuntimeOcrAnchorBridgeRow {
        val forbiddenReason = rawText?.forbiddenScaleLabelReason()
        val missingGeometryReason = if (numericValue != null && pixelCoordinate.isNaN()) {
            "runtime_ocr_anchor.pixel_geometry_missing"
        } else {
            null
        }
        val status = when {
            forbiddenReason != null || rejectionReason != null || missingGeometryReason != null ->
                TickOcrItemStatus.REJECTED
            numericValue == null -> TickOcrItemStatus.REJECTED
            requestedStatus == TickOcrItemStatus.ACCEPTED -> TickOcrItemStatus.ACCEPTED
            else -> requestedStatus
        }
        val cropReason = cropMissingReason(cropPath, evidenceType)
        val rowRejectionReason = when {
            forbiddenReason != null -> forbiddenReason
            rejectionReason != null -> rejectionReason
            missingGeometryReason != null -> missingGeometryReason
            numericValue == null -> "runtime_ocr_anchor.numeric_value_missing"
            status != TickOcrItemStatus.ACCEPTED -> "runtime_ocr_anchor.rejected_by_axis_scale_resolver"
            else -> null
        }
        return RuntimeOcrAnchorBridgeRow(
            runtimeRowId = "",
            graphId = "graph:$graphIndex",
            graphIndex = graphIndex,
            axis = axis,
            rawText = rawText ?: numericValue?.toString() ?: "unparsed-axis-label",
            parsedNumericValue = numericValue,
            pixelCoordinate = pixelCoordinate,
            coordinateFrame = RuntimeOcrAnchorCoordinateFrame.PLOT_RELATIVE,
            sourceCropRef = cropPath?.let { "crop:$it" }
                ?: "graph:$graphIndex:${axis.name}:${pixelCoordinate}",
            sourceCropPath = cropPath,
            cropFileAvailable = !cropPath.isNullOrBlank(),
            cropMissingReason = cropReason,
            confidence = confidence,
            geometrySource = evidenceType,
            numericSource = "LOCAL_OCR_TEXT",
            projectionSource = projectionSource,
            status = status,
            rejectionReason = rowRejectionReason,
        )
    }

    private fun TickOcrItem.toBridgeRow(graphIndex: Int): RuntimeOcrAnchorBridgeRow {
        val forbiddenReason = rawText.forbiddenScaleLabelReason()
        val vlmOnlyReason = if (ocrEngine == TickOcrEngine.VLM) {
            "runtime_ocr_anchor.vlm_numeric_text_advisory_only"
        } else {
            null
        }
        val rowStatus = when {
            forbiddenReason != null || vlmOnlyReason != null -> TickOcrItemStatus.REJECTED
            status == TickOcrItemStatus.ACCEPTED &&
                parsedNumericValue != null &&
                tickPixelPosition != null -> TickOcrItemStatus.ACCEPTED
            status == TickOcrItemStatus.SEMANTIC_ONLY -> TickOcrItemStatus.SEMANTIC_ONLY
            else -> TickOcrItemStatus.REJECTED
        }
        val rowRejectionReason = when {
            forbiddenReason != null -> forbiddenReason
            vlmOnlyReason != null -> vlmOnlyReason
            rejectionReason != null -> rejectionReason
            parsedNumericValue == null -> "runtime_ocr_anchor.numeric_value_missing"
            tickPixelPosition == null -> "runtime_ocr_anchor.pixel_geometry_missing"
            rowStatus != TickOcrItemStatus.ACCEPTED -> "runtime_ocr_anchor.rejected_by_tick_ocr_matcher"
            else -> null
        }
        return RuntimeOcrAnchorBridgeRow(
            runtimeRowId = "",
            graphId = "graph:$graphIndex",
            graphIndex = graphIndex,
            axis = axis,
            rawText = rawText,
            parsedNumericValue = parsedNumericValue,
            pixelCoordinate = tickPixelPosition,
            coordinateFrame = RuntimeOcrAnchorCoordinateFrame.IMAGE_ABSOLUTE,
            sourceCropRef = localCropPath?.let { "crop:$it" }
                ?: "graph:$graphIndex:${axis.name}:${tickPixelPosition ?: "no_pixel"}",
            sourceCropPath = localCropPath,
            cropFileAvailable = !localCropPath.isNullOrBlank(),
            cropMissingReason = cropMissingReason(localCropPath, AxisScaleEvidenceType.EXPLICIT_TICK_MARK),
            confidence = confidence,
            geometrySource = AxisScaleEvidenceType.EXPLICIT_TICK_MARK,
            numericSource = if (ocrEngine == TickOcrEngine.VLM) {
                "VLM_TEXT_ADVISORY_REJECTED"
            } else {
                "LOCAL_TICK_CROP_OCR"
            },
            status = rowStatus,
            rejectionReason = rowRejectionReason,
        )
    }

    private fun RuntimeOcrAnchorBridgeRow.anchorKey(): String =
        listOf(
            axis.name,
            rawText.trim(),
            parsedNumericValue?.toString().orEmpty(),
            pixelCoordinate?.toString().orEmpty(),
            coordinateFrame?.name.orEmpty(),
            geometrySource?.name.orEmpty(),
            status.name,
            rejectionReason.orEmpty(),
        ).joinToString("|")

    private fun cropMissingReason(
        cropPath: String?,
        evidenceType: AxisScaleEvidenceType?,
    ): String? =
        if (!cropPath.isNullOrBlank()) {
            null
        } else {
            when (evidenceType) {
                AxisScaleEvidenceType.EXPLICIT_TICK_MARK,
                AxisScaleEvidenceType.OCR_LABEL_BOX,
                AxisScaleEvidenceType.LABEL_PROJECTION ->
                    "runtime_ocr_anchor.crop_path_missing"
                else -> null
            }
        }

    private fun String.forbiddenScaleLabelReason(): String? {
        val lower = lowercase()
        return when {
            "m/z" in lower || "ion" in lower || lower.startsWith("sim") || "scan" in lower ||
                " to " in lower || "):" in lower ->
                "runtime_ocr_anchor.title_ion_or_method_text_rejected"
            else -> null
        }
    }
}
