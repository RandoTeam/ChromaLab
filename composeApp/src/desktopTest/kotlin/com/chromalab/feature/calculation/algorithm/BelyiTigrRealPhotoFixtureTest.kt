package com.chromalab.feature.calculation.algorithm

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.math.roundToInt
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

    @Test
    fun earlyDominantPeakTensionIsVisibleInRealPhoto() {
        val fixture = BelyiTigrIon92RealPhotoFixture
        val image = ImageIO.read(ByteArrayInputStream(fixture.resourceBytes()))
        assertNotNull(image, "Belyi Tigr Ion 92 fixture must remain a readable JPEG.")

        val earlyFeature = fixture.plotAudit.observeTallestDarkFeature(
            image = image,
            window = TimeWindow(startMinutes = 8.0, endMinutes = 20.0),
        )
        val referenceClaimWindowFeature = fixture.plotAudit.observeTallestDarkFeature(
            image = image,
            window = TimeWindow(startMinutes = 48.0, endMinutes = 50.5),
        )

        assertTrue(
            earlyFeature.timeMinutes < 20.0,
            "Expected the visually dominant fixture feature to be early, but found $earlyFeature.",
        )
        assertTrue(
            referenceClaimWindowFeature.timeMinutes in 48.0..50.5,
            "Expected the comparison feature to stay inside the reference claim window, but found $referenceClaimWindowFeature.",
        )
        assertTrue(
            earlyFeature.heightPixels >= referenceClaimWindowFeature.heightPixels * 2,
            "Reference claim must remain flagged: early=$earlyFeature, referenceWindow=$referenceClaimWindowFeature.",
        )
        assertTrue(
            fixture.requiredWarnings.contains("dominant_peak_reference_discrepancy"),
            "Reports for this fixture must warn instead of copying the 49 min dominant-peak claim.",
        )
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
    val plotAudit: BelyiTigrPlotAudit = BelyiTigrPlotAudit(
        signalTopY = 500,
        plotBottomY = 860,
        tenMinuteTickX = 134,
        fiftyFiveMinuteTickX = 524,
        darkPixelThreshold = 120,
        minDarkPixelsPerColumn = 3,
    )

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

private data class TimeWindow(
    val startMinutes: Double,
    val endMinutes: Double,
)

private data class SignalFeature(
    val x: Int,
    val topY: Int,
    val heightPixels: Int,
    val timeMinutes: Double,
)

private data class BelyiTigrPlotAudit(
    val signalTopY: Int,
    val plotBottomY: Int,
    val tenMinuteTickX: Int,
    val fiftyFiveMinuteTickX: Int,
    val darkPixelThreshold: Int,
    val minDarkPixelsPerColumn: Int,
) {
    private val pixelsPerMinute: Double =
        (fiftyFiveMinuteTickX - tenMinuteTickX) / (55.0 - 10.0)

    fun observeTallestDarkFeature(image: BufferedImage, window: TimeWindow): SignalFeature {
        val startX = xForTime(window.startMinutes).coerceIn(0, image.width - 1)
        val endX = xForTime(window.endMinutes).coerceIn(startX, image.width - 1)
        var best: SignalFeature? = null

        for (x in startX..endX) {
            var topY: Int? = null
            var darkPixels = 0
            for (y in signalTopY until plotBottomY) {
                if (image.grayAt(x, y) < darkPixelThreshold) {
                    darkPixels += 1
                    if (topY == null) {
                        topY = y
                    }
                }
            }

            val candidateTopY = topY
            if (candidateTopY != null && darkPixels >= minDarkPixelsPerColumn) {
                val candidate = SignalFeature(
                    x = x,
                    topY = candidateTopY,
                    heightPixels = plotBottomY - candidateTopY,
                    timeMinutes = timeForX(x),
                )
                if (best == null || candidate.heightPixels > best.heightPixels) {
                    best = candidate
                }
            }
        }

        return requireNotNull(best) {
            "No dark signal feature found in ${window.startMinutes}..${window.endMinutes} min."
        }
    }

    private fun xForTime(minutes: Double): Int =
        (tenMinuteTickX + (minutes - 10.0) * pixelsPerMinute).roundToInt()

    private fun timeForX(x: Int): Double =
        10.0 + (x - tenMinuteTickX) / pixelsPerMinute
}

private fun BufferedImage.grayAt(x: Int, y: Int): Int {
    val argb = getRGB(x, y)
    val red = argb shr 16 and 0xFF
    val green = argb shr 8 and 0xFF
    val blue = argb and 0xFF
    return (red + green + blue) / 3
}
