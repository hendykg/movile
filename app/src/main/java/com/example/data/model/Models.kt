package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val points: Int,
    val predictionCount: Int,
    val joinedDate: String
)

@JsonClass(generateAdapter = true)
data class Group(
    val id: Int,
    val name: String,
    val code: String,
    val description: String,
    val membersCount: Int,
    val adminId: String,
    val stadiumName: String? = null
)

@JsonClass(generateAdapter = true)
data class LeaderboardEntry(
    val userId: String,
    val username: String,
    val points: Int,
    val predictionCount: Int,
    val position: Int
)

@JsonClass(generateAdapter = true)
data class MatchResponse(
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val dateTime: Long, // timestamp
    val status: String,   // "SCHEDULED", "LIVE", "FINISHED"
    val stadiumId: Int,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class MatchSyncResponse(
    val lastSyncTime: Long,
    val updatedMatches: List<MatchResponse>
)

@JsonClass(generateAdapter = true)
data class PredictionResponse(
    val id: String,
    val matchId: Int,
    val userId: String,
    val predictedHomeScore: Int,
    val predictedAwayScore: Int,
    val pointsEarned: Int
)

@JsonClass(generateAdapter = true)
data class StadiumResponse(
    val id: Int,
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val capacity: Int,
    val imageUrl: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class StadiumMatchesResponse(
    val stadiumId: Int,
    val matches: List<MatchResponse>
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val user: UserProfile
)
