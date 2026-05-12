package com.chromalab.feature.processing.report

import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.InputSourceType
import com.chromalab.feature.reports.ModelExecutionInfo
import com.chromalab.feature.reports.PixelRect
import com.chromalab.feature.reports.ProcessingMode
import com.chromalab.feature.reports.ReportStageTiming
import com.chromalab.feature.reports.StoredReportMetadataCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProcessingReportMetadataBuilderTest {

    @Test
    fun processingMetadataConfigContainsStableReportEnvelope() {
        val config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\input\raw_photo.jpg""",
            processedPath = """C:\input\normalized_photo.jpg""",
            sourceType = SourceType.PHOTO,
            graphIndex = 2,
            detectedGraphCount = 3,
            signalPointCount = 512,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_750L,
            sourceImageBounds = PixelRect(0, 0, 1200, 800),
            detectedGraphBounds = PixelRect(120, 160, 900, 420),
            cropConfidence = 0.82,
            preprocessingSteps = listOf("EXIF normalized", "Auto-sweep selected config: high_contrast"),
            scanMode = "photo-processing-flow/high_contrast",
            titleOcrConfidence = null,
            axisOcrConfidence = 0.76,
            tickOcrConfidence = 0.88,
            selectedModel = ModelExecutionInfo(
                modelId = "gemma-litert",
                modelName = "Gemma LiteRT",
                runtime = ExecutedRuntime.LITERT,
            ),
            executedModel = ModelExecutionInfo(
                modelId = "gemma-litert",
                modelName = "Gemma LiteRT",
                runtime = ExecutedRuntime.LITERT,
                backendLabel = "LiteRT GPU",
            ),
            deviceName = "Pixel Test",
            stageTimings = listOf(
                ReportStageTiming("IMAGE_QUALITY", "IMAGE_QUALITY", 125L),
                ReportStageTiming("OCR_SUGGESTION", "OCR_SUGGESTION", 450L),
            ),
        )

        val metadata = StoredReportMetadataCodec.decodeOrNull(config)

        assertNotNull(metadata)
        assertEquals(InputSourceType.CAMERA_CAPTURE, metadata.inputSourceType)
        assertEquals("normalized_photo.jpg", metadata.sourceName)
        assertEquals(3, metadata.detectedGraphCount)
        assertEquals(1_750L, metadata.totalAnalysisDurationMillis)
        assertEquals("gemma-litert", metadata.selectedModel?.modelId)
        assertEquals("LiteRT GPU", metadata.executedModel?.backendLabel)
        assertEquals(ExecutedRuntime.LITERT, metadata.executedRuntime)
        assertEquals("Pixel Test", metadata.deviceName)
        assertEquals(ProcessingMode.FULL_ANALYSIS, metadata.processingMode)
        assertEquals(2, metadata.stageTimings.size)
        assertEquals(450L, metadata.stageTimings.last().durationMillis)

        val graph = metadata.graphs.single()
        assertEquals(2, graph.graphIndex)
        assertEquals("photo-processing-flow/high_contrast", graph.source?.scanMode)
        assertEquals(PixelRect(0, 0, 1200, 800), graph.source?.sourceImageBounds)
        assertEquals(PixelRect(120, 160, 900, 420), graph.source?.detectedGraphBounds)
        assertEquals(0.82, graph.source?.cropConfidence)
        assertEquals(null, graph.source?.titleOcrConfidence)
        assertEquals(0.76, graph.source?.axisOcrConfidence)
        assertEquals(0.88, graph.source?.tickOcrConfidence)
        assertTrue(
            graph.source?.preprocessingSteps.orEmpty()
                .contains("Digitized signal points: 512"),
        )
        assertTrue(
            graph.source?.preprocessingSteps.orEmpty()
                .contains("Auto-sweep selected config: high_contrast"),
        )
    }
}
