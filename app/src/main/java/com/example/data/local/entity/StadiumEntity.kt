package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stadiums")
data class StadiumEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val capacity: Int,
    val imageUrl: String,
    val description: String
)
