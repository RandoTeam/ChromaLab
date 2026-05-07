package com.chromalab.core.data.dao

import androidx.room.*
import com.chromalab.core.data.entity.SampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {
    @Insert
    suspend fun insert(sample: SampleEntity): Long

    @Update
    suspend fun update(sample: SampleEntity)

    @Delete
    suspend fun delete(sample: SampleEntity)

    @Query("SELECT * FROM samples WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getByProjectId(projectId: Long): Flow<List<SampleEntity>>

    @Query("SELECT * FROM samples WHERE id = :id")
    suspend fun getById(id: Long): SampleEntity?
}
