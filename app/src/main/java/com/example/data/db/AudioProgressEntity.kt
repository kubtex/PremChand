package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_progress")
data class AudioProgressEntity(
    @PrimaryKey val storyId: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedTimestamp: Long,
    val isCompleted: Boolean,
    val playbackSpeed: Float = 1.0f
)
