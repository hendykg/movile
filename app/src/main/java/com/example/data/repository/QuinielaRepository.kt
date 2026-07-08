package com.example.data.repository

import android.util.Log
import com.example.data.local.SessionManager
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class QuinielaRepository(
    private val apiService: ApiService,
    private val matchDao: MatchDao,
    private val predictionDao: PredictionDao,
    private val stadiumDao: StadiumDao,
    private val syncLogDao: SyncLogDao,
    private val sessionManager: SessionManager
) {

    companion object {
        private const val TAG = "QuinielaRepository"
    }

    // Auth & Profile
    suspend fun register(username: String, email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(mapOf("username" to username, "email" to email, "password" to password))
            sessionManager.saveSession(
                token = response.token,
                userId = response.user.id,
                username = response.user.username,
                email = response.user.email
            )
            logSync("/register", "SUCCESS")
            response
        } catch (e: Exception) {
            logSync("/register", "FAILED", e.localizedMessage)
            throw e
        }
    }

    suspend fun login(email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(mapOf("email" to email, "username" to email, "password" to password))
            sessionManager.saveSession(
                token = response.token,
                userId = response.user.id,
                username = response.user.username,
                email = response.user.email
            )
            logSync("/login", "SUCCESS")
            response
        } catch (e: Exception) {
            logSync("/login", "FAILED", e.localizedMessage)
            throw e
        }
    }

    suspend fun getProfile(): UserProfile = withContext(Dispatchers.IO) {
        val token = sessionManager.getToken() ?: throw Exception("No autenticado")
        try {
            val profile = apiService.getProfile(token)
            logSync("/profile", "SUCCESS")
            profile
        } catch (e: Exception) {
            logSync("/profile", "FAILED", e.localizedMessage)
            throw e
        }
    }

    fun getSessionUser(): UserProfile? {
        val uid = sessionManager.getUserId() ?: return null
        return UserProfile(
            id = uid,
            username = sessionManager.getUsername() ?: "Usuario",
            email = sessionManager.getEmail() ?: "",
            points = 0,
            predictionCount = 0,
            joinedDate = "2026"
        )
    }

    fun logout() {
        sessionManager.clearSession()
    } //CIERRE

    // Groups
    suspend fun getGroups(): List<Group> = withContext(Dispatchers.IO) {
        val token = sessionManager.getToken() ?: throw Exception("No autenticado")
        try {
            val list = apiService.getGroups(token)
            logSync("/groups", "SUCCESS")
            list
        } catch (e: Exception) {
            logSync("/groups", "FAILED", e.localizedMessage)
            throw e
        }
    }

    suspend fun createGroup(name: String, description: String, stadiumName: String): Group = withContext(Dispatchers.IO) {
        val token = sessionManager.getToken() ?: throw Exception("No autenticado")
        try {
            val group = apiService.createGroup(
                token, 
                mapOf("name" to name, "description" to description, "stadiumName" to stadiumName)
            )
            logSync("/groups [POST]", "SUCCESS")
            group
        } catch (e: Exception) {
            logSync("/groups [POST]", "FAILED", e.localizedMessage)
            throw e
        }
    }

    suspend fun joinGroup(code: String): Group = withContext(Dispatchers.IO) {
        val token = sessionManager.getToken() ?: throw Exception("No autenticado")
        try {
            val group = apiService.joinGroup(token, mapOf("code" to code))
            logSync("/groups/join", "SUCCESS")
            group
        } catch (e: Exception) {
            logSync("/groups/join", "FAILED", e.localizedMessage)
            throw e
        }
    }

    suspend fun getGroupDetails(id: Int): Group = withContext(Dispatchers.IO) {
        val token = sessionManager.getToken() ?: throw Exception("No autenticado")
        try {
            val details = apiService.getGroupDetails(token, id)
            logSync("/groups/$id", "SUCCESS")
            details
        } catch (e: Exception) {
            logSync("/groups/$id", "FAILED", e.localizedMessage)
            throw e
        }
    }

    suspend fun getGroupLeaderboard(id: Int): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        val token = sessionManager.getToken() ?: throw Exception("No autenticado")
        try {
            val leaderboard = apiService.getGroupLeaderboard(token, id)
            logSync("/groups/$id/leaderboard", "SUCCESS")
            leaderboard
        } catch (e: Exception) {
            logSync("/groups/$id/leaderboard", "FAILED", e.localizedMessage)
            throw e
        }
    }

    // Matches & Sincronización Delta/Partial (Requirements 9, 10, 11, 24, 25, 26, 34)
    fun getLocalMatches(): Flow<List<MatchEntity>> {
        return matchDao.getAllMatches()
    }

    suspend fun syncMatches() = withContext(Dispatchers.IO) {
        //SINCRONIZA DB /SYNC
        val lastSyncLocal = sessionManager.getLastSyncTime()
        val localCount = matchDao.getAllMatches().firstOrNull()?.size ?: 0

        try {
            if (localCount == 0 || lastSyncLocal == 0L) {
                // Perform first-time full sync
                val matchesResponseList = apiService.getMatches()
                val matchEntities = matchesResponseList.map { it.toMatchEntity() }
                matchDao.insertMatches(matchEntities)
                sessionManager.saveLastSyncTime(System.currentTimeMillis())
                logSync("/matches", "SUCCESS")
            } else {
                // Perform incremental partial sync based on last modified time (Requirement 11)
                val syncResult = apiService.getMatchesUpdates(lastSyncLocal)
                if (syncResult.updatedMatches.isNotEmpty()) {
                    val entityUpdates = syncResult.updatedMatches.map { it.toMatchEntity() }
                    matchDao.insertMatches(entityUpdates) // inserts or overwrites updated data
                }
                sessionManager.saveLastSyncTime(syncResult.lastSyncTime)
                logSync("/matches/updates", "SUCCESS")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing matches, displaying current offline copies", e)
            logSync(if (localCount == 0) "/matches" else "/matches/updates", "FAILED", e.localizedMessage)
            // Squelch compiler error, displaying existing local info as fallback (Requirements 25 & 26)
        }
    }

    suspend fun getMatchDetails(id: Int): MatchEntity? = withContext(Dispatchers.IO) {
        // Query local DB first (Offline-first, Requirement 25)
        val localModel = matchDao.getMatchById(id)
        try {
            val apiModel = apiService.getMatchDetails(id)
            val updated = apiModel.toMatchEntity()
            matchDao.insertMatches(listOf(updated))
            logSync("/matches/$id", "SUCCESS")
            updated
        } catch (e: Exception) {
            logSync("/matches/$id", "FAILED", e.localizedMessage)
            localModel // Fallback to Room offline copy (Requirement 26)
        }
    }

    // Predictions (Requirements 13, 14, 15, 16)
    fun getLocalPredictions(): Flow<List<PredictionEntity>> {
        val userId = sessionManager.getUserId() ?: ""
        return predictionDao.getPredictionsForUser(userId)
    }

    suspend fun syncPredictions() = withContext(Dispatchers.IO) {
        val token = sessionManager.getToken() ?: return@withContext
        val userId = sessionManager.getUserId() ?: return@withContext
        try {
            val remotePreds = apiService.getMyPredictions(token)
            val entities = remotePreds.map { pred ->
                PredictionEntity(
                    id = pred.id,
                    userId = pred.userId,
                    matchId = pred.matchId,
                    predictedHomeScore = pred.predictedHomeScore,
                    predictedAwayScore = pred.predictedAwayScore,
                    pointsEarned = pred.pointsEarned,
                    isSynced = true
                )
            }
            predictionDao.insertPredictions(entities)
            logSync("/predictions/me", "SUCCESS")
        } catch (e: Exception) {
            logSync("/predictions/me", "FAILED", e.localizedMessage)
            Log.e(TAG, "Error syncing predictions", e)
        }
    }

    suspend fun submitPrediction(matchId: Int, homeScore: Int, awayScore: Int) = withContext(Dispatchers.IO) {
        val token = sessionManager.getToken() ?: throw Exception("No autenticado")
        val userId = sessionManager.getUserId() ?: throw Exception("No autenticado")
        val predId = "${userId}_${matchId}"

        // Check if game has started locally first (Requirement 16)
        val matchLocal = matchDao.getMatchById(matchId)
        if (matchLocal != null && System.currentTimeMillis() >= matchLocal.dateTime) {
            throw Exception("No se puede pronosticar. El partido ya ha comenzado.")
        }

        // Offline-first save: write locally immediately so it's searchable offline (Requirement 15)
        val localPrediction = PredictionEntity(
            id = predId,
            userId = userId,
            matchId = matchId,
            predictedHomeScore = homeScore,
            predictedAwayScore = awayScore,
            pointsEarned = 0,
            isSynced = false
        )
        predictionDao.insertPredictions(listOf(localPrediction))

        try {
            // Send to live server API
            val response = apiService.placePrediction(
                token,
                mapOf(
                    "matchId" to matchId,
                    "predictedHomeScore" to homeScore,
                    "predictedAwayScore" to awayScore
                )
            )

            // Update to synced state
            val syncedPrediction = PredictionEntity(
                id = response.id,
                userId = response.userId,
                matchId = response.matchId,
                predictedHomeScore = response.predictedHomeScore,
                predictedAwayScore = response.predictedAwayScore,
                pointsEarned = response.pointsEarned,
                isSynced = true
            )
            predictionDao.insertPredictions(listOf(syncedPrediction))
            logSync("/predictions [POST]", "SUCCESS")
        } catch (e: Exception) {
            logSync("/predictions [POST]", "FAILED", e.localizedMessage)
            Log.e(TAG, "Prediction saved offline. Will sync later.", e)
            // Do not throw if it was successfully written offline - enables fully offline work!
        }
    }

    // Stadiums (Requirements 18, 19, 21, 22)
    fun getLocalStadiums(): Flow<List<StadiumEntity>> {
        return stadiumDao.getAllStadiums()
    }

    suspend fun syncStadiums() = withContext(Dispatchers.IO) {
        val existing = stadiumDao.getAllStadiums().firstOrNull() ?: emptyList()
        if (existing.isNotEmpty()) {
            return@withContext // Avoid repeating stadium API queries for rarely-changing data (Requirement 19)
        }

        try {
            val list = apiService.getStadiums()
            val entities = list.map { it.toStadiumEntity() }
            stadiumDao.insertStadiums(entities)
            logSync("/stadiums", "SUCCESS")
        } catch (e: Exception) {
            logSync("/stadiums", "FAILED", e.localizedMessage)
            Log.e(TAG, "Error fetching stadiums", e)
        }
    }

    suspend fun getStadiumDetails(id: Int): StadiumEntity? = withContext(Dispatchers.IO) {
        val local = stadiumDao.getStadiumById(id)
        try {
            val apiStadium = apiService.getStadiumDetails(id)
            val updated = apiStadium.toStadiumEntity()
            stadiumDao.insertStadiums(listOf(updated))
            logSync("/stadiums/$id", "SUCCESS")
            updated
        } catch (e: Exception) {
            logSync("/stadiums/$id", "FAILED", e.localizedMessage)
            local
        }
    }

    suspend fun getMatchesInStadium(stadiumId: Int): List<MatchEntity> = withContext(Dispatchers.IO) {
        // Consult local Room database first (Requirement 22/25)
        val localMatches = matchDao.getMatchesByStadium(stadiumId)
        try {
            val list = apiService.getStadiumMatches(stadiumId)
            val entities = list.map { it.toMatchEntity() }
            matchDao.insertMatches(entities)
            logSync("/stadiums/$stadiumId/matches", "SUCCESS")
            entities
        } catch (e: Exception) {
            logSync("/stadiums/$stadiumId/matches", "FAILED", e.localizedMessage)
            localMatches
        }
    }

    // Sync Run History logs (Requirement 35)
    fun getSyncLogs(): Flow<List<SyncLogEntity>> {
        return syncLogDao.getAllLogs()
    }

    suspend fun clearSyncLogs() = withContext(Dispatchers.IO) {
        syncLogDao.clearLogs()
    }

    private suspend fun logSync(endpoint: String, result: String, error: String? = null) {
        try {
            syncLogDao.insertLog(
                SyncLogEntity(
                    result = result,
                    endpoint = endpoint,
                    errorMessage = error
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert sync log", e)
        }
    }

    // Model translators
    private fun MatchResponse.toMatchEntity(): MatchEntity {
        return MatchEntity(
            id = this.id,
            homeTeam = this.homeTeam,
            awayTeam = this.awayTeam,
            homeScore = this.homeScore,
            awayScore = this.awayScore,
            dateTime = this.dateTime,
            status = this.status,
            stadiumId = this.stadiumId,
            updatedAt = this.updatedAt
        )
    }

    private fun StadiumResponse.toStadiumEntity(): StadiumEntity {
        return StadiumEntity(
            id = this.id,
            name = this.name,
            city = this.city,
            latitude = this.latitude,
            longitude = this.longitude,
            capacity = this.capacity,
            imageUrl = this.imageUrl,
            description = this.description
        )
    }
}
