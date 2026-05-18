package com.chromalab.feature.processing.peaks

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeakLabelTextHeuristicsTest {
    @Test
    fun ionRangeTitleIsNotAPeakLabel() {
        assertTrue(PeakLabelTextHeuristics.isTitleOrIonChannelText("Ion 71.00 (70.70 to 71.70): BELYI TIGR_1.Data.ms"))
        assertTrue(PeakLabelTextHeuristics.isTitleOrIonChannelText("71.70); BELIY TIGR_1.Data.ms"))
    }

    @Test
    fun simpleRetentionTimeLabelIsNotTitleOrChannelText() {
        assertFalse(PeakLabelTextHeuristics.isTitleOrIonChannelText("5.610"))
        assertFalse(PeakLabelTextHeuristics.isTitleOrIonChannelText("8.560"))
    }
}
