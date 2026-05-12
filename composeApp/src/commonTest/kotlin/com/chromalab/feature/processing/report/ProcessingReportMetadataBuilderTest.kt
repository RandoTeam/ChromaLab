package com.chromalab.feature.processing.report

import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.InputSourceType
import com.chromalab.feature.reports.ProcessingMode
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
        )

        val metadata = StoredReportMetadataCodec.decodeOrNull(config)

        assertNotNull(metadata)
        assertEquals(InputSourceType.CAMERA_CAPTURE, metadata.inputSourceType)
        assertEquals("normalized_photo.jpg", metadata.sourceName)
        assertEquals(3, metadata.detectedGraphCount)
        assertEquals(1_750L, metadata.totalAnalysisDurationMillis)
        assertEquals(ExecutedRuntime.UNKNOWN, metadata.executedRuntime)
        assertEquals(ProcessingMode.FULL_ANALYSIS, metadata.processingMode)

        val graph = metadata.graphs.single()
        assertEquals(2, graph.graphIndex)
        assertEquals("photo-processing-flow", graph.source?.scanMode)
        assertTrue(
            graph.source?.preprocessingSteps.orEmpty()
                .contains("Digitized signal points: 512"),
        )
    }
}
