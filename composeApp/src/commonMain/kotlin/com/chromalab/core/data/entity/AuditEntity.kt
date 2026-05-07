package com.chromalab.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chromalab.core.data.model.AuditAction

@Entity(tableName = "audit_log")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val action: AuditAction,
    val field: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val reason: String? = null,
    val timestamp: Long,
)
