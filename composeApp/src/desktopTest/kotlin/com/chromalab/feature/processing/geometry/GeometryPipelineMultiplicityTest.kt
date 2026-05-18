package com.chromalab.feature.processing.geometry

import java.io.File
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GeometryPipelineMultiplicityTest {
    @Test
    fun duplicateCandidateFixtureResolvesToOnePhysicalGraph() = runBlocking {
        val imageFile = File("src/desktopTest/resources/fixtures/chromatogram_bench/bench_08_mz71_duplicate_candidate.jpg")
        val image = ImageIO.read(imageFile)
        val outputDir = kotlin.io.path.createTempDirectory("chromalab_geometry_multiplicity").toFile()

        val result = GeometryPipelineRunner().run(
            imagePath = imageFile.absolutePath,
            outputDir = outputDir.absolutePath,
            imageWidth = image.width,
            imageHeight = image.height,
            runVlmHint = false,
            runTickOcr = false,
        )
        image.flush()
        outputDir.deleteRecursively()

        val multiplicity = result.trace.multiplicityResolution
        assertNotNull(multiplicity)
        assertEquals(GraphMultiplicityStatus.SINGLE_GRAPH, multiplicity.multiplicityStatus)
        assertEquals(1, multiplicity.resolvedGraphPanels.size)
    }
}
