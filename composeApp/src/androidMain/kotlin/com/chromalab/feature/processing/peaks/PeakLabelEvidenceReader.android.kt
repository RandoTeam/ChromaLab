package com.chromalab.feature.processing.peaks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.inference.ActiveVisionModelBackend
import com.chromalab.feature.processing.inference.VisionLocalTextCropContext
import com.chromalab.feature.processing.inference.VisionLocalTextCropResult
import com.chromalab.feature.processing.inference.VisionTextRegionType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.withTimeoutOrNull

actual class PeakLabelEvidenceReader actual constructor() {
    private val visionBackend = ActiveVisionModelBackend()

    actual suspend fun readPeakLabels(
        imagePath: String,
        outputDir: String,
        graphPanelBounds: GraphRegion,
        plotAreaBounds: GraphRegion,
    ): PeakLabelEvidenceResult {
        val source = BitmapFactory.decodeFile(imagePath)
            ?: return PeakLabelEvidenceResult(warnings = listOf("peak_label_ocr.image_read_failed"))
        val dir = File(outputDir, "peak_label_crops").also { it.mkdirs() }
        return try {
            val crops = buildPeakLabelCropRegions(source.width, source.height, graphPanelBounds, plotAreaBounds)
            val labels = mutableListOf<PeakLabelEvidence>()
            val cropPaths = mutableListOf<String>()
            val fallbackWarnings = mutableListOf<String>()
            crops.forEachIndexed { index, crop ->
                val file = File(dir, "peak_label_${index.toString().padStart(2, '0')}_${crop.kind}.png")
                val bitmap = Bitmap.createBitmap(
                    source,
                    crop.region.x,
                    crop.region.y,
                    crop.region.width,
                    crop.region.height,
                )
                val saved = FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (saved) cropPaths.add(file.absolutePath)
                val elements = scanCrop(bitmap, crop.region)
                val mlKitLabels = elements.map { element ->
                    val parsed = parsePeakRetentionLabel(element.text)
                    val classification = classifyTextElement(
                        text = element.text,
                        parsedRetentionTime = parsed,
                        elementRegion = element.region,
                        crop = crop,
                        plotArea = plotAreaBounds,
                    )
                    PeakLabelEvidence(
                        rawText = element.text,
                        normalizedText = normalizePeakLabelText(element.text),
                        parsedRetentionTime = parsed,
                        labelBoxPx = element.region,
                        cropBoundsPx = crop.region,
                        linkedGraphPanelBounds = graphPanelBounds,
                        linkedPlotAreaBounds = plotAreaBounds,
                        localCropPath = file.absolutePath.takeIf { saved },
                        source = PeakLabelEvidenceSource.ML_KIT,
                        confidence = element.confidence,
                        status = when {
                            parsed == null -> PeakLabelEvidenceStatus.REJECTED
                            element.confidence >= 0.55f -> PeakLabelEvidenceStatus.VALID_TEXT
                            else -> PeakLabelEvidenceStatus.AMBIGUOUS_TEXT
                        },
                        textClassification = classification,
                        isRuntimeEvidence = true,
                        rejectionReason = when {
                            parsed == null -> "ocr_text_not_rt_like"
                            classification != PeakLabelTextClassification.PEAK_ANNOTATION -> "ocr_text_not_peak_annotation"
                            element.confidence < 0.55f -> "ocr_low_confidence"
                            else -> null
                        },
                        warnings = buildList {
                            add("peak_label_ocr.local_crop:${crop.kind}")
                            if (crop.insidePlotArea) add("peak_label_ocr.crop_inside_plot_area_requires_signal_verification")
                            if (classification != PeakLabelTextClassification.PEAK_ANNOTATION) {
                                add("peak_label_ocr.not_peak_annotation:${classification.name.lowercase()}")
                            }
                        },
                    )
                }
                val vlmResult = if (saved && crop.insidePlotArea && shouldUseVlmFallback(mlKitLabels)) {
                    withTimeoutOrNull(LOCAL_CROP_VLM_TIMEOUT_MS) {
                        visionBackend.readLocalTextCrop(
                            cropImagePath = file.absolutePath,
                            context = VisionLocalTextCropContext(
                                cropKind = crop.kind,
                                insidePlotArea = crop.insidePlotArea,
                                graphContext = "graphPanel=${graphPanelBounds.width}x${graphPanelBounds.height};plotArea=${plotAreaBounds.width}x${plotAreaBounds.height}",
                            ),
                        )
                    }.also {
                        if (it == null) fallbackWarnings += "peak_label_ocr.vlm_local_crop_timeout:${crop.kind}"
                    }
                } else {
                    if (saved && !crop.insidePlotArea && shouldUseVlmFallback(mlKitLabels)) {
                        fallbackWarnings += "peak_label_ocr.vlm_skipped_non_plot_text_band:${crop.kind}"
                    }
                    null
                }
                val vlmLabel = vlmResult?.toPeakLabelEvidence(
                        crop = crop,
                        graphPanel = graphPanelBounds,
                        plotArea = plotAreaBounds,
                        cropPath = file.absolutePath,
                    )
                bitmap.recycle()
                labels += mergeMlKitAndVlmEvidence(mlKitLabels, vlmLabel)
            }
            val mergedLabels = labels
                .distinctBy { "${it.normalizedText}:${it.parsedRetentionTime}:${it.labelBoxPx?.x}:${it.labelBoxPx?.y}:${it.source}" }
                .sortedBy { it.parsedRetentionTime ?: Double.POSITIVE_INFINITY }
            val peakAnnotationLabels = mergedLabels.filter {
                it.textClassification == PeakLabelTextClassification.PEAK_ANNOTATION
            }
            val cropOverlay = savePeakLabelOverlay(
                source = source,
                outputDir = dir,
                fileName = "peak_label_crop_bounds_overlay.png",
                crops = crops,
                labels = emptyList(),
                showLabels = false,
            )
            val classificationOverlay = savePeakLabelOverlay(
                source = source,
                outputDir = dir,
                fileName = "peak_label_text_classification_overlay.png",
                crops = crops,
                labels = mergedLabels,
                showLabels = true,
            )
            PeakLabelEvidenceResult(
                labels = peakAnnotationLabels,
                cropPaths = cropPaths.distinct(),
                cropBoundsOverlayPath = cropOverlay,
                textClassificationOverlayPath = classificationOverlay,
                warnings = buildList {
                    if (peakAnnotationLabels.isEmpty()) {
                        add("peak_label_ocr.no_peak_annotations")
                    }
                    if (cropPaths.isEmpty()) add("peak_label_ocr.no_crops_written")
                    add("peak_label_ocr.runtime_local_crops_used")
                    if (labels.any { it.source == PeakLabelEvidenceSource.VLM || it.source == PeakLabelEvidenceSource.BOTH }) {
                        add("peak_label_ocr.vlm_local_crop_fallback_used")
                    }
                    addAll(fallbackWarnings)
                }.distinct(),
            )
        } finally {
            source.recycle()
        }
    }

    private suspend fun scanCrop(bitmap: Bitmap, cropRegion: GraphRegion): List<PeakLabelTextElement> {
        val input = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return suspendCoroutine { cont ->
            recognizer.process(input)
                .addOnSuccessListener { text ->
                    val elements = text.textBlocks.flatMap { block ->
                        block.lines.mapNotNull { line ->
                            val box = line.boundingBox ?: return@mapNotNull null
                            val raw = line.text.trim()
                            if (raw.isBlank()) return@mapNotNull null
                            PeakLabelTextElement(
                                text = raw,
                                region = GraphRegion(
                                    x = cropRegion.x + box.left,
                                    y = cropRegion.y + box.top,
                                    width = box.width().coerceAtLeast(1),
                                    height = box.height().coerceAtLeast(1),
                                    label = "Peak label OCR",
                                ),
                                confidence = 0.78f,
                            )
                        }
                    }
                    cont.resume(elements)
                }
                .addOnFailureListener {
                    cont.resume(emptyList())
                }
        }
    }
}

private data class PeakLabelCropRegion(
    val kind: String,
    val region: GraphRegion,
    val insidePlotArea: Boolean,
)

private data class PeakLabelTextElement(
    val text: String,
    val region: GraphRegion,
    val confidence: Float,
)

private fun shouldUseVlmFallback(labels: List<PeakLabelEvidence>): Boolean {
    if (labels.isEmpty()) return true
    if (labels.none { it.parsedRetentionTime != null && it.textClassification == PeakLabelTextClassification.PEAK_ANNOTATION }) {
        return true
    }
    return labels.any {
        it.status == PeakLabelEvidenceStatus.AMBIGUOUS_TEXT ||
            it.confidence < 0.55f ||
            it.textClassification == PeakLabelTextClassification.UNKNOWN_TEXT
    }
}

private fun VisionLocalTextCropResult.toPeakLabelEvidence(
    crop: PeakLabelCropRegion,
    graphPanel: GraphRegion,
    plotArea: GraphRegion,
    cropPath: String,
): PeakLabelEvidence {
    val parsed = parsedRetentionTime ?: parsePeakRetentionLabel(rawText)
    val classification = classifyVlmTextRegion(rawText, textType, parsed, crop, plotArea)
    return PeakLabelEvidence(
        rawText = rawText,
        normalizedText = normalizedText.ifBlank { normalizePeakLabelText(rawText) },
        parsedRetentionTime = parsed,
        labelBoxPx = crop.region,
        cropBoundsPx = crop.region,
        linkedGraphPanelBounds = graphPanel,
        linkedPlotAreaBounds = plotArea,
        localCropPath = cropPath,
        source = PeakLabelEvidenceSource.VLM,
        confidence = confidence,
        status = when {
            rawText.isBlank() -> PeakLabelEvidenceStatus.REJECTED
            parsed != null && confidence >= 0.50f -> PeakLabelEvidenceStatus.VALID_TEXT
            parsed != null -> PeakLabelEvidenceStatus.AMBIGUOUS_TEXT
            else -> PeakLabelEvidenceStatus.REJECTED
        },
        textClassification = classification,
        isRuntimeEvidence = true,
        rejectionReason = when {
            rawText.isBlank() -> "vlm_empty_text"
            parsed == null -> "vlm_text_not_rt_like"
            classification != PeakLabelTextClassification.PEAK_ANNOTATION -> "vlm_text_not_peak_annotation"
            confidence < 0.50f -> "vlm_low_confidence"
            else -> null
        },
        rejectedForbiddenFields = rejectedForbiddenFields,
        runtimeProfile = runtimeProfile,
        warnings = warnings + buildList {
            add("peak_label_ocr.local_crop:${crop.kind}")
            add("peak_label_ocr.vlm_text_only_no_peak_metrics")
            if (crop.insidePlotArea) add("peak_label_ocr.crop_inside_plot_area_requires_signal_verification")
        },
    )
}

private fun VisionTextRegionType.toPeakLabelTextClassification(parsed: Double?): PeakLabelTextClassification =
    when (this) {
        VisionTextRegionType.PEAK_ANNOTATION -> PeakLabelTextClassification.PEAK_ANNOTATION
        VisionTextRegionType.TICK_LABEL -> PeakLabelTextClassification.TICK_LABEL
        VisionTextRegionType.AXIS_LABEL -> PeakLabelTextClassification.AXIS_LABEL
        VisionTextRegionType.TITLE_OR_CHANNEL -> PeakLabelTextClassification.TITLE_OR_CHANNEL
        VisionTextRegionType.PAGE_TEXT -> PeakLabelTextClassification.PAGE_TEXT
        VisionTextRegionType.UNKNOWN_TEXT -> if (parsed != null) {
            PeakLabelTextClassification.PEAK_ANNOTATION
        } else {
            PeakLabelTextClassification.UNKNOWN_TEXT
        }
    }

private fun classifyVlmTextRegion(
    text: String,
    textType: VisionTextRegionType,
    parsed: Double?,
    crop: PeakLabelCropRegion,
    plotArea: GraphRegion,
): PeakLabelTextClassification {
    if (isTitleOrIonChannelText(text)) return PeakLabelTextClassification.TITLE_OR_CHANNEL
    return when (textType) {
        VisionTextRegionType.UNKNOWN_TEXT -> classifyTextElement(
            text = text,
            parsedRetentionTime = parsed,
            elementRegion = crop.region,
            crop = crop,
            plotArea = plotArea,
        )
        else -> textType.toPeakLabelTextClassification(parsed)
    }
}

private fun mergeMlKitAndVlmEvidence(
    mlKitLabels: List<PeakLabelEvidence>,
    vlmLabel: PeakLabelEvidence?,
): List<PeakLabelEvidence> {
    if (vlmLabel == null) return mlKitLabels
    val matchingIndex = mlKitLabels.indexOfFirst { ml ->
        val mlRt = ml.parsedRetentionTime
        val vlmRt = vlmLabel.parsedRetentionTime
        if (mlRt != null && vlmRt != null) {
            kotlin.math.abs(mlRt - vlmRt) <= 0.001
        } else {
            ml.normalizedText.equals(vlmLabel.normalizedText, ignoreCase = true)
        }
    }
    if (matchingIndex < 0) {
        return mlKitLabels + vlmLabel.copy(
            warnings = vlmLabel.warnings + "peak_label_ocr.vlm_unmatched_mlkit_text",
        )
    }
    return mlKitLabels.mapIndexed { index, ml ->
        if (index != matchingIndex) {
            ml
        } else {
            ml.copy(
                source = PeakLabelEvidenceSource.BOTH,
                confidence = maxOf(ml.confidence, vlmLabel.confidence),
                rejectedForbiddenFields = vlmLabel.rejectedForbiddenFields,
                runtimeProfile = vlmLabel.runtimeProfile,
                status = when {
                    ml.status == PeakLabelEvidenceStatus.VALID_TEXT ||
                        vlmLabel.status == PeakLabelEvidenceStatus.VALID_TEXT -> PeakLabelEvidenceStatus.VALID_TEXT
                    ml.status == PeakLabelEvidenceStatus.AMBIGUOUS_TEXT ||
                        vlmLabel.status == PeakLabelEvidenceStatus.AMBIGUOUS_TEXT -> PeakLabelEvidenceStatus.AMBIGUOUS_TEXT
                    else -> PeakLabelEvidenceStatus.REJECTED
                },
                warnings = (ml.warnings + vlmLabel.warnings + "peak_label_ocr.mlkit_vlm_agree").distinct(),
            )
        }
    }
}

private fun buildPeakLabelCropRegions(
    imageWidth: Int,
    imageHeight: Int,
    graphPanel: GraphRegion,
    plotArea: GraphRegion,
): List<PeakLabelCropRegion> = buildList {
    val panelTitleHeight = (plotArea.y - graphPanel.y).coerceAtLeast(0)
    if (panelTitleHeight >= 12) {
        add(
            PeakLabelCropRegion(
                kind = "panel_text_band",
                region = GraphRegion(
                    x = graphPanel.x,
                    y = graphPanel.y,
                    width = graphPanel.width,
                    height = panelTitleHeight,
                    label = "Peak label panel text band",
                ).clampedTo(imageWidth, imageHeight),
                insidePlotArea = false,
            ),
        )
    }
    add(
        PeakLabelCropRegion(
            kind = "plot_top_label_band",
            region = GraphRegion(
                x = plotArea.x,
                y = plotArea.y,
                width = plotArea.width,
                height = (plotArea.height * 0.42f).toInt().coerceAtLeast(16),
                label = "Peak label plot top band",
            ).clampedTo(imageWidth, imageHeight),
            insidePlotArea = true,
        ),
    )
}

private fun parsePeakRetentionLabel(text: String): Double? {
    val match = Regex("""(?<!\d)(\d{1,3}[.,]\d{1,4})(?!\d)""").find(text.replace(" ", ""))
        ?: return null
    return match.groupValues[1].replace(',', '.').toDoubleOrNull()
}

private fun classifyTextElement(
    text: String,
    parsedRetentionTime: Double?,
    elementRegion: GraphRegion,
    crop: PeakLabelCropRegion,
    plotArea: GraphRegion,
): PeakLabelTextClassification {
    val normalized = normalizePeakLabelText(text).lowercase()
    val centerX = elementRegion.x + elementRegion.width / 2f
    val centerY = elementRegion.y + elementRegion.height / 2f
    val insidePlot = centerX in plotArea.x.toFloat()..plotArea.right.toFloat() &&
        centerY in plotArea.y.toFloat()..plotArea.bottom.toFloat()
    val nearPlot = centerX in (plotArea.x - plotArea.width * 0.08f)..(plotArea.right + plotArea.width * 0.08f) &&
        centerY in (plotArea.y - plotArea.height * 0.10f)..(plotArea.bottom + plotArea.height * 0.10f)
    return when {
        isTitleOrIonChannelText(normalized) -> PeakLabelTextClassification.TITLE_OR_CHANNEL
        parsedRetentionTime != null && (insidePlot || nearPlot) -> PeakLabelTextClassification.PEAK_ANNOTATION
        normalized.contains("time") ||
            normalized.contains("время") ||
            normalized.contains("abundance") ||
            normalized.contains("intensity") ||
            normalized.contains("интенсив") -> PeakLabelTextClassification.AXIS_LABEL
        parsedRetentionTime != null && !crop.insidePlotArea -> PeakLabelTextClassification.TICK_LABEL
        !crop.insidePlotArea -> PeakLabelTextClassification.PAGE_TEXT
        else -> PeakLabelTextClassification.UNKNOWN_TEXT
    }
}

private fun isTitleOrIonChannelText(text: String): Boolean {
    return PeakLabelTextHeuristics.isTitleOrIonChannelText(text)
}

private fun normalizePeakLabelText(text: String): String =
    PeakLabelTextHeuristics.normalize(text)

private const val LOCAL_CROP_VLM_TIMEOUT_MS = 6_000L

private fun savePeakLabelOverlay(
    source: Bitmap,
    outputDir: File,
    fileName: String,
    crops: List<PeakLabelCropRegion>,
    labels: List<PeakLabelEvidence>,
    showLabels: Boolean,
): String? {
    val bitmap = source.copy(Bitmap.Config.ARGB_8888, true) ?: return null
    val canvas = Canvas(bitmap)
    val cropPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(220, 33, 150, 243)
    }
    val textPaint = Paint().apply {
        style = Paint.Style.FILL
        textSize = 22f
        color = Color.argb(240, 0, 150, 80)
    }
    crops.forEachIndexed { index, crop ->
        canvas.drawRect(
            crop.region.x.toFloat(),
            crop.region.y.toFloat(),
            crop.region.right.toFloat(),
            crop.region.bottom.toFloat(),
            cropPaint,
        )
        canvas.drawText(
            "#${index + 1} ${crop.kind}",
            crop.region.x.toFloat() + 4f,
            (crop.region.y + 22).toFloat(),
            textPaint,
        )
    }
    if (showLabels) {
        val labelPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        labels.forEach { label ->
            val region = label.labelBoxPx ?: return@forEach
            labelPaint.color = when (label.textClassification) {
                PeakLabelTextClassification.PEAK_ANNOTATION -> Color.argb(230, 76, 175, 80)
                PeakLabelTextClassification.TICK_LABEL -> Color.argb(230, 255, 193, 7)
                PeakLabelTextClassification.AXIS_LABEL -> Color.argb(230, 255, 152, 0)
                PeakLabelTextClassification.TITLE_OR_CHANNEL -> Color.argb(230, 244, 67, 54)
                PeakLabelTextClassification.PAGE_TEXT -> Color.argb(230, 156, 39, 176)
                PeakLabelTextClassification.UNKNOWN_TEXT -> Color.argb(230, 96, 125, 139)
            }
            canvas.drawRect(
                region.x.toFloat(),
                region.y.toFloat(),
                region.right.toFloat(),
                region.bottom.toFloat(),
                labelPaint,
            )
            canvas.drawText(
                "${label.textClassification.name}:${label.rawText.take(18)}",
                region.x.toFloat() + 4f,
                (region.y - 6).coerceAtLeast(18).toFloat(),
                textPaint,
            )
        }
    }
    val file = File(outputDir, fileName)
    val saved = FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    bitmap.recycle()
    return file.absolutePath.takeIf { saved }
}

private fun GraphRegion.clampedTo(imageWidth: Int, imageHeight: Int): GraphRegion {
    val safeX = x.coerceIn(0, (imageWidth - 1).coerceAtLeast(0))
    val safeY = y.coerceIn(0, (imageHeight - 1).coerceAtLeast(0))
    val safeRight = right.coerceIn(safeX + 1, imageWidth.coerceAtLeast(safeX + 1))
    val safeBottom = bottom.coerceIn(safeY + 1, imageHeight.coerceAtLeast(safeY + 1))
    return copy(x = safeX, y = safeY, width = safeRight - safeX, height = safeBottom - safeY)
}
