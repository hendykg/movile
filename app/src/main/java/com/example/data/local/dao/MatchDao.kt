package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY dateTime ASC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Int): MatchEntity?

    @Query("SELECT * FROM matches WHERE stadiumId = :stadiumId ORDER BY dateTime ASC")
    suspend fun getMatchesByStadium(stadiumId: Int): List<MatchEntity>

    @Query("SELECT MAX(updatedAt) FROM matches")
    suspend fun getLastUpdatedTime(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Query("DELETE FROM matches")
    suspend fun clearAllMatches()
}
