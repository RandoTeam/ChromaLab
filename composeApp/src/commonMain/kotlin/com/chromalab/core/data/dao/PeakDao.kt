package com.chromalab.core.data.dao

import androidx.room.*
import com.chromalab.core.data.entity.PeakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeakDao {
    @Insert
    suspend fun insert(peak: PeakEntity): Long

    @Insert
    suspend fun insertAll(peaks: List<PeakEntity>)

    @Update
    suspend fun update(peak: PeakEntity)

    @Delete
    suspend fun delete(peak: PeakEntity)

    @Query("SELECT * FROM peaks WHERE chromatogramId = :chromatogramId ORDER BY peakNumber ASC")
    fun getByChromatogramId(chromatogramId: Long): Flow<List<PeakEntity>>

    @Query("DELETE FROM peaks WHERE chromatogramId = :chromatogramId")
    suspend fun deleteByChromatogramId(chromatogramId: Long)
}
