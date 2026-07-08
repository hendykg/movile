package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StadiumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StadiumDao {
    @Query("SELECT * FROM stadiums ORDER BY name ASC")
    fun getAllStadiums(): Flow<List<StadiumEntity>>

    @Query("SELECT * FROM stadiums WHERE id = :id")
    suspend fun getStadiumById(id: Int): StadiumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStadiums(stadiums: List<StadiumEntity>)

    @Query("DELETE FROM stadiums")
    suspend fun clearAllStadiums()
}
