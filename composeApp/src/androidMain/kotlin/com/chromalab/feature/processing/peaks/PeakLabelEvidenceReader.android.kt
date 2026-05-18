package com.chromalab.feature.processing.peaks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.chromalab.feature.processing.graph.GraphRegion
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class PeakLabelEvidenceReader actual constructor() {
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
                bitmap.recycle()
                labels += elements.mapNotNull { element ->
                    val parsed = parsePeakRetentionLabel(element.text) ?: return@mapNotNull null
                    PeakLabelEvidence(
                        rawText = element.text,
                        parsedRetentionTime = parsed,
                        labelBoxPx = element.region,
                        linkedGraphPanelBounds = graphPanelBounds,
                        linkedPlotAreaBounds = plotAreaBounds,
                        localCropPath = file.absolutePath.takeIf { saved },
                        source = PeakLabelEvidenceSource.ML_KIT,
                        confidence = element.confidence,
                        status = if (element.confidence >= 0.55f) {
                            PeakLabelEvidenceStatus.VALID_TEXT
                        } else {
                            PeakLabelEvidenceStatus.AMBIGUOUS_TEXT
                        },
                        isRuntimeEvidence = true,
                        warnings = buildList {
                            add("peak_label_ocr.local_crop:${crop.kind}")
                            if (crop.insidePlotArea) add("peak_label_ocr.crop_inside_plot_area_requires_signal_verification")
                        },
                    )
                }
            }
            PeakLabelEvidenceResult(
                labels = labels
                    .distinctBy { "${it.parsedRetentionTime}:${it.labelBoxPx?.x}:${it.labelBoxPx?.y}" }
                    .sortedBy { it.parsedRetentionTime ?: Double.POSITIVE_INFINITY },
                cropPaths = cropPaths.distinct(),
                warnings = buildList {
                    if (labels.isEmpty()) add("peak_label_ocr.no_peak_labels")
                    if (cropPaths.isEmpty()) add("peak_label_ocr.no_crops_written")
                    add("peak_label_ocr.runtime_local_crops_used")
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
    val match = Regex("""(?<!\d)(\d{1,3}[.,]\d{2,4})(?!\d)""").find(text.replace(" ", ""))
        ?: return null
    return match.groupValues[1].replace(',', '.').toDoubleOrNull()
}

private fun GraphRegion.clampedTo(imageWidth: Int, imageHeight: Int): GraphRegion {
    val safeX = x.coerceIn(0, (imageWidth - 1).coerceAtLeast(0))
    val safeY = y.coerceIn(0, (imageHeight - 1).coerceAtLeast(0))
    val safeRight = right.coerceIn(safeX + 1, imageWidth.coerceAtLeast(safeX + 1))
    val safeBottom = bottom.coerceIn(safeY + 1, imageHeight.coerceAtLeast(safeY + 1))
    return copy(x = safeX, y = safeY, width = safeRight - safeX, height = safeBottom - safeY)
}
