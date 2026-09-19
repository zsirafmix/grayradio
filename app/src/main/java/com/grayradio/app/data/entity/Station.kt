package com.grayradio.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class Station(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val countryCode: String? = null,
    val tags: String? = null,
)
