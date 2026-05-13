package com.chromalab.feature.knowledge

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KovatsIndexCalculatorTest {

    @Test
    fun temperatureProgrammedFormulaInterpolatesBetweenAdjacentNParaffins() {
        val result = KovatsIndexCalculator.calculate(
            KovatsIndexCalculationInput(
                targetRetentionTime = 12.5,
                referenceRetentions = listOf(
                    NParaffinReferenceRetention(carbonNumber = 7, retentionTime = 10.0),
                    NParaffinReferenceRetention(carbonNumber = 8, retentionTime = 15.0),
                ),
                formula = KovatsIndexFormula.VAN_DEN_DOOL_KRATZ_LINEAR,
            ),
        )

        assertTrue(result.isCalculated)
        assertEquals(KovatsIndexCalculationKind.INTERPOLATED, result.kind)
        assertEquals(750.0, result.value)
        assertEquals(7, result.lowerReference?.carbonNumber)
        assertEquals(8, result.upperReference?.carbonNumber)
    }

    @Test
    fun isothermalFormulaUsesLogInterpolation() {
        val result = KovatsIndexCalculator.calculate(
            KovatsIndexCalculationInput(
                targetRetentionTime = sqrt(200.0),
                referenceRetentions = listOf(
                    NParaffinReferenceRetention(carbonNumber = 7, retentionTime = 10.0),
                    NParaffinReferenceRetention(carbonNumber = 8, retentionTime = 20.0),
                ),
                formula = KovatsIndexFormula.KOVATS_ISOTHERMAL_LOG,
            ),
        )

        assertTrue(result.isCalculated)
        assertEquals(KovatsIndexCalculationKind.INTERPOLATED, result.kind)
        assertEquals(750.0, result.value?.let { kotlin.math.round(it * 1_000_000.0) / 1_000_000.0 })
    }

    @Test
    fun exactReferenceRetentionReturnsDirectReferenceIndex() {
        val result = KovatsIndexCalculator.calculate(
            KovatsIndexCalculationInput(
                targetRetentionTime = 15.0,
                referenceRetentions = listOf(
                    NParaffinReferenceRetention(carbonNumber = 7, retentionTime = 10.0),
                    NParaffinReferenceRetention(carbonNumber = 8, retentionTime = 15.0),
                    NParaffinReferenceRetention(carbonNumber = 9, retentionTime = 21.0),
                ),
            ),
        )

        assertTrue(result.isCalculated)
        assertEquals(KovatsIndexCalculationKind.DIRECT_REFERENCE, result.kind)
        assertEquals(800.0, result.value)
        assertEquals(8, result.lowerReference?.carbonNumber)
        assertEquals(8, result.upperReference?.carbonNumber)
    }

    @Test
    fun missingAdjacentNParaffinReferenceBlocksCalculation() {
        val result = KovatsIndexCalculator.calculate(
            KovatsIndexCalculationInput(
                targetRetentionTime = 15.0,
                referenceRetentions = listOf(
                    NParaffinReferenceRetention(carbonNumber = 7, retentionTime = 10.0),
                    NParaffinReferenceRetention(carbonNumber = 9, retentionTime = 20.0),
                ),
            ),
        )

        assertEquals(KovatsIndexCalculationStatus.MISSING_ADJACENT_REFERENCE, result.status)
        assertEquals(KovatsIndexCalculationKind.NOT_CALCULABLE, result.kind)
        assertNull(result.value)
        assertEquals(7, result.lowerReference?.carbonNumber)
        assertEquals(9, result.upperReference?.carbonNumber)
    }

    @Test
    fun targetOutsideReferenceRangeBlocksCalculation() {
        val result = KovatsIndexCalculator.calculate(
            KovatsIndexCalculationInput(
                targetRetentionTime = 7.5,
                referenceRetentions = listOf(
                    NParaffinReferenceRetention(carbonNumber = 7, retentionTime = 10.0),
                    NParaffinReferenceRetention(carbonNumber = 8, retentionTime = 15.0),
                ),
            ),
        )

        assertEquals(KovatsIndexCalculationStatus.TARGET_OUTSIDE_REFERENCE_RANGE, result.status)
        assertNull(result.value)
    }

    @Test
    fun invalidReferenceOrderingBlocksCalculation() {
        val result = KovatsIndexCalculator.calculate(
            KovatsIndexCalculationInput(
                targetRetentionTime = 12.5,
                referenceRetentions = listOf(
                    NParaffinReferenceRetention(carbonNumber = 7, retentionTime = 15.0),
                    NParaffinReferenceRetention(carbonNumber = 8, retentionTime = 10.0),
                ),
            ),
        )

        assertEquals(KovatsIndexCalculationStatus.INVALID_REFERENCE_SERIES, result.status)
        assertNull(result.value)
    }
}
