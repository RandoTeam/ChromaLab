package com.chromalab.feature.processing.peaks

import com.chromalab.feature.processing.graph.GraphRegion

actual class PeakLabelEvidenceReader actual constructor() {
    actual suspend fun readPeakLabels(
        imagePath: String,
        outputDir: String,
        graphPanelBounds: GraphRegion,
        plotAreaBounds: GraphRegion,
    ): PeakLabelEvidenceResult =
        PeakLabelEvidenceResult(
            warnings = listOf("peak_label_ocr.desktop_runtime_reader_not_configured"),
        )
}
