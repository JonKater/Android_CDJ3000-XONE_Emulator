package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val bpm: Double,
    val initialKey: String,
    val durationMs: Long,
    val filePath: String = "",
    val waveformData: String = "", // Comma-separated normalized peak amplitudes (0.0 to 1.0)
    val beatGridOffsetMs: Long = 0,
    val genre: String = "Electronic",
    val dateAdded: Long = System.currentTimeMillis()
)
