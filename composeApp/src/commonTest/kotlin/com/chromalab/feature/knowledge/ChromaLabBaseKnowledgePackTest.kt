package com.chromalab.feature.knowledge

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChromaLabBaseKnowledgePackTest {

    @Test
    fun basePackValidatesAndRoundTrips() {
        val pack = ChromaLabBaseKnowledgePack.pack
        val validation = LocalKnowledgePackValidator.validate(pack)

        assertTrue(validation.isValid, validation.errors.joinToString { "${it.path}: ${it.message}" })
        assertTrue(validation.warnings.isEmpty())
        assertEquals(CURRENT_LOCAL_KNOWLEDGE_PACK_SCHEMA, pack.schemaVersion)

        val encoded = json.encodeToString(pack)
        val decoded = json.decodeFromString<LocalKnowledgePack>(encoded)

        assertEquals(pack, decoded)
        assertEquals("chromalab-base", decoded.id)
        assertTrue(decoded.chromatogramTypes.any { it.id == "gc-ms-ei-eic" })
        assertTrue(decoded.compoundClasses.any { it.id == "n-paraffins" })
        assertTrue(decoded.carbonNumberSeries.any { it.id == "n-paraffin-reference-series" })
    }

    @Test
    fun basePackCoversReferenceFixtureChromatogramModesAndIonChannels() {
        val pack = ChromaLabBaseKnowledgePack.pack
        val chromatogramTypeIds = pack.chromatogramTypes.map { it.id }.toSet()
        val ionIds = pack.ionFragments.map { it.id }.toSet()
        val compoundClassIds = pack.compoundClasses.map { it.id }.toSet()

        assertTrue(
            chromatogramTypeIds.containsAll(
                setOf("gc-ms-ei-tic", "gc-ms-ei-eic", "gc-ms-ei-xic", "gc-ms-ei-sim"),
            ),
        )
        assertTrue(
            ionIds.containsAll(
                setOf(
                    "ei-mz-71",
                    "ei-mz-83",
                    "ei-mz-92",
                    "ei-mz-217",
                    "ei-mz-218",
                    "ei-mz-198-0315",
                    "ei-mz-326",
                    "ei-mz-360",
                    "ei-mz-394",
                ),
            ),
        )
        assertTrue(
            compoundClassIds.containsAll(
                setOf("monoalkylbenzenes", "n-paraffins", "petroleum-biomarkers", "method-targeted-extracts"),
            ),
        )
    }

    @Test
    fun mz92IsConservativeTolueneCompatibleChannelNotStandaloneAssignment() {
        val pack = ChromaLabBaseKnowledgePack.pack
        val mz92 = pack.ionFragments.single { it.id == "ei-mz-92" }

        assertEquals(92.0, mz92.nominalMz)
        assertEquals(91.70, mz92.mzWindow?.minimum)
        assertEquals(92.70, mz92.mzWindow?.maximum)
        assertEquals("m/z", mz92.mzWindow?.unit)
        assertEquals(IonizationMode.ELECTRON_IONIZATION, mz92.ionization)
        assertEquals(listOf("monoalkylbenzenes"), mz92.diagnosticForCompoundClassIds)
        assertEquals(listOf("ei-mz-91"), mz92.relatedIonFragmentIds)

        assertTrue(
            mz92.interpretation.any {
                it.sourceIds == listOf("nist-toluene") &&
                    it.text.contains("toluene") &&
                    it.text.contains("92.1384")
            },
        )
        assertTrue(
            mz92.cautions.any {
                it.confidence == 1.0 &&
                    it.text.contains("m/z 92 alone") &&
                    it.text.contains("retention index")
            },
        )
    }

    @Test
    fun alkylbenzeneSeedLibraryIncludesC7AndC8ReferenceEntries() {
        val pack = ChromaLabBaseKnowledgePack.pack
        val library = pack.kovatsLibraries.single { it.id == "nist-nonpolar-ramp-alkylbenzene-seed" }
        val entriesByName = library.entries.associateBy { it.compoundName }

        assertEquals("gc-ms-ei-eic", library.chromatogramTypeId)
        assertEquals("monoalkylbenzene-carbon-series", library.referenceSeriesId)
        assertEquals(setOf("Toluene", "Ethylbenzene", "m-Xylene", "p-Xylene", "o-Xylene"), entriesByName.keys)

        val toluene = entriesByName.getValue("Toluene")
        assertEquals("C7H8", toluene.formula)
        assertEquals(7, toluene.carbonNumber)
        assertEquals(KnowledgeEvidence.LITERATURE, toluene.evidence)
        assertTrue(toluene.kovatsRange?.isValid() == true)

        listOf("Ethylbenzene", "m-Xylene", "p-Xylene", "o-Xylene").forEach { compound ->
            val entry = entriesByName.getValue(compound)
            assertEquals("C8H10", entry.formula)
            assertEquals(8, entry.carbonNumber)
            assertEquals("monoalkylbenzenes", entry.compoundClassId)
            assertTrue(entry.sourceIds.single().startsWith("nist-"))
            assertTrue(entry.kovatsRange?.isValid() == true)
        }
    }

    @Test
    fun alkylbenzeneSeriesKeepsCandidateLabelsSeparateFromVerifiedNames() {
        val pack = ChromaLabBaseKnowledgePack.pack
        val compoundClass = pack.compoundClasses.single { it.id == "monoalkylbenzenes" }
        val series = pack.carbonNumberSeries.single { it.id == "monoalkylbenzene-carbon-series" }

        assertEquals(KnowledgeIntRange(7, 30), compoundClass.carbonNumberRange)
        assertEquals(listOf("ei-mz-91", "ei-mz-92"), compoundClass.diagnosticIonFragmentIds)
        assertEquals(RetentionOrder.INCREASES_WITH_CARBON_NUMBER, series.retentionOrder)
        assertEquals("C{n} alkylbenzene candidate", series.memberNameTemplate)
        assertTrue(
            series.interpretationNotes.any {
                it.text.contains("candidate grouping") &&
                    it.text.contains("verified compound names")
            },
        )
    }

    @Test
    fun nParaffinReferenceScaleDefinesC7ToC30KovatsAnchors() {
        val pack = ChromaLabBaseKnowledgePack.pack
        val compoundClass = pack.compoundClasses.single { it.id == "n-paraffins" }
        val series = pack.carbonNumberSeries.single { it.id == "n-paraffin-reference-series" }
        val library = pack.kovatsLibraries.single { it.id == "kovats-n-paraffin-reference-scale-c7-c30" }

        assertEquals("CnH2n+2", compoundClass.formulaPattern)
        assertEquals(KnowledgeIntRange(7, 30), compoundClass.carbonNumberRange)
        assertEquals("n-paraffins", series.compoundClassId)
        assertEquals(RetentionOrder.INCREASES_WITH_CARBON_NUMBER, series.retentionOrder)
        assertEquals("n-C{n} paraffin reference", series.memberNameTemplate)
        assertEquals("n-paraffin-reference-series", library.referenceSeriesId)
        assertEquals(24, library.entries.size)

        library.entries.forEachIndexed { index, entry ->
            val carbonNumber = index + 7
            assertEquals(carbonNumber, entry.carbonNumber)
            assertEquals("C${carbonNumber}H${carbonNumber * 2 + 2}", entry.formula)
            assertEquals(carbonNumber * 100.0, entry.kovatsIndex)
            assertEquals("n-paraffins", entry.compoundClassId)
            assertEquals(KnowledgeEvidence.LITERATURE, entry.evidence)
            assertEquals(listOf("nist-gc-retention-data"), entry.sourceIds)
        }
    }

    private companion object {
        val json = Json {
            encodeDefaults = true
            prettyPrint = false
        }
    }
}
