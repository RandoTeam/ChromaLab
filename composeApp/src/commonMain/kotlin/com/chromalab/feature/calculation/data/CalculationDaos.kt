package com.chromalab.feature.calculation.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Phase 2 DAOs (§2.29).
 *
 * Rule: old calculation runs are NEVER overwritten.
 * New run = new insert. History preserved.
 */

// ─── Calculation Run DAO ────────────────────────────────────────

@Dao
interface CalculationRunDao {
    @Insert
    suspend fun insert(run: CalculationRunEntity): Long

    @Query("SELECT * FROM calculation_runs WHERE chromatogramId = :chromatogramId ORDER BY createdAt DESC")
    fun getByChromatogramId(chromatogramId: Long): Flow<List<CalculationRunEntity>>

    @Query("SELECT * FROM calculation_runs WHERE id = :id")
    suspend fun getById(id: Long): CalculationRunEntity?

    @Query("SELECT * FROM calculation_runs WHERE chromatogramId = :chromatogramId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(chromatogramId: Long): CalculationRunEntity?

    @Query("SELECT COUNT(*) FROM calculation_runs WHERE chromatogramId = :chromatogramId")
    suspend fun countRuns(chromatogramId: Long): Int

    @Delete
    suspend fun delete(run: CalculationRunEntity)
}

// ─── Calculation Peak DAO ───────────────────────────────────────

@Dao
interface CalculationPeakDao {
    @Insert
    suspend fun insertAll(peaks: List<CalculationPeakEntity>)

    @Query("SELECT * FROM calculation_peaks WHERE runId = :runId ORDER BY peakIndex ASC")
    fun getByRunId(runId: Long): Flow<List<CalculationPeakEntity>>

    @Query("SELECT * FROM calculation_peaks WHERE runId = :runId ORDER BY peakIndex ASC")
    suspend fun getByRunIdOnce(runId: Long): List<CalculationPeakEntity>

    @Query("SELECT COUNT(*) FROM calculation_peaks WHERE runId = :runId")
    suspend fun countPeaks(runId: Long): Int

    @Query("DELETE FROM calculation_peaks WHERE runId = :runId")
    suspend fun deleteByRunId(runId: Long)
}

// ─── Baseline Result DAO ────────────────────────────────────────

@Dao
interface BaselineResultDao {
    @Insert
    suspend fun insert(baseline: BaselineResultEntity): Long

    @Query("SELECT * FROM calculation_baselines WHERE runId = :runId")
    suspend fun getByRunId(runId: Long): BaselineResultEntity?
}

// ─── Noise Region DAO ───────────────────────────────────────────

@Dao
interface NoiseRegionDao {
    @Insert
    suspend fun insertAll(regions: List<NoiseRegionEntity>)

    @Query("SELECT * FROM calculation_noise_regions WHERE runId = :runId")
    suspend fun getByRunId(runId: Long): List<NoiseRegionEntity>
}

// ─── Manual Edit DAO ────────────────────────────────────────────

@Dao
interface ManualEditDao {
    @Insert
    suspend fun insert(edit: ManualEditEntity): Long

    @Insert
    suspend fun insertAll(edits: List<ManualEditEntity>)

    @Query("SELECT * FROM calculation_manual_edits WHERE runId = :runId ORDER BY timestamp ASC")
    fun getByRunId(runId: Long): Flow<List<ManualEditEntity>>

    @Query("SELECT * FROM calculation_manual_edits WHERE runId = :runId ORDER BY timestamp ASC")
    suspend fun getByRunIdOnce(runId: Long): List<ManualEditEntity>

    @Query("SELECT COUNT(*) FROM calculation_manual_edits WHERE runId = :runId")
    suspend fun countEdits(runId: Long): Int
}

// ─── Export Record DAO ──────────────────────────────────────────

@Dao
interface ExportRecordDao {
    @Insert
    suspend fun insert(record: ExportRecordEntity): Long

    @Query("SELECT * FROM calculation_exports WHERE runId = :runId ORDER BY exportedAt DESC")
    fun getByRunId(runId: Long): Flow<List<ExportRecordEntity>>
}
