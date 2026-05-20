package com.chromalab.feature.reports

import com.chromalab.feature.knowledge.EvidenceClaimScope
import com.chromalab.feature.knowledge.KnowledgeEntryType
import com.chromalab.feature.knowledge.KnowledgeLicenseStatus
import com.chromalab.feature.knowledge.KnowledgeSourceRef
import com.chromalab.feature.knowledge.KnowledgeSourceTrustTier
import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.geometry.AxisCalibrationFit
import com.chromalab.feature.processing.geometry.AxisGeometry
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryAxis
import com.chromalab.feature.processing.geometry.GeometryCandidateSource
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.geometry.GeometryTrace
import com.chromalab.feature.processing.geometry.GraphPanelBounds
import com.chromalab.feature.processing.geometry.PlotAreaBounds
import com.chromalab.feature.processing.geometry.TickGeometry
import com.chromalab.feature.processing.geometry.TickPixelPosition
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.reports.fixtures.BelyiTigrIon92ReportFixture

object Phase7BReportFixtures {

    fun releaseReadyReport(): ChromatogramReport {
        val base = BelyiTigrIon92ReportFixture.buildReport()
        val graph = base.graphs.single()
        val trace = graph.source.geometryTrace ?: GeometryTrace()
        val releaseGraph = graph.copy(
            source = graph.source.copy(
                geometryReportStatus = GeometryReportStatus.SCIENTIFIC_READY,
                geometryTrace = trace.copy(
                    originalImagePath = "original.png",
                    normalizedImagePath = "normalized.png",
                    selectedGraphPanelBounds = GraphPanelBounds(
                        region = GraphRegion(0, 0, 100, 100),
                        candidateSource = GeometryCandidateSource.CV,
                        confidence = 0.95f,
                    ),
                    selectedPlotAreaBounds = PlotAreaBounds(
                        region = GraphRegion(10, 10, 80, 80),
                        parentGraphPanelRegion = GraphRegion(0, 0, 100, 100),
                        confidence = 0.90f,
                    ),
                    finalCenterlineOverlayPath = "centerline.png",
                    axisGeometry = AxisGeometry(
                        xAxisLinePx = AxisLine(10f, 90f, 90f, 90f),
                        yAxisLinePx = AxisLine(10f, 10f, 10f, 90f),
                        axisConfidence = 0.95f,
                    ),
                    tickGeometry = TickGeometry(
                        xTicks = listOf(TickPixelPosition(10f, confidence = 0.95f)),
                        yTicks = listOf(TickPixelPosition(90f, confidence = 0.95f)),
                    ),
                ),
            ),
            axisCalibration = graph.axisCalibration.copy(
                xCalibrationFit = AxisCalibrationFit(axis = GeometryAxis.X, status = CalibrationFitStatus.VALID),
                yCalibrationFit = AxisCalibrationFit(axis = GeometryAxis.Y, status = CalibrationFitStatus.VALID),
            ),
        )
        return base.copy(
            metadata = base.metadata.copy(processingMode = ProcessingMode.AUTONOMOUS_PRODUCTION),
            graphs = listOf(releaseGraph.withAutoValidPeakEvidence()),
            warnings = emptyList(),
            knowledgeCitations = listOf(knowledgeCitation()),
        )
    }

    fun reviewOnlyReport(): ChromatogramReport {
        val report = releaseReadyReport()
        val graph = report.graphs.single()
        return report.copy(
            graphs = listOf(
                graph.copy(
                    peakRecovery = graph.peakRecovery.copy(
                        peakEvidenceTable = graph.peakRecovery.peakEvidenceTable.mapIndexed { index, evidence ->
                            if (index == 0) evidence.copy(
                                status = PeakEvidenceStatus.AUTO_REVIEW,
                                gateStatus = PeakGateStatus.REVIEW,
                                flags = evidence.flags + "review_required",
                            ) else evidence
                        },
                    ),
                ),
            ),
        )
    }

    fun diagnosticMissingCalibrationReport(): ChromatogramReport {
        val report = releaseReadyReport()
        val graph = report.graphs.single()
        return report.copy(
            graphs = listOf(
                graph.copy(
                    axisCalibration = graph.axisCalibration.copy(
                        xCalibrationFit = null,
                        yCalibrationFit = null,
                        pixelToUnitTransform = null,
                    ),
                ),
            ),
        )
    }

    fun blockedMissingGraphEvidenceReport(): ChromatogramReport =
        releaseReadyReport().copy(
            metadata = releaseReadyReport().metadata.copy(detectedGraphCount = 1),
            graphs = emptyList(),
        )

    fun multiGraphReport(): ChromatogramReport {
        val report = releaseReadyReport()
        val graph = report.graphs.single()
        return report.copy(
            metadata = report.metadata.copy(detectedGraphCount = 2),
            graphs = listOf(
                graph,
                graph.copy(
                    graphIndex = 2,
                    identification = graph.identification.copy(
                        ionOrChannel = ReportTextValue(
                            value = "m/z 71.00",
                            status = ReportValueStatus.DETECTED,
                            confidence = 0.92,
                            source = ReportValueSource.OCR,
                        ),
                    ),
                    quality = graph.quality.copy(totalDetectedPeaks = 2),
                    warnings = graph.warnings + ReportWarning(
                        code = "graph2.review",
                        message = "Second graph requires review.",
                        severity = ReportSeverity.WARNING,
                        stage = "fixture",
                        graphIndex = 2,
                    ),
                ),
            ),
        )
    }

    fun knowledgeCitation(
        generatedBy: ReportKnowledgeGeneratedBy = ReportKnowledgeGeneratedBy.KNOWLEDGE_PACK,
        usedEntryIds: List<String> = listOf("report_caveat_calibration_required"),
        unsupportedClaims: List<String> = emptyList(),
        rejectionReason: String? = null,
        attemptedNumericMetricUse: Boolean = false,
    ): ReportKnowledgeCitation =
        ReportKnowledgeCitation(
            citationId = "citation:calibration_required",
            knowledgePackVersion = "chromalab-knowledge-v2",
            usedEntryIds = usedEntryIds,
            usedEntryRecords = usedEntryIds.map { id ->
                ReportKnowledgeEntryRecord(
                    entryId = id,
                    entryType = KnowledgeEntryType.REPORT_CAVEAT,
                    claimScope = listOf(EvidenceClaimScope.REPORT_CAVEAT, EvidenceClaimScope.NOT_MEASUREMENT),
                    allowedUse = listOf("explain report caveats", "ground release-gate warnings"),
                    forbiddenUse = listOf(
                        "create measured RT",
                        "create peak height",
                        "create peak area",
                        "create FWHM",
                        "create S/N",
                        "create baseline",
                        "create Kovats/retention index",
                    ),
                    sourceRefs = listOf(
                        KnowledgeSourceRef(
                            sourceId = "chromalab_internal_curated",
                            label = "ChromaLab internal curated report knowledge",
                            citation = "ChromaLab Knowledge Pack v2, internal curated safety caveats.",
                            licenseStatus = KnowledgeLicenseStatus.INTERNAL_CURATED,
                            trustTier = KnowledgeSourceTrustTier.TIER_0_INTERNAL_CURATED,
                            canBundle = true,
                            canTransform = true,
                        ),
                    ),
                    trustTier = KnowledgeSourceTrustTier.TIER_0_INTERNAL_CURATED,
                )
            },
            explanationTarget = ReportKnowledgeExplanationTarget.CAVEAT,
            generatedBy = generatedBy,
            explanation = "Release-quality reports require calibration evidence before numeric peak claims.",
            unsupportedClaims = unsupportedClaims,
            rejectionReason = rejectionReason,
            attemptedNumericMetricUse = attemptedNumericMetricUse,
        )

    private fun GraphReport.withAutoValidPeakEvidence(): GraphReport =
        copy(
            peakRecovery = peakRecovery.copy(
                peakEvidenceTable = peaks.map { peak ->
                    PeakEvidence(
                        evidenceId = "evidence:${peak.number}",
                        peakId = peak.number.toString(),
                        peakNumber = peak.number,
                        status = PeakEvidenceStatus.AUTO_VALID,
                        gateStatus = PeakGateStatus.VALID,
                        retentionTime = PeakMetricEvidence.calculated(peak.retentionTime.value, "min"),
                        apexPointIndex = peak.number,
                        localMaximumEvidence = true,
                        height = PeakMetricEvidence.calculated(peak.heightAboveBaseline.value ?: 1.0, "a.u."),
                        area = PeakMetricEvidence.calculated(peak.integratedArea.value ?: 1.0),
                        areaPercent = PeakMetricEvidence.calculated(peak.areaPercent.value ?: 1.0, "%"),
                        fwhm = PeakMetricEvidence.calculated(peak.fwhm.value ?: 0.1, "min"),
                        signalToNoise = PeakMetricEvidence.calculated(peak.signalToNoise.value ?: 3.0),
                        boundaryEvidence = PeakBoundaryEvidence(
                            startRetentionTime = PeakMetricEvidence.calculated(
                                peak.startRetentionTime.value ?: 0.0,
                                "min",
                            ),
                            endRetentionTime = PeakMetricEvidence.calculated(
                                peak.endRetentionTime.value ?: 1.0,
                                "min",
                            ),
                            status = PeakMetricEvidenceStatus.CALCULATED,
                        ),
                    )
                },
            ),
        )
}
