package com.chromalab.core.data.dao

import androidx.room.*
import com.chromalab.core.data.entity.CalculationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {
    @Insert
    suspend fun insert(calculation: CalculationEntity): Long

    @Update
    suspend fun update(calculation: CalculationEntity)

    @Delete
    suspend fun delete(calculation: CalculationEntity)

    @Query("SELECT * FROM calculations WHERE sampleId = :sampleId ORDER BY createdAt DESC")
    fun getBySampleId(sampleId: Long): Flow<List<CalculationEntity>>

    @Query("SELECT * FROM calculations WHERE id = :id")
    suspend fun getById(id: Long): CalculationEntity?
}
