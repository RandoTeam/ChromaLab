package com.chromalab.core.data.dao

import androidx.room.*
import com.chromalab.core.data.entity.AuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(audit: AuditEntity): Long

    @Query("SELECT * FROM audit_log WHERE entityType = :entityType AND entityId = :entityId ORDER BY timestamp DESC")
    fun getByEntity(entityType: String, entityId: Long): Flow<List<AuditEntity>>

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<AuditEntity>>
}
