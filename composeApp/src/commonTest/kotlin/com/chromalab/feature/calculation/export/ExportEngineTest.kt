package com.chromalab.feature.calculation.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportEngineTest {

    @Test
    fun peakCsvUsesDotDecimalAndEscapesTextCells() {
        val csv = PeaksCsvExporter.export(
            listOf(
                ExportPeak(
                    peakId = 1,
                    status = "AUTO",
                    rtApex = 12.34567,
                    rtCentroid = 12.45678,
                    height = 1234.5,
                    area = 4567.89,
                    widthBase = 0.12345,
                    widthHalfHeight = 0.06789,
                    prominence = 98.765,
                    snr = 12.3,
                    snrMethod = "MAD",
                    baselineMethod = "ALS",
                    integrationMethod = "TRAPEZOIDAL",
                    confidenceGrade = "HIGH",
                    confidenceScore = 0.9876,
                    overlapStatus = "ISOLATED",
                    boundaryMethod = "LOCAL_MINIMA",
                    leftBoundary = 11.1,
                    rightBoundary = 13.2,
                    positiveArea = 4567.89,
                    negativeArea = 0.0,
                    isManuallyEdited = false,
                    warnings = listOf("contains, comma", "contains \"quote\""),
                    tailingFactor = 1.2345,
                    asymmetryFactor = 1.1111,
                    plateCount = 12345,
                    resolution = 2.3456,
                    areaPercent = 100.0,
                    compoundName = "C10, test",
                    compoundSource = "AUTO_SERIES",
                ),
            ),
        )

        val row = csv.lines()[1]

        assertTrue("12.3457" in row)
        assertTrue("0.988" in row)
        assertTrue("\"C10, test\"" in row)
        assertTrue("\"contains, comma; contains \"\"quote\"\"\"" in row)
        assertFalse("12,3457" in row)
    }

    @Test
    fun signalCsvKeepsTwoColumns() {
        val csv = SignalCsvExporter.export(
            listOf(
                ExportPoint(time = 1.2345678, intensity = -0.12345),
                ExportPoint(time = 2.0, intensity = 10.0),
            ),
            label = "Corrected",
        )

        assertEquals("Time,Corrected", csv.lines()[0])
        assertEquals("1.234568,-0.1234", csv.lines()[1])
        assertEquals("2.000000,10.0000", csv.lines()[2])
    }
}
