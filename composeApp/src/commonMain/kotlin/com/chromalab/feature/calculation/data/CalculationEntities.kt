package com.chromalab.feature.calculation.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chromalab.core.data.entity.ChromatogramEntity

/**
 * Phase 2 Room entities (§2.29).
 *
 * Schema: one ChromatogramEntity → many CalculationRunEntity → many child entities.
 * Old runs are NEVER overwritten — each recalculation creates a new run.
 */

// ─── Calculation Run ────────────────────────────────────────────

@Entity(
    tableName = "calculation_runs",
    foreignKeys = [
        ForeignKey(
            entity = ChromatogramEntity::class,
            parentColumns = ["id"],
            childColumns = ["chromatogramId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("chromatogramId")],
)
data class CalculationRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chromatogramId: Long,
    val pipelineVersion: String,
    val algorithmVersion: String,
    val presetName: String,
    val paramsJson: String,
    val warningsJson: String = "[]",
    val manualEditsCsv: String = "",
    val createdAt: Long,
)

// ─── Detected Peak ──────────────────────────────────────────────

@Entity(
    tableName = "calculation_peaks",
    foreignKeys = [
        ForeignKey(
            entity = CalculationRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("runId")],
)
data class CalculationPeakEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val runId: Long,
    val peakIndex: Int,
    val status: String,
    val rtApex: Double,
    val rtCentroid: Double,
    val height: Double,
    val area: Double,
    val widthBase: Double,
    val widthHalfHeight: Double,
    val prominence: Double,
    val snr: Double,
    val snrFlag: String,
    val confidenceGrade: String,
    val confidenceScore: Double,
    val overlapStatus: String,
    val boundaryMethod: String,
    val boundaryConfidence: Double,
    val leftBoundaryTime: Double,
    val rightBoundaryTime: Double,
    val positiveArea: Double,
    val negativeArea: Double,
    val isManuallyEdited: Boolean,
    val warningsJson: String = "[]",
)

// ─── Baseline Result ────────────────────────────────────────────

@Entity(
    tableName = "calculation_baselines",
    foreignKeys = [
        ForeignKey(
            entity = CalculationRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("runId")],
)
data class BaselineResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val runId: Long,
    val method: String,
    val quality: String,
    val flatnessScore: Double,
    val residualStd: Double,
    val negativePercent: Double,
    val pointCount: Int,
    val valuesJson: String,
)

// ─── Noise Region ───────────────────────────────────────────────

@Entity(
    tableName = "calculation_noise_regions",
    foreignKeys = [
        ForeignKey(
            entity = CalculationRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("runId")],
)
data class NoiseRegionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val runId: Long,
    val startTime: Double,
    val endTime: Double,
    val noiseMethod: String,
    val noiseValue: Double,
    val isAutoDetected: Boolean,
)

// ─── Manual Edit ────────────────────────────────────────────────

@Entity(
    tableName = "calculation_manual_edits",
    foreignKeys = [
        ForeignKey(
            entity = CalculationRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("runId")],
)
data class ManualEditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val runId: Long,
    val peakIndex: Int,
    val editType: String,
    val oldValue: String,
    val newValue: String,
    val reason: String = "",
    val timestamp: Long,
)

// ─── Export Record ──────────────────────────────────────────────

@Entity(
    tableName = "calculation_exports",
    foreignKeys = [
        ForeignKey(
            entity = CalculationRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("runId")],
)
data class ExportRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val runId: Long,
    val format: String,
    val filePath: String,
    val exportedAt: Long,
)
