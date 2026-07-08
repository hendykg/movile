package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val result: String,       // "SUCCESS" or "FAILED"
    val endpoint: String,     // The target synchronization endpoint
    val errorMessage: String? // Null if successful, otherwise contains detail
)
