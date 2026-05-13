package com.chromalab.feature.knowledge

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalKnowledgePackSchemaTest {

    @Test
    fun localKnowledgePackSchemaRoundTripsAndValidates() {
        val pack = samplePack()
        val validation = LocalKnowledgePackValidator.validate(pack)

        assertTrue(validation.isValid, validation.errors.joinToString { "${it.path}: ${it.message}" })
        assertTrue(validation.warnings.isEmpty())
        assertEquals(CURRENT_LOCAL_KNOWLEDGE_PACK_SCHEMA, pack.schemaVersion)

        val encoded = json.encodeToString(pack)
        val decoded = json.decodeFromString<LocalKnowledgePack>(encoded)

        assertEquals(pack, decoded)
        assertEquals("gc-ms-eic", decoded.chromatogramTypes.single().id)
        assertEquals("ion-test-100", decoded.ionFragments.single().id)
        assertEquals("test-aromatic", decoded.compoundClasses.single().id)
        assertEquals("test-aromatic-series", decoded.carbonNumberSeries.single().id)
        assertEquals("test-kovats", decoded.kovatsLibraries.single().id)
    }

    @Test
    fun validatorRejectsDanglingReferencesAndInvalidRanges() {
        val broken = samplePack().copy(
            ionFragments = listOf(
                samplePack().ionFragments.single().copy(
                    nominalMz = -100.0,
                    mzWindow = KnowledgeDoubleRange(101.0, 99.0, "m/z"),
                    diagnosticForCompoundClassIds = listOf("missing-class"),
                    sourceIds = listOf("missing-source"),
                ),
            ),
            compoundClasses = listOf(
                samplePack().compoundClasses.single().copy(
                    carbonNumberRange = KnowledgeIntRange(20, 6),
                    diagnosticIonFragmentIds = listOf("missing-ion"),
                    homologousSeriesIds = listOf("missing-series"),
                ),
            ),
            carbonNumberSeries = listOf(
                samplePack().carbonNumberSeries.single().copy(
                    compoundClassId = "missing-class",
                    carbonNumberRange = KnowledgeIntRange(12, 6),
                ),
            ),
            kovatsLibraries = listOf(
                samplePack().kovatsLibraries.single().copy(
                    chromatogramTypeId = "missing-type",
                    referenceSeriesId = "missing-series",
                    entries = listOf(
                        samplePack().kovatsLibraries.single().entries.single().copy(
                            compoundClassId = "missing-class",
                            carbonNumber = -1,
                            kovatsIndex = null,
                            kovatsRange = null,
                            sourceIds = listOf("missing-source"),
                        ),
                    ),
                ),
            ),
        )

        val validation = LocalKnowledgePackValidator.validate(broken)

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.path.contains("nominalMz") })
        assertTrue(validation.errors.any { it.path.contains("mzWindow") })
        assertTrue(validation.errors.any { it.message.contains("unknown referenced id") })
        assertTrue(validation.errors.any { it.message.contains("minimum must be <= maximum") })
        assertTrue(validation.errors.any { it.message.contains("either kovatsIndex or kovatsRange") })
    }

    @Test
    fun validatorWarnsAboutEmptyKovatsLibrariesWithoutRejectingSchemaDrafts() {
        val draft = samplePack().copy(
            kovatsLibraries = listOf(samplePack().kovatsLibraries.single().copy(entries = emptyList())),
        )

        val validation = LocalKnowledgePackValidator.validate(draft)

        assertTrue(validation.isValid)
        assertTrue(validation.warnings.any { it.path.endsWith(".entries") })
    }

    private fun samplePack(): LocalKnowledgePack {
        val source = KnowledgeSource(
            id = "test-source",
            label = "Curated test source",
            citation = "Synthetic schema test fixture",
        )
        return LocalKnowledgePack(
            id = "test-pack",
            name = "Test Knowledge Pack",
            revision = "2026-05-13-test",
            description = "Schema-only fixture; not production chemistry data.",
            sources = listOf(source),
            chromatogramTypes = listOf(
                ChromatogramTypeDefinition(
                    id = "gc-ms-eic",
                    label = "GC-MS extracted ion chromatogram",
                    analysisType = "GC-MS",
                    chromatogramMode = "EIC",
                    expectedXAxisUnit = "min",
                    expectedYAxisUnit = "counts",
                    supportedIonFragmentIds = listOf("ion-test-100"),
                    targetCompoundClassIds = listOf("test-aromatic"),
                    interpretationNotes = listOf(
                        KnowledgeStatement(
                            text = "Use as a schema fixture only.",
                            confidence = 1.0,
                            sourceIds = listOf(source.id),
                        ),
                    ),
                    sourceIds = listOf(source.id),
                ),
            ),
            ionFragments = listOf(
                IonFragmentDefinition(
                    id = "ion-test-100",
                    nominalMz = 100.0,
                    mzWindow = KnowledgeDoubleRange(99.5, 100.5, "m/z"),
                    label = "Test diagnostic fragment",
                    ionization = IonizationMode.ELECTRON_IONIZATION,
                    diagnosticForCompoundClassIds = listOf("test-aromatic"),
                    interpretation = listOf(
                        KnowledgeStatement(
                            text = "Synthetic diagnostic fragment for schema tests.",
                            confidence = 0.9,
                            sourceIds = listOf(source.id),
                        ),
                    ),
                    sourceIds = listOf(source.id),
                ),
            ),
            compoundClasses = listOf(
                CompoundClassDefinition(
                    id = "test-aromatic",
                    label = "Test aromatic class",
                    formulaPattern = "CnH2n-6",
                    carbonNumberRange = KnowledgeIntRange(6, 20),
                    diagnosticIonFragmentIds = listOf("ion-test-100"),
                    homologousSeriesIds = listOf("test-aromatic-series"),
                    sourceIds = listOf(source.id),
                ),
            ),
            carbonNumberSeries = listOf(
                CarbonNumberSeriesDefinition(
                    id = "test-aromatic-series",
                    label = "Test aromatic homologous series",
                    compoundClassId = "test-aromatic",
                    carbonNumberRange = KnowledgeIntRange(6, 20),
                    retentionOrder = RetentionOrder.INCREASES_WITH_CARBON_NUMBER,
                    memberNameTemplate = "C{n} test aromatic",
                    expectedIonFragmentIds = listOf("ion-test-100"),
                    sourceIds = listOf(source.id),
                ),
            ),
            kovatsLibraries = listOf(
                KovatsReferenceLibrary(
                    id = "test-kovats",
                    label = "Test Kovats library",
                    chromatogramTypeId = "gc-ms-eic",
                    stationaryPhase = "test phase",
                    referenceSeriesId = "test-aromatic-series",
                    entries = listOf(
                        KovatsReferenceEntry(
                            compoundName = "test aromatic C10",
                            compoundClassId = "test-aromatic",
                            formula = "C10H14",
                            carbonNumber = 10,
                            kovatsIndex = 1000.0,
                            kovatsRange = KnowledgeDoubleRange(990.0, 1010.0, "RI"),
                            evidence = KnowledgeEvidence.CURATED,
                            sourceIds = listOf(source.id),
                        ),
                    ),
                    sourceIds = listOf(source.id),
                ),
            ),
        )
    }

    private companion object {
        val json = Json {
            encodeDefaults = true
            prettyPrint = false
        }
    }
}
