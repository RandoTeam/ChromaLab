package com.chromalab.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chromalab.core.data.model.IntegrationStatus

@Entity(
    tableName = "peaks",
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
data class PeakEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chromatogramId: Long,
    val peakNumber: Int,
    val retentionTime: Double,
    val startTime: Double,
    val endTime: Double,
    val height: Double,
    val area: Double,
    val width: Double,
    val snRatio: Double? = null,
    val integrationStatus: IntegrationStatus,
    val createdAt: Long,
)
