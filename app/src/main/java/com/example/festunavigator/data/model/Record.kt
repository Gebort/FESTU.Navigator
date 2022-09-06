package com.example.festunavigator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val start: String,
    val end: String,
    val time: Long
)