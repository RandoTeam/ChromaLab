package com.chromalab.core.data.dao

import androidx.room.*
import com.chromalab.core.data.entity.ChromatogramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChromatogramDao {
    @Insert
    suspend fun insert(chromatogram: ChromatogramEntity): Long

    @Update
    suspend fun update(chromatogram: ChromatogramEntity)

    @Delete
    suspend fun delete(chromatogram: ChromatogramEntity)

    @Query("SELECT * FROM chromatograms WHERE sampleId = :sampleId ORDER BY createdAt DESC")
    fun getBySampleId(sampleId: Long): Flow<List<ChromatogramEntity>>

    @Query("SELECT * FROM chromatograms WHERE id = :id")
    suspend fun getById(id: Long): ChromatogramEntity?
}
