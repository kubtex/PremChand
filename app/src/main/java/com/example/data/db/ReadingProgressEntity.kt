package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val storyId: String,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val percentCompleted: Int = 0,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
)
