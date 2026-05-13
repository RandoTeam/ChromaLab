package com.chromalab.feature.calculation.algorithm

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BelyiTigrRealPhotoFixtureTest {

    @Test
    fun photoAssetMatchesReferenceIdentity() {
        val fixture = BelyiTigrIon92RealPhotoFixture
        val bytes = fixture.resourceBytes()

        assertEquals(fixture.sizeBytes, bytes.size)
        assertEquals(fixture.sha256, bytes.sha256())

        val image = ImageIO.read(ByteArrayInputStream(bytes))
        assertNotNull(image, "Belyi Tigr Ion 92 fixture must remain a readable JPEG.")
        assertEquals(fixture.width, image.width)
        assertEquals(fixture.height, image.height)
    }

    @Test
    fun realPhotoFixtureKeepsAnalysisContractExplicit() {
        val fixture = BelyiTigrIon92RealPhotoFixture

        assertEquals("not_locked", fixture.numericTruthStatus)
        assertFalse(fixture.locksPeakMetrics)
        assertTrue(fixture.requiresGraphCrop)
        assertEquals(1, fixture.expectedGraphCount)
        assertEquals("m/z 92.00", fixture.ion)
        assertEquals("91.70 to 92.70", fixture.ionRange)
        assertEquals(
            "Ion 92.00 (91.70 to 92.70): BELIY TIGR_1.D\\data.ms",
            fixture.titleApprox,
        )
        assertTrue(
            fixture.requiredGraphPreparationArtifacts.containsAll(
                listOf(
                    "source_image_bounds",
                    "detected_graph_bounds",
                    "selected_preprocessing_variant",
                    "rejected_preprocessing_variants",
                ),
            ),
        )
        assertTrue(fixture.requiredWarnings.contains("dominant_peak_reference_discrepancy"))
        assertTrue(fixture.requiredWarnings.contains("low_confidence_crop"))
        assertTrue(fixture.requiredWarnings.contains("low_confidence_axis_extraction"))
    }
}

private object BelyiTigrIon92RealPhotoFixture {
    const val resourcePath: String = "fixtures/belyi_tigr_ion92/photo_2026-05-10_06-16-01.jpg"
    const val sha256: String = "D1F0A55F6491E6FA7E3857086FDCCE97CDD3723A4F786D40000480F9A4B8BDFE"
    const val sizeBytes: Int = 57_090
    const val width: Int = 576
    const val height: Int = 1280
    const val numericTruthStatus: String = "not_locked"
    const val locksPeakMetrics: Boolean = false
    const val requiresGraphCrop: Boolean = true
    const val expectedGraphCount: Int = 1
    const val ion: String = "m/z 92.00"
    const val ionRange: String = "91.70 to 92.70"
    const val titleApprox: String = "Ion 92.00 (91.70 to 92.70): BELIY TIGR_1.D\\data.ms"

    val requiredGraphPreparationArtifacts: Set<String> = setOf(
        "source_image_bounds",
        "detected_graph_bounds",
        "selected_preprocessing_variant",
        "rejected_preprocessing_variants",
    )

    val requiredWarnings: Set<String> = setOf(
        "low_confidence_crop",
        "low_confidence_axis_extraction",
        "dominant_peak_reference_discrepancy",
    )

    fun resourceBytes(): ByteArray {
        val stream = requireNotNull(
            Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath),
        ) {
            "Missing real-photo fixture resource: $resourcePath"
        }
        return stream.use { it.readBytes() }
    }
}

private fun ByteArray.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> "%02X".format(byte) }
