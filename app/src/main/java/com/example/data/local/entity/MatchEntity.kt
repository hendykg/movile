package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val dateTime: Long, // Match start time timestamp
    val status: String,   // "SCHEDULED", "LIVE", "FINISHED"
    val stadiumId: Int,
    val updatedAt: Long
)
