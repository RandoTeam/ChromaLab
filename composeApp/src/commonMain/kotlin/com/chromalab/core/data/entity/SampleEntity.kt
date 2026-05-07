package com.chromalab.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "samples",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("projectId")],
)
data class SampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val vial: String? = null,
    val analysisDate: Long? = null,
    val operator: String? = null,
    val instrument: String? = null,
    val method: String? = null,
    val matrix: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
