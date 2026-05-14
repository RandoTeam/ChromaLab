package com.chromalab.feature.processing.fixtures

import com.chromalab.feature.processing.bench.OfflineAnalysisAudit
import com.chromalab.feature.processing.bench.OfflineAnalysisAuditArtifacts
import com.chromalab.feature.processing.bench.OfflineAnalysisInput
import com.chromalab.feature.processing.bench.OfflineAnalysisRunner
import com.chromalab.feature.processing.bench.OfflineStageStatus
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChromatogramBenchFixtureTest {

    @Test
    fun benchResourcesMatchManifestIdentity() {
        ChromatogramBenchFixtures.all.forEach { fixture ->
            val bytes = fixture.resourceBytes()

            assertEquals(fixture.sizeBytes, bytes.size, "${fixture.id} byte size changed")
            assertEquals(fixture.sha256, bytes.sha256(), "${fixture.id} SHA-256 changed")

            val image = ImageIO.read(ByteArrayInputStream(bytes))
            assertNotNull(image, "${fixture.id} must remain a readable image")
            assertEquals(fixture.width, image.width, "${fixture.id} width changed")
            assertEquals(fixture.height, image.height, "${fixture.id} height changed")
        }
    }

    @Test
    fun benchExpectedFactsRemainExplicit() {
        assertEquals(8, ChromatogramBenchFixtures.all.size)

        ChromatogramBenchFixtures.all.forEach { fixture ->
            assertTrue(fixture.expectedGraphCount >= 1, "${fixture.id} must define graph count")
            assertTrue(fixture.tags.isNotEmpty(), "${fixture.id} must define fixture tags")
            assertEquals("not_locked", fixture.numericTruthStatus)
        }

        val byId = ChromatogramBenchFixtures.all.associateBy { it.id }
        assertEquals(2, byId.getValue("bench_01_mz71_screenshot_page").expectedGraphCount)
        assertTrue(byId.getValue("bench_01_mz71_screenshot_page").strictGraphCount)
        assertEquals(4, byId.getValue("bench_04_stacked_xic_resolution").expectedGraphCount)
        assertEquals(4, byId.getValue("bench_05_tic_plus_ions").expectedGraphCount)
        assertEquals(2, byId.getValue("bench_06_photo_two_graphs_page").expectedGraphCount)
        assertTrue(byId.getValue("bench_07_rotated_page_photo").requiresRotationCorrection)
        assertTrue("near_duplicate_candidate" in byId.getValue("bench_08_mz71_duplicate_candidate").tags)
    }

    @Test
    fun offlineRunnerProducesStageAuditForEveryBenchFixture() = runBlocking {
        val runner = OfflineAnalysisRunner()
        val root = Files.createTempDirectory("chromalab-bench-runner")

        ChromatogramBenchFixtures.all.forEach { fixture ->
            val inputPath = root.resolve("${fixture.id}.${fixture.extension}")
            Files.write(inputPath, fixture.resourceBytes())
            val outputDir = root.resolve("${fixture.id}_out")

            val audit = runner.run(
                OfflineAnalysisInput(
                    sourceId = fixture.id,
                    imagePath = inputPath.toAbsolutePath().toString(),
                    outputDir = outputDir.toAbsolutePath().toString(),
                    expectedGraphCount = fixture.expectedGraphCount,
                )
            )

            assertEquals(fixture.width, audit.imageWidth, "${fixture.id} normalized width")
            assertEquals(fixture.height, audit.imageHeight, "${fixture.id} normalized height")
            assertTrue(audit.stages.any { it.stage == "normalize" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.stages.any { it.stage == "graph_region" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.graphCandidates.isNotEmpty(), "${fixture.id} must expose graph candidate audit")
            assertTrue(audit.graphs.isNotEmpty(), "${fixture.id} must expose per-graph audit")
            assertTrue(audit.blockedAtStage != null, "${fixture.id} should be blocked honestly until desktop curve extraction exists")

            writeAuditArtifacts(audit, imagePath = inputPath, outputDir = outputDir)
            assertTrue(Files.size(outputDir.resolve("audit.json")) > 0L, "${fixture.id} audit JSON must be written")
            assertTrue(Files.size(outputDir.resolve("audit_summary.md")) > 0L, "${fixture.id} audit summary must be written")
            assertTrue(Files.size(outputDir.resolve("graph_candidates.png")) > 0L, "${fixture.id} graph overlay must be written")

            if (fixture.strictGraphCount) {
                assertEquals(fixture.expectedGraphCount, audit.detectedGraphCount, "${fixture.id} detected graph count")
            } else if (audit.detectedGraphCount != fixture.expectedGraphCount) {
                assertTrue(
                    audit.warnings.any { it.startsWith("graph.count_mismatch.") },
                    "${fixture.id} must expose current desktop graph split limitation",
                )
            }
        }
    }
}

private object ChromatogramBenchFixtures {
    val all: List<ChromatogramBenchFixture> = listOf(
        ChromatogramBenchFixture(
            id = "bench_01_mz71_screenshot_page",
            resourcePath = "$RESOURCE_ROOT/bench_01_mz71_screenshot_page.jpg",
            sha256 = "8EB64774D29F93C1CCB3D4F9035C96F9075121551600B2814BA7966021ECFD01",
            sizeBytes = 128_010,
            width = 964,
            height = 1280,
            expectedGraphCount = 2,
            expectedIon = "m/z 217 and m/z 218",
            expectedTitle = "Ion 217.00 / Ion 218.00: 0301002.D",
            strictGraphCount = true,
            tags = setOf("printed_page_photo", "two_graphs", "m_z_217_218", "crop_required"),
        ),
        ChromatogramBenchFixture(
            id = "bench_02_mz92_belyi_tigr",
            resourcePath = "$RESOURCE_ROOT/bench_02_mz92_belyi_tigr.jpg",
            sha256 = "D1F0A55F6491E6FA7E3857086FDCCE97CDD3723A4F786D40000480F9A4B8BDFE",
            sizeBytes = 57_090,
            width = 576,
            height = 1280,
            expectedGraphCount = 1,
            expectedIon = "m/z 92.00",
            expectedTitle = "Ion 92.00 (91.70 to 92.70): BELIY TIGR_1.D\\data.ms",
            tags = setOf("phone_screenshot", "document_context", "m_z_92", "belyi_tigr_reference"),
        ),
        ChromatogramBenchFixture(
            id = "bench_03_small_tic_export",
            resourcePath = "$RESOURCE_ROOT/bench_03_small_tic_export.jpg",
            sha256 = "C36A405C937741C6DE834AD9FD3196E658CE42ECBA03AFBBF1D2B47D00437DF4",
            sizeBytes = 6_682,
            width = 381,
            height = 132,
            expectedGraphCount = 1,
            expectedIon = null,
            expectedTitle = "TIC Scan CK-1.D",
            tags = setOf("small_export", "tic", "low_resolution", "labeled_peaks"),
        ),
        ChromatogramBenchFixture(
            id = "bench_04_stacked_xic_resolution",
            resourcePath = "$RESOURCE_ROOT/bench_04_stacked_xic_resolution.png",
            sha256 = "27D998C2ACA33B12DC3700BFCC88FC3EFB15FAF2A3FE51317AFD220F0E2C3C25",
            sizeBytes = 165_615,
            width = 534,
            height = 1110,
            expectedGraphCount = 4,
            expectedIon = "XIC 198.0315",
            expectedTitle = null,
            tags = setOf("stacked_graphs", "xic", "seconds_axis", "multiple_mass_windows"),
        ),
        ChromatogramBenchFixture(
            id = "bench_05_tic_plus_ions",
            resourcePath = "$RESOURCE_ROOT/bench_05_tic_plus_ions.png",
            sha256 = "3F715862316D9D24394F18CC21DFA4C42D6BAC694DD4E1DD8F743BF94984E1F9",
            sizeBytes = 162_230,
            width = 683,
            height = 807,
            expectedGraphCount = 4,
            expectedIon = "m/z 326/360/394",
            expectedTitle = "TIC: NERPA1.D",
            tags = setOf("multi_panel", "tic", "ion_traces", "russian_labels"),
        ),
        ChromatogramBenchFixture(
            id = "bench_06_photo_two_graphs_page",
            resourcePath = "$RESOURCE_ROOT/bench_06_photo_two_graphs_page.jpg",
            sha256 = "04B95E7D8B992FE31708AEAB46E6582329AF1F48B54DB300EF4B664A4EDCB090",
            sizeBytes = 106_950,
            width = 964,
            height = 1280,
            expectedGraphCount = 2,
            expectedIon = "m/z 83 and m/z 92",
            expectedTitle = null,
            tags = setOf("printed_page_photo", "two_graphs", "perspective_distortion", "crop_required"),
        ),
        ChromatogramBenchFixture(
            id = "bench_07_rotated_page_photo",
            resourcePath = "$RESOURCE_ROOT/bench_07_rotated_page_photo.jpg",
            sha256 = "83B60C45E6D9C66BAC5B60A150EE9B283D9BC07E6B5EF4FDF8BD7A107EC3A105",
            sizeBytes = 103_875,
            width = 1280,
            height = 964,
            expectedGraphCount = 1,
            expectedIon = "m/z 71.00",
            expectedTitle = "Ion 71.00 (70.70 to 71.70): BELIY TIGR_1.D\\data.ms",
            requiresRotationCorrection = true,
            tags = setOf("rotated_page", "printed_page_photo", "orientation_correction", "m_z_71"),
        ),
        ChromatogramBenchFixture(
            id = "bench_08_mz71_duplicate_candidate",
            resourcePath = "$RESOURCE_ROOT/bench_08_mz71_duplicate_candidate.jpg",
            sha256 = "C1EF5E8BC1BD3BB3F9921A9A102FE8B3BADB7ED6BF9EC5E49DFD6EE54D15E0C6",
            sizeBytes = 70_617,
            width = 576,
            height = 1280,
            expectedGraphCount = 1,
            expectedIon = "m/z 71.00",
            expectedTitle = "Ion 71.00 (70.70 to 71.70): BELIY TIGR_1.D\\data.ms",
            tags = setOf("phone_screenshot", "document_context", "m_z_71", "near_duplicate_candidate"),
        ),
    )

    private const val RESOURCE_ROOT = "fixtures/chromatogram_bench"
}

private data class ChromatogramBenchFixture(
    val id: String,
    val resourcePath: String,
    val sha256: String,
    val sizeBytes: Int,
    val width: Int,
    val height: Int,
    val expectedGraphCount: Int,
    val expectedIon: String?,
    val expectedTitle: String?,
    val requiresRotationCorrection: Boolean = false,
    val strictGraphCount: Boolean = expectedGraphCount == 1,
    val numericTruthStatus: String = "not_locked",
    val tags: Set<String>,
) {
    val extension: String
        get() = resourcePath.substringAfterLast('.')

    fun resourceBytes(): ByteArray {
        val stream = requireNotNull(
            Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath),
        ) {
            "Missing chromatogram bench fixture resource: $resourcePath"
        }
        return stream.use { it.readBytes() }
    }
}

private fun ByteArray.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> "%02X".format(byte) }

private fun writeAuditArtifacts(
    audit: OfflineAnalysisAudit,
    imagePath: Path,
    outputDir: Path,
) {
    Files.createDirectories(outputDir)
    Files.writeString(outputDir.resolve("audit.json"), OfflineAnalysisAuditArtifacts.toJson(audit))
    Files.writeString(outputDir.resolve("audit_summary.md"), OfflineAnalysisAuditArtifacts.toSummaryMarkdown(audit))
    writeGraphCandidateOverlay(audit, imagePath, outputDir.resolve("graph_candidates.png"))
}

private fun writeGraphCandidateOverlay(
    audit: OfflineAnalysisAudit,
    imagePath: Path,
    outputPath: Path,
) {
    val source = assertNotNull(
        ImageIO.read(imagePath.toFile()),
        "${audit.sourceId} source image must be readable for overlay",
    )
    val image = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        graphics.drawImage(source, 0, 0, null)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.stroke = BasicStroke((source.width.coerceAtLeast(source.height) / 240f).coerceAtLeast(2f))
        graphics.font = Font(Font.SANS_SERIF, Font.BOLD, (source.width / 34).coerceIn(12, 28))

        audit.graphCandidates.forEach { candidate ->
            val color = if (candidate.accepted) Color(0x1B, 0x8A, 0x3A, 220) else Color(0xD3, 0x2F, 0x2F, 220)
            graphics.color = color
            val region = candidate.region
            graphics.drawRect(region.x, region.y, region.width.coerceAtLeast(1), region.height.coerceAtLeast(1))
            graphics.drawString(
                "#${candidate.graphIndex} ${if (candidate.accepted) "accepted" else "rejected"}",
                (region.x + 8).coerceAtMost(source.width - 1),
                (region.y + 24).coerceIn(24, source.height - 1),
            )
        }

        graphics.color = Color(0x15, 0x65, 0xC0, 230)
        graphics.stroke = BasicStroke((source.width.coerceAtLeast(source.height) / 140f).coerceAtLeast(3f))
        audit.graphs.forEach { graph ->
            val region = graph.region
            graphics.drawRect(region.x, region.y, region.width.coerceAtLeast(1), region.height.coerceAtLeast(1))
        }
    } finally {
        graphics.dispose()
    }

    ImageIO.write(image, "png", outputPath.toFile())
}
