package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {
    @Query("SELECT * FROM predictions WHERE userId = :userId")
    fun getPredictionsForUser(userId: String): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE id = :id")
    suspend fun getPredictionById(id: String): PredictionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPredictions(predictions: List<PredictionEntity>)

    @Query("DELETE FROM predictions")
    suspend fun clearAllPredictions()
}
