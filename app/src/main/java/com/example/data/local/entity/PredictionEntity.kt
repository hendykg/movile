package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "predictions")
data class PredictionEntity(
    @PrimaryKey val id: String, // String ID formed by "${userId}_${matchId}"
    val userId: String,
    val matchId: Int,
    val predictedHomeScore: Int,
    val predictedAwayScore: Int,
    val pointsEarned: Int = 0,
    val isSynced: Boolean = true
)
