package com.gerbort.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val start: String,
    val end: String,
    val time: Long
)