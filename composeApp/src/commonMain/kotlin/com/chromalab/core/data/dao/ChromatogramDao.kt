package com.chromalab.core.data.dao

import androidx.room.*
import com.chromalab.core.data.entity.ChromatogramEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for chromatogram records.
 *
 * Provides CRUD operations plus queries used by:
 * - CalculationsListScreen (getAll, deleteById)
 * - AnalysisFlowScreen (getById)
 * - Processing pipeline (insert)
 */
@Dao
interface ChromatogramDao {
    @Insert
    suspend fun insert(chromatogram: ChromatogramEntity): Long

    @Update
    suspend fun update(chromatogram: ChromatogramEntity)

    @Delete
    suspend fun delete(chromatogram: ChromatogramEntity)

    /** Delete a single chromatogram by its ID. Used by Calculations list. */
    @Query("DELETE FROM chromatograms WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Get all chromatograms, newest first. Used by CalculationsListScreen. */
    @Query("SELECT * FROM chromatograms ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ChromatogramEntity>>

    @Query("SELECT * FROM chromatograms WHERE sampleId = :sampleId ORDER BY createdAt DESC")
    fun getBySampleId(sampleId: Long): Flow<List<ChromatogramEntity>>

    @Query("SELECT * FROM chromatograms WHERE id = :id")
    suspend fun getById(id: Long): ChromatogramEntity?
}
