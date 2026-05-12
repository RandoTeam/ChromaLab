package com.chromalab.feature.reports.fixtures

import com.chromalab.feature.reports.AxisReport
import com.chromalab.feature.reports.ChemicalInterpretationReport
import com.chromalab.feature.reports.ChromatogramIdentification
import com.chromalab.feature.reports.ChromatogramReport
import com.chromalab.feature.reports.ChromatographicQualityReport
import com.chromalab.feature.reports.CompoundAssignment
import com.chromalab.feature.reports.DistributionBucket
import com.chromalab.feature.reports.DoubleRangeValue
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.GraphReport
import com.chromalab.feature.reports.GraphSourceMetadata
import com.chromalab.feature.reports.InputSourceType
import com.chromalab.feature.reports.KovatsCalculationKind
import com.chromalab.feature.reports.KovatsIndexReport
import com.chromalab.feature.reports.KovatsIndexResult
import com.chromalab.feature.reports.ModelExecutionInfo
import com.chromalab.feature.reports.PixelRect
import com.chromalab.feature.reports.PixelToUnitTransform
import com.chromalab.feature.reports.ProcessingMode
import com.chromalab.feature.reports.ReportAnomaly
import com.chromalab.feature.reports.ReportAxisCalibration
import com.chromalab.feature.reports.ReportContractValidationResult
import com.chromalab.feature.reports.ReportContractValidator
import com.chromalab.feature.reports.ReportDoubleValue
import com.chromalab.feature.reports.ReportMarkdownRenderer
import com.chromalab.feature.reports.ReportMetadata
import com.chromalab.feature.reports.ReportSectionStatus
import com.chromalab.feature.reports.ReportSeverity
import com.chromalab.feature.reports.ReportTextValue
import com.chromalab.feature.reports.ReportValueSource
import com.chromalab.feature.reports.ReportValueStatus
import com.chromalab.feature.reports.ReportWarning
import com.chromalab.feature.reports.SignalAndBaselineReport
import com.chromalab.feature.reports.ReportPeak

/**
 * Phase 1.5 executable fixture for the Belyi Tigr Ion 92 reference case.
 *
 * The values here are report-shape placeholders from the visible fixture facts. They are not a
 * validated numeric answer and must not be used to judge calculation accuracy.
 */
object BelyiTigrIon92ReportFixture {

    const val fixtureId: String = "belyi_tigr_ion92_report_reference"
    const val imageSha256: String = "D1F0A55F6491E6FA7E3857086FDCCE97CDD3723A4F786D40000480F9A4B8BDFE"
    const val referenceSha256: String = "8D68B75738DD33DFFD74CFB83696F3485C46D3E8AE6A770CAA562A02D0404DB0"

    fun buildReport(): ChromatogramReport {
        val warnings = listOf(
            ReportWarning(
                code = "fixture.numeric_truth_not_locked",
                message = "This fixture validates report shape and completeness; numeric values are not locked ground truth.",
                severity = ReportSeverity.INFO,
                stage = "fixture",
                graphIndex = 1,
            ),
            ReportWarning(
                code = "fixture.dominant_peak_reference_discrepancy",
                message = "Reference text claims a dominant peak near 49 min, while the supplied screenshot visually shows a dominant early peak.",
                severity = ReportSeverity.WARNING,
                stage = "fixture",
                graphIndex = 1,
            ),
        )

        return ChromatogramReport(
            metadata = ReportMetadata(
                reportId = fixtureId,
                appVersion = "fixture",
                analysisStartedAtEpochMillis = 0L,
                analysisCompletedAtEpochMillis = 900_000L,
                totalAnalysisDurationMillis = 900_000L,
                inputSourceType = InputSourceType.TEST_FIXTURE,
                sourceName = "Belyi Tigr Ion 92 screenshot fixture",
                detectedGraphCount = 1,
                selectedModel = ModelExecutionInfo(
                    modelId = "fixture-selected-vision-model",
                    modelName = "Fixture selected vision model",
                    runtime = ExecutedRuntime.LITERT,
                ),
                executedModel = ModelExecutionInfo(
                    modelId = "fixture-executed-vision-model",
                    modelName = "Fixture executed vision model",
                    runtime = ExecutedRuntime.LITERT,
                ),
                executedRuntime = ExecutedRuntime.MIXED,
                deviceName = "fixture-device",
                processingMode = ProcessingMode.FULL_ANALYSIS,
            ),
            graphs = listOf(buildGraph(warnings)),
            warnings = warnings,
        )
    }

    fun validate(): ReportContractValidationResult =
        ReportContractValidator.validate(buildReport())

    fun renderMarkdown(): String =
        ReportMarkdownRenderer.render(buildReport())

    private fun buildGraph(warnings: List<ReportWarning>): GraphReport =
        GraphReport(
            graphIndex = 1,
            source = GraphSourceMetadata(
                sourceImageBounds = PixelRect(x = 0, y = 0, width = 576, height = 1280),
                detectedGraphBounds = PixelRect(x = 7, y = 425, width = 560, height = 478),
                cropConfidence = 0.82,
                preprocessingSteps = listOf(
                    "document-page crop required",
                    "phone UI and article text excluded",
                    "graph region normalized",
                ),
                scanMode = "Smart Scan gallery fixture",
                titleOcrConfidence = 0.86,
                axisOcrConfidence = 0.78,
                tickOcrConfidence = 0.76,
                manuallyAdjusted = false,
            ),
            identification = ChromatogramIdentification(
                analysisType = detected("GC-MS", 0.82, ReportValueSource.OCR),
                chromatogramMode = detected("EIC", 0.80, ReportValueSource.LOCAL_KNOWLEDGE),
                ionOrChannel = detected("m/z 92.00", 0.93, ReportValueSource.OCR),
                ionRange = detected("91.70 to 92.70", 0.88, ReportValueSource.OCR),
                sampleName = detected("BELIY TIGR_1", 0.86, ReportValueSource.OCR),
                samplePathOrInstrumentLabel = detected("BELIY TIGR_1.D\\data.ms", 0.84, ReportValueSource.OCR),
                matrix = inferred("oil / condensate", 0.55, ReportValueSource.LOCAL_KNOWLEDGE),
                targetCompoundClass = inferred("alkylbenzenes", 0.62, ReportValueSource.LOCAL_KNOWLEDGE),
            ),
            axisCalibration = ReportAxisCalibration(
                xAxis = AxisReport(
                    label = detected("Time", 0.86, ReportValueSource.OCR),
                    unit = detected("min", 0.80, ReportValueSource.OCR),
                    visibleMinimum = calculated(8.0, "min", 0.62),
                    visibleMaximum = calculated(57.0, "min", 0.70),
                    majorTicks = listOf(10.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0)
                        .map { calculated(it, "min", 0.78) },
                ),
                yAxis = AxisReport(
                    label = detected("Abundance", 0.86, ReportValueSource.OCR),
                    unit = detected("counts", 0.70, ReportValueSource.LOCAL_KNOWLEDGE),
                    visibleMinimum = calculated(0.0, "counts", 0.82),
                    visibleMaximum = calculated(10_000.0, "counts", 0.82),
                    majorTicks = (0..10).map { calculated(it * 1000.0, "counts", 0.78) },
                ),
                calibrationConfidence = 0.76,
                pixelToUnitTransform = PixelToUnitTransform(
                    xScale = 0.0875,
                    xOffset = 8.0,
                    yScale = -24.8,
                    yOffset = 10_000.0,
                    method = "fixture-linear-axis-estimate",
                ),
                warnings = listOf(
                    ReportWarning(
                        code = "fixture.axis_calibration_visual_estimate",
                        message = "Axis transform is a visual fixture estimate, not a validated calibration result.",
                        severity = ReportSeverity.INFO,
                        stage = "axis_calibration",
                        graphIndex = 1,
                    ),
                ),
            ),
            signal = SignalAndBaselineReport(
                pointCount = 560,
                smoothingMethod = "not applied in fixture",
                baselineMethod = "visual fixture baseline",
                baselineMean = calculated(75.0, "counts", 0.45),
                baselineDrift = calculated(80.0, "counts", 0.45),
                rmsNoise = calculated(35.0, "counts", 0.45),
                noiseMethod = "visual estimate",
                correctedSignalAvailable = false,
                signalExtractionConfidence = 0.55,
            ),
            peaks = representativePeaks(),
            quality = ChromatographicQualityReport(
                totalDetectedPeaks = 35,
                significantPeakCount = 30,
                significantPeakSnrThreshold = 3.0,
                meanSnr = calculated(18.0, confidence = 0.40),
                medianSnr = calculated(12.0, confidence = 0.40),
                maximumPeakHeight = calculated(9500.0, "counts", 0.50),
                dominantPeakNumber = 1,
                baselineQuality = inferred("low baseline with slight late drift", 0.55, ReportValueSource.LOCAL_KNOWLEDGE),
                averageResolution = ReportDoubleValue.notCalculated(),
                minimumResolution = ReportDoubleValue.notCalculated(),
                theoreticalPlates = ReportDoubleValue.notCalculated(),
                hetp = ReportDoubleValue.notCalculated(),
                globalIntegratedArea = ReportDoubleValue.notCalculated(),
                areaNormalizationStatus = inferred("not locked in fixture", 0.50, ReportValueSource.USER),
                anomalies = listOf(
                    ReportAnomaly(
                        code = "early_dominant_peak",
                        message = "Very tall early peak is visible and must be checked as anomaly/internal standard/co-elution.",
                        peakNumber = 1,
                        severity = ReportSeverity.WARNING,
                    ),
                ),
            ),
            kovats = KovatsIndexReport(
                status = ReportValueStatus.NOT_CALCULATED,
                formula = "I = 100*z + 100*(RT(x)-RT(z))/(RT(z+1)-RT(z))",
                referenceSeries = "n-paraffins",
                results = listOf(
                    KovatsIndexResult(
                        peakNumber = 2,
                        compoundName = "toluene",
                        carbonNumber = "C7",
                        retentionTime = 10.2,
                        calculatedIndex = ReportDoubleValue.notCalculated(),
                        literatureRange = DoubleRangeValue(762.0, 780.0),
                        calculationKind = KovatsCalculationKind.NOT_CALCULABLE,
                        confidence = 0.30,
                    ),
                    KovatsIndexResult(
                        peakNumber = 4,
                        compoundName = "ethylbenzene",
                        carbonNumber = "C8",
                        retentionTime = 13.2,
                        calculatedIndex = ReportDoubleValue.notCalculated(),
                        literatureRange = DoubleRangeValue(855.0, 870.0),
                        calculationKind = KovatsCalculationKind.NOT_CALCULABLE,
                        confidence = 0.30,
                    ),
                ),
                trendLinearityR2 = ReportDoubleValue.notCalculated(),
                missingDataNotes = listOf(
                    "n-paraffin reference retention times are not available in this fixture.",
                    "Kovats values must remain not calculable until the reference series is supplied or detected.",
                ),
            ),
            interpretation = ChemicalInterpretationReport(
                distributionByCarbonNumber = listOf(
                    DistributionBucket("C7-C10", areaPercent = calculated(22.0, "%", 0.30), peakCount = 10),
                    DistributionBucket("C11-C15", areaPercent = calculated(34.0, "%", 0.30), peakCount = 12),
                    DistributionBucket("C16-C20", areaPercent = calculated(36.0, "%", 0.30), peakCount = 10),
                    DistributionBucket("C21+", areaPercent = calculated(8.0, "%", 0.30), peakCount = 3),
                ),
                homologSeriesNotes = listOf(
                    "m/z 92 supports an alkylbenzene-oriented interpretation when confirmed by local knowledge and peak pattern.",
                    "The report must distinguish calculated values from compound assignment hypotheses.",
                ),
                likelyCompoundClass = inferred("alkylbenzene homolog series", 0.62, ReportValueSource.LOCAL_KNOWLEDGE),
                domainContextNotes = listOf(
                    "Belyi Tigr context is useful as interpretation metadata, not as a source of numeric peak values.",
                    "The early dominant peak should be explained as possible internal standard, contamination, or co-elution until verified.",
                ),
                unresolvedAssignments = listOf(
                    "Exact compound assignments require validated peak extraction and a local domain knowledge pack.",
                ),
            ),
            sectionStatus = ReportSectionStatus(
                overview = ReportValueStatus.DETECTED,
                graphPreparation = ReportValueStatus.DETECTED,
                axisCalibration = ReportValueStatus.INFERRED,
                peakTable = ReportValueStatus.INFERRED,
                graphOverlay = ReportValueStatus.NOT_CALCULATED,
                chromatographicQuality = ReportValueStatus.INFERRED,
                kovatsAnalysis = ReportValueStatus.NOT_CALCULATED,
                chemicalInterpretation = ReportValueStatus.INFERRED,
                technicalAppendix = ReportValueStatus.DETECTED,
            ),
            warnings = warnings,
        )

    private fun representativePeaks(): List<ReportPeak> =
        listOf(
            peak(
                number = 1,
                rt = 8.4,
                height = 9500.0,
                areaPercent = 18.0,
                name = "early dominant peak",
                carbon = null,
                flags = listOf("visual_fixture_estimate", "anomaly_candidate", "reference_text_discrepancy"),
            ),
            peak(
                number = 2,
                rt = 10.2,
                height = 400.0,
                areaPercent = 4.0,
                name = "toluene",
                carbon = "C7",
                flags = listOf("reference_format_candidate", "not_numeric_truth"),
            ),
            peak(
                number = 3,
                rt = 13.2,
                height = 600.0,
                areaPercent = 6.0,
                name = "ethylbenzene",
                carbon = "C8",
                flags = listOf("reference_format_candidate", "not_numeric_truth"),
            ),
            peak(
                number = 4,
                rt = 35.5,
                height = 1600.0,
                areaPercent = 12.0,
                name = "alkylbenzene homolog candidate",
                carbon = "C15",
                flags = listOf("visual_fixture_estimate", "compound_assignment_unverified"),
            ),
            peak(
                number = 5,
                rt = 49.0,
                height = 1100.0,
                areaPercent = 8.0,
                name = "late alkylbenzene candidate",
                carbon = "C20",
                flags = listOf("visual_fixture_estimate", "reference_claim_requires_extraction_check"),
            ),
        )

    private fun peak(
        number: Int,
        rt: Double,
        height: Double,
        areaPercent: Double,
        name: String,
        carbon: String?,
        flags: List<String>,
    ): ReportPeak =
        ReportPeak(
            number = number,
            retentionTime = calculated(rt, "min", 0.45),
            absoluteApexIntensity = calculated(height + 75.0, "counts", 0.40),
            baselineAtApex = calculated(75.0, "counts", 0.40),
            heightAboveBaseline = calculated(height, "counts", 0.45),
            startRetentionTime = calculated(rt - 0.08, "min", 0.30),
            endRetentionTime = calculated(rt + 0.10, "min", 0.30),
            widthAtBase = calculated(0.18, "min", 0.30),
            fwhm = calculated(0.08, "min", 0.30),
            integratedArea = calculated(height * 0.12, "counts*min", 0.30),
            areaPercent = calculated(areaPercent, "%", 0.30),
            signalToNoise = calculated(height / 70.0, confidence = 0.30),
            asymmetry = calculated(1.15, confidence = 0.25),
            overlapClass = inferred("not assessed in fixture", 0.25, ReportValueSource.USER),
            boundaryMethod = "fixture visual window",
            integrationMethod = "fixture visual estimate",
            confidence = 0.35,
            compound = CompoundAssignment(
                probableName = inferred(name, 0.35, ReportValueSource.LOCAL_KNOWLEDGE),
                formula = ReportTextValue.notCalculated(),
                compoundClass = inferred("alkylbenzene candidate", 0.35, ReportValueSource.LOCAL_KNOWLEDGE),
                carbonNumber = carbon?.let { inferred(it, 0.35, ReportValueSource.LOCAL_KNOWLEDGE) }
                    ?: ReportTextValue.notCalculated(),
                kovatsIndex = ReportDoubleValue.notCalculated(),
                assignmentConfidence = 0.35,
                assignmentBasis = "fixture report-shape placeholder",
            ),
            flags = flags,
        )

    private fun detected(
        value: String,
        confidence: Double,
        source: ReportValueSource,
    ): ReportTextValue =
        ReportTextValue(value, ReportValueStatus.DETECTED, confidence, source)

    private fun inferred(
        value: String,
        confidence: Double,
        source: ReportValueSource,
    ): ReportTextValue =
        ReportTextValue(value, ReportValueStatus.INFERRED, confidence, source)

    private fun calculated(
        value: Double,
        unit: String? = null,
        confidence: Double,
    ): ReportDoubleValue =
        ReportDoubleValue(value, unit, ReportValueStatus.CALCULATED, confidence, ReportValueSource.USER)
}
