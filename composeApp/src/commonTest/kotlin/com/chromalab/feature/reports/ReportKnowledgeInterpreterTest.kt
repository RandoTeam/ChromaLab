package com.chromalab.feature.reports

import com.chromalab.feature.knowledge.ChromaLabBaseKnowledgePack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportKnowledgeInterpreterTest {

    @Test
    fun matchesBelyiTigrMz92ChannelAsClassHypothesisOnly() {
        val context = ReportKnowledgeInterpreter.interpret(
            identification = ChromatogramIdentification(
                chromatogramTitle = detectedText("Ion 92.00 (91.70 to 92.70): BELIY TIGR_1.D\\data.ms"),
                analysisType = detectedText("GC-MS"),
                chromatogramMode = detectedText("EIC"),
                ionOrChannel = detectedText("m/z 92.00"),
                ionRange = detectedText("91.70 to 92.70"),
            ),
            pack = ChromaLabBaseKnowledgePack.pack,
        )

        assertEquals("gc-ms-ei-eic", context.chromatogramType?.id)
        assertEquals("ei-mz-92", context.ionFragment?.id)
        assertEquals("monoalkylbenzenes", context.compoundClasses.single().id)
        assertEquals("Monocyclic alkylbenzenes", context.likelyCompoundClass.value)
        assertEquals(ReportValueStatus.INFERRED, context.likelyCompoundClass.status)
        assertTrue(context.assignmentCautions.any { it.contains("m/z 92 alone") })
        assertTrue(context.assignmentCautions.any { it.contains("channel/class evidence only") })
    }

    @Test
    fun parsesCommaDecimalExactMassXicWithoutInventingCompoundName() {
        val context = ReportKnowledgeInterpreter.interpret(
            identification = ChromatogramIdentification(
                analysisType = detectedText("GC-MS"),
                chromatogramMode = detectedText("XIC"),
                ionOrChannel = detectedText("XIC(198,0315+/-0,2)"),
            ),
            pack = ChromaLabBaseKnowledgePack.pack,
        )

        assertEquals("gc-ms-ei-xic", context.chromatogramType?.id)
        assertEquals("ei-mz-198-0315", context.ionFragment?.id)
        assertEquals("method-targeted-extracts", context.compoundClasses.single().id)
        assertEquals("Method-targeted extracted channels", context.likelyCompoundClass.value)
        assertTrue(context.assignmentCautions.any { it.contains("method target") })
    }

    @Test
    fun estimatesChannelCenterFromIonRangeWhenChannelLabelIsMissing() {
        val context = ReportKnowledgeInterpreter.interpret(
            identification = ChromatogramIdentification(
                analysisType = detectedText("GC-MS"),
                chromatogramMode = detectedText("EIC"),
                ionRange = detectedText("91.70 to 92.70"),
            ),
            pack = ChromaLabBaseKnowledgePack.pack,
        )

        assertEquals("ei-mz-92", context.ionFragment?.id)
    }

    private fun detectedText(value: String): ReportTextValue =
        ReportTextValue(
            value = value,
            status = ReportValueStatus.DETECTED,
            confidence = 0.90,
            source = ReportValueSource.OCR,
        )
}
