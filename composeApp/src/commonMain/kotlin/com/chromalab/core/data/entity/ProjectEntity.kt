package com.chromalab.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val clientName: String? = null,
    val date: Long,
    val methodology: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
